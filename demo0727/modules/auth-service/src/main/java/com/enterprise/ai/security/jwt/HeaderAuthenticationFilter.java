package com.enterprise.ai.security.jwt;

import com.enterprise.ai.common.constant.HeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Header 信任认证过滤器（取代 JwtAuthenticationFilter）
 *
 * 信任前提：本服务不对外暴露端口，所有请求必经网关。
 * 网关已校验 JWT 并注入可信的 X-User-Id / X-User-Name / X-User-Roles header，
 * 本过滤器直接读取并写入 SecurityContext，不再实时查库加载角色（角色已内嵌 token）。
 *
 * 直连本服务（无 X-User-* header）→ 清空上下文，被 SecurityConfig 拦截为 401。
 * 兼容 SecurityContextUtils：principal=Long userId、details=username。
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(HeaderConstants.USER_ID);

        if (StringUtils.hasText(userId)) {
            try {
                Long uid = Long.valueOf(userId);
                String username = request.getHeader(HeaderConstants.USER_NAME);
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                authorities.addAll(parseRoles(request.getHeader(HeaderConstants.USER_ROLES)));
                // 权限码作为普通 authority（形如 menu:user），供 @PreAuthorize("hasAuthority('menu:xxx')") 鉴权
                authorities.addAll(parseRoles(request.getHeader(HeaderConstants.USER_PERMISSIONS)));
                // 无任何角色时回退默认 ROLE_USER（对齐原 JwtAuthenticationFilter 逻辑）
                if (authorities.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(uid, null, authorities);
                authentication.setDetails(username);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                SecurityContextHolder.clearContext();
            }
        } else {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesHeader.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
}
