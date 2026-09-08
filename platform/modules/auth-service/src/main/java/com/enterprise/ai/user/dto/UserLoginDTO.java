package com.enterprise.ai.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码唯一标识 */
    @NotBlank(message = "验证码不能为空")
    private String captchaUuid;

    /** 验证码内容 */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
