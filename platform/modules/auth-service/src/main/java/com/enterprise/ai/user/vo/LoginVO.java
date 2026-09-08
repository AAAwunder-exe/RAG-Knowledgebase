package com.enterprise.ai.user.vo;

import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO {

    /** Access Token */
    private String accessToken;

    /** Refresh Token（前端用于自动续期，与 accessToken 分开存储） */
    private String refreshToken;

    /** Token 类型 */
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserVO user;
}
