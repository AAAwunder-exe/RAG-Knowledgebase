package com.enterprise.ai.gateway.filter;

import com.enterprise.ai.common.constant.HeaderConstants;
import com.enterprise.ai.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 网关全局 JWT 认证过滤器
 *
 * 职责：
 * - 放行清单内（/api/auth/** 等）：剥离客户端伪造的 X-User-* header 后直接放行
 * - 其余请求：校验 Authorization Bearer token（签名 / 过期 / type==access），
 *   成功则剥离伪造 header 并注入可信的 X-User-Id / X-User-Name / X-User-Roles
 *
 * 注意：只操作 header、绝不读取 body，保证 multipart 大文件可流式透传。
 * 下游服务（HeaderAuthenticationFilter）信任这些 header，因此本过滤器必须剥离
 * 客户端直接携带的同名 header，防止越权伪造。
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_TYPE_ACCESS = "access";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 与 auth-service 的 security.jwt.secret 保持一致（dev profile 值） */
    @Value("${security.jwt.secret:dev-secret-key-2024-enterprise-ai-platform-dev-secret-key-very-long}")
    private String jwtSecret;

    /** 放行路径前缀（无需 token，但仍剥离伪造 X-User-*） */
    private static final List<String> WHITE_LIST = List.of(
        "/api/auth/",
        "/api/system/config/public",
        "/api-docs",
        "/swagger-ui",
        "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 放行路径：去伪造 header 后直接放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange.mutate()
                .request(stripForwardedHeaders(request))
                .build());
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "缺少认证 Token");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.warn("JWT 校验失败, path={}, err={}", path, e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期");
        }

        // 只接受 access token，refresh token 不能访问业务接口
        if (!CLAIM_TYPE_ACCESS.equals(claims.get("type", String.class))) {
            return unauthorized(exchange, "Token 类型错误");
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        List<String> roles = getRoles(claims);
        List<String> permissions = getPermissions(claims);

        ServerHttpRequest mutated = request.mutate()
            .headers(h -> {
                // 先剥离客户端可能伪造的用户 header，再注入可信信息
                h.remove(HeaderConstants.USER_ID);
                h.remove(HeaderConstants.USER_NAME);
                h.remove(HeaderConstants.USER_ROLES);
                h.remove(HeaderConstants.USER_PERMISSIONS);
                h.set(HeaderConstants.USER_ID, String.valueOf(userId));
                h.set(HeaderConstants.USER_NAME, username == null ? "" : username);
                h.set(HeaderConstants.USER_ROLES, String.join(",", roles));
                h.set(HeaderConstants.USER_PERMISSIONS, String.join(",", permissions));
            })
            .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private List<String> getRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return ((List<?>) roles).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<String> getPermissions(Claims claims) {
        Object permissions = claims.get("permissions");
        if (permissions instanceof List) {
            return ((List<?>) permissions).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    /** 剥离客户端携带的 X-User-* header（放行路径也要剥离，防伪造） */
    private ServerHttpRequest stripForwardedHeaders(ServerHttpRequest request) {
        return request.mutate()
            .headers(h -> {
                h.remove(HeaderConstants.USER_ID);
                h.remove(HeaderConstants.USER_NAME);
                h.remove(HeaderConstants.USER_ROLES);
                h.remove(HeaderConstants.USER_PERMISSIONS);
            })
            .build();
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Result.failure(401, message));
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":401,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 最先执行（在路由转发过滤器 NettyRoutingFilter 之前）
        return -100;
    }
}
