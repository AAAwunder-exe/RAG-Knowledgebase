package com.enterprise.ai.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.system.entity.Role;
import com.enterprise.ai.system.service.PermissionService;
import com.enterprise.ai.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理", description = "角色 CRUD、分配权限等接口")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @Operation(summary = "分页查询角色列表")
    @GetMapping("/page")
    public Result<Page<Role>> pageRoles(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roleName) {
        Page<Role> page = new Page<>(current, size);
        return Result.success(roleService.pageRoles(page, roleName));
    }

    @Operation(summary = "获取所有角色")
    @GetMapping("/list")
    public Result<List<Role>> listRoles() {
        return Result.success(roleService.listRoles());
    }

    @Operation(summary = "根据 ID 获取角色")
    @GetMapping("/{id}")
    public Result<Role> getRoleById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public Result<Role> createRole(@RequestBody Role role) {
        return Result.success(roleService.createRole(role));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return Result.success(roleService.updateRole(id, role));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "获取角色的权限 ID 列表")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        return Result.success(roleService.getRolePermissionIds(id));
    }

    @Operation(summary = "为角色分配权限")
    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long id,
            @RequestBody List<Long> permissionIds) {
        permissionService.assignPermissionsToRole(id, permissionIds);
        return Result.success();
    }
}
