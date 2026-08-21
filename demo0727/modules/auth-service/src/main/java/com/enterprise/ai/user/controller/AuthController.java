package com.enterprise.ai.user.controller;

import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.user.dto.PasswordUpdateDTO;
import com.enterprise.ai.user.dto.UserLoginDTO;
import com.enterprise.ai.user.dto.UserRegisterDTO;
import com.enterprise.ai.user.dto.UserUpdateDTO;
import com.enterprise.ai.user.service.UserService;
import com.enterprise.ai.user.vo.LoginVO;
import com.enterprise.ai.user.vo.RefreshTokenVO;
import com.enterprise.ai.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户注册、登录、Token 刷新、退出等接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        return Result.success(userService.register(registerDTO));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<RefreshTokenVO> refreshToken(@RequestHeader("Authorization") String token) {
        String refreshToken = token.replace("Bearer ", "");
        return Result.success(userService.refreshToken(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
}
