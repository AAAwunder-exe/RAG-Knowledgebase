package com.enterprise.ai.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.user.dto.PasswordUpdateDTO;
import com.enterprise.ai.user.dto.UserAdminUpdateDTO;
import com.enterprise.ai.user.dto.UserCreateDTO;
import com.enterprise.ai.user.dto.UserUpdateDTO;
import com.enterprise.ai.user.service.UserService;
import com.enterprise.ai.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户信息查询、更新、密码修改等接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }

    @Operation(summary = "根据 ID 获取用户信息")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新当前用户信息")
    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@Valid @RequestBody UserUpdateDTO updateDTO) {
        return Result.success(userService.updateUser(updateDTO));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO passwordDTO) {
        userService.updatePassword(passwordDTO);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Result<Page<UserVO>> pageUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status) {
        Page<?> page = new Page<>(current, size);
        return Result.success(userService.pageUsers(page, username, realName, status));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        return Result.success(userService.createUser(createDTO));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserAdminUpdateDTO updateDTO) {
        return Result.success(userService.adminUpdateUser(id, updateDTO));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
