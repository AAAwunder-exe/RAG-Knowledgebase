package com.enterprise.ai.captcha.controller;

import com.enterprise.ai.captcha.service.CaptchaService;
import com.enterprise.ai.captcha.vo.CaptchaVO;
import com.enterprise.ai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器
 */
@Tag(name = "认证管理", description = "图形验证码接口")
@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码")
    @GetMapping
    public Result<CaptchaVO> getCaptcha() {
        return Result.success(captchaService.generate());
    }
}
