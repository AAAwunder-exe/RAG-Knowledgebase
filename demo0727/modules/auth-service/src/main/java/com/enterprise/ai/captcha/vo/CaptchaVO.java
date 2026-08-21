package com.enterprise.ai.captcha.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码唯一标识（提交登录时回传） */
    private String uuid;

    /** 验证码图片（PNG 裸 base64，前端需拼 data:image/png;base64, 前缀） */
    private String imgBase64;
}
