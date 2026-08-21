package com.enterprise.ai.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员更新用户请求 DTO
 */
@Data
public class UserAdminUpdateDTO {

    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatar;

    /** 状态：0-禁用，1-启用 */
    private Integer status;
}
