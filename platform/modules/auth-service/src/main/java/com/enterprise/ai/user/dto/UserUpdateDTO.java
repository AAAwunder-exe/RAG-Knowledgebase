package com.enterprise.ai.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求 DTO
 */
@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "真实姓名长度不能超过 50 个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatar;
}
