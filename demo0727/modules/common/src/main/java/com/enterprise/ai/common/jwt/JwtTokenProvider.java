package com.enterprise.ai.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
public class JwtTokenProvider {

    @Value("${security.jwt.secret:enterprise-ai-platform-secret-key-2024}")
    private String jwtSecret;

    @Value("${security.jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Value("${security.jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

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
