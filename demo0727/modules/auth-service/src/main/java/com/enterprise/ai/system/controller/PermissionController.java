package com.enterprise.ai.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.system.entity.Permission;
import com.enterprise.ai.system.entity.Role;
import com.enterprise.ai.system.service.PermissionMenuService;
import com.enterprise.ai.system.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@Tag(name = "权限管理", description = "权限 CRUD、用户角色分配等接口")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionMenuService permissionMenuService;
    private final PermissionService permissionService;

    @Operation(summary = "分页查询权限列表")
    @GetMapping("/page")
    public Result<Page<Permission>> pagePermissions(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String permissionName) {
        Page<Permission> page = new Page<>(current, size);
        return Result.success(permissionMenuService.pagePermissions(page, permissionName));
    }

    @Operation(summary = "获取所有权限")
    @GetMapping("/list")
    public Result<List<Permission>> listPermissions() {
        return Result.success(permissionMenuService.listPermissions());
    }

    @Operation(summary = "根据 ID 获取权限")
    @GetMapping("/{id}")
    public Result<Permission> getPermissionById(@PathVariable Long id) {
        return Result.success(permissionMenuService.getPermissionById(id));
    }

    @Operation(summary = "创建权限")
    @PostMapping
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        return Result.success(permissionMenuService.createPermission(permission));
    }

    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        return Result.success(permissionMenuService.updatePermission(id, permission));
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionMenuService.deletePermission(id);
        return Result.success();
    }

    @Operation(summary = "获取用户的角色列表")
    @GetMapping("/users/{userId}/roles")
    public Result<List<Role>> getUserRoles(@PathVariable Long userId) {
        return Result.success(permissionService.getUserRoles(userId));
    }

    @Operation(summary = "获取用户的权限列表")
    @GetMapping("/users/{userId}/permissions")
    public Result<List<Permission>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(permissionService.getUserPermissions(userId));
    }

    @Operation(summary = "为用户分配角色")
    @PostMapping("/users/{userId}/roles")
    public Result<Void> assignRolesToUser(
            @PathVariable Long userId,
            @RequestBody List<Long> roleIds) {
        permissionService.assignRolesToUser(userId, roleIds);
        return Result.success();
    }
}
