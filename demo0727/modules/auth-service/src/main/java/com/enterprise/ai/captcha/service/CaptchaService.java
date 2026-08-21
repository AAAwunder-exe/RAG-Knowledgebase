package com.enterprise.ai.captcha.service;

import com.enterprise.ai.captcha.vo.CaptchaVO;

/**
 * 图形验证码服务
 */
public interface CaptchaService {

    /**
     * 生成验证码：绘制图片并返回 uuid + base64
     */
    CaptchaVO generate();

    /**
     * 校验验证码：单次使用，无论成败均删除
     *
     * @param uuid 验证码唯一标识
     * @param code 用户输入的验证码
     */
    void validate(String uuid, String code);
}
