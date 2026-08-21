package com.enterprise.ai.common.constant;

/**
 * 网关与下游服务之间透传的用户信息 Header 常量
 * 网关统一校验 JWT 后注入；下游服务 HeaderAuthenticationFilter 读取并写入 SecurityContext。
 * 客户端请求中携带的这些 header 一律由网关剥离（防止伪造）。
 */
public final class HeaderConstants {

    /** 用户 ID */
    public static final String USER_ID = "X-User-Id";

    /** 用户名 */
    public static final String USER_NAME = "X-User-Name";

    /** 角色编码列表（逗号分隔，形如 ROLE_ADMIN,ROLE_USER） */
    public static final String USER_ROLES = "X-User-Roles";

    private HeaderConstants() {
    }
}
