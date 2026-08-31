package com.enterprise.ai.user.vo;

import lombok.Data;

/**
 * Token 刷新响应 VO
 */
@Data
public class RefreshTokenVO {

    /** 新的 Access Token */
    private String accessToken;

    /** Token 类型 */
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private Long expiresIn;
}
