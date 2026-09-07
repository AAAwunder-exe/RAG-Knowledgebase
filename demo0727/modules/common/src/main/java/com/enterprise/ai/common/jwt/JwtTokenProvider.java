package com.enterprise.ai.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT 工具类
 */
@Component
public class JwtTokenProvider implements InitializingBean {

    /** 仓库内置的开发默认密钥（仅限 dev 回退使用），生产必须通过 JWT_SECRET 覆盖 */
    private static final String DEV_DEFAULT_SECRET = "enterprise-ai-platform-secret-key-2024-enterprise-ai-platform-secret-key";

    /** HMAC-SHA 密钥最小字节数（HS256 要求 >= 256 bit） */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Value("${security.jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    /**
     * 启动时校验密钥强度，避免弱密钥 / 内置默认密钥被用于生产环境。
     * 若密钥为内置默认值且当前激活 prod profile，则直接拒绝启动（fail-fast）。
     */
    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("security.jwt.secret 未配置，请通过环境变量 JWT_SECRET 提供强随机密钥");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("security.jwt.secret 长度不足 " + MIN_SECRET_BYTES +
                    " 字节（HMAC-SHA256 要求 >= 32 字节），请配置更强的随机密钥");
        }
        if (isProdProfile() && DEV_DEFAULT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("生产环境禁止使用内置默认 JWT secret，请通过环境变量 JWT_SECRET 配置随机密钥");
        }
    }

    private boolean isProdProfile() {
        if (!StringUtils.hasText(activeProfiles)) {
            return false;
        }
        return Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     * @param roles 角色编码列表，形如 ["ROLE_ADMIN"]（与 SimpleGrantedAuthority 一致，
     *              @PreAuthorize("hasRole('ADMIN')") 才能匹配）
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return generateAccessToken(userId, username, roles, Collections.emptyList());
    }

    /**
     * 生成 Access Token（含权限码）
     * @param roles 角色编码列表，形如 ["ROLE_ADMIN"]
     * @param permissions 权限码列表，形如 ["menu:knowledge","menu:role"]，
     *                    供下游服务按权限码鉴权 @PreAuthorize("hasAuthority('menu:xxx')")
     */
    public String generateAccessToken(Long userId, String username, List<String> roles, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成 Refresh Token（同样内嵌 roles，刷新重签 access token 时沿用）
     */
    public String generateRefreshToken(Long userId, String username, List<String> roles) {
        return generateRefreshToken(userId, username, roles, Collections.emptyList());
    }

    /**
     * 生成 Refresh Token（含权限码）
     * @param roles 角色编码列表，形如 ["ROLE_ADMIN"]
     * @param permissions 权限码列表，形如 ["menu:knowledge","menu:role"]
     */
    public String generateRefreshToken(Long userId, String username, List<String> roles, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取 Claims
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 检查 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 检查 Token 类型是否为 Access Token
     */
    public boolean isAccessToken(String token) {
        try {
            String type = getClaimsFromToken(token).get("type", String.class);
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 Token 类型是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        try {
            String type = getClaimsFromToken(token).get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取角色编码列表（无 roles claim 时返回空列表）
     */
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Object roles = claims.get("roles");
            if (roles instanceof List) {
                return ((List<?>) roles).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 从 Token 中获取权限码列表（无 permissions claim 时返回空列表）
     */
    public List<String> getPermissionsFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Object permissions = claims.get("permissions");
            if (permissions instanceof List) {
                return ((List<?>) permissions).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
