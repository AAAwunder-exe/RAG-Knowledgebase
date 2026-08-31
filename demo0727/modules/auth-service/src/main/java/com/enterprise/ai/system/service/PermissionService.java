package com.enterprise.ai.system.service;

import com.enterprise.ai.system.entity.Permission;
import com.enterprise.ai.system.entity.Role;
import com.enterprise.ai.system.vo.MenuNode;

import java.util.List;

/**
 * 权限管理服务接口
 */
public interface PermissionService {

    /** 用户角色缓存 key 前缀 */
    String ROLE_CACHE_KEY_PREFIX = "user:roles:";

    /** 用户角色缓存 TTL（分钟） */
    long ROLE_CACHE_TTL_MINUTES = 30;

    /**
     * 获取用户的角色列表
     */
    List<Role> getUserRoles(Long userId);

    /**
     * 获取用户的权限列表
     */
    List<Permission> getUserPermissions(Long userId);

    /**
     * 获取用户拥有的权限码集合
     */
    List<String> getUserPermissionCodes(Long userId);

    /**
     * 构建当前用户的动态菜单树（基于角色绑定的 menu 类型权限）
     */
    List<MenuNode> getUserMenus(Long userId);

    /**
     * 获取角色的权限列表
     */
    List<Permission> getRolePermissions(Long roleId);

    /**
     * 为用户分配角色
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 为角色分配权限
     */
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 检查用户是否拥有指定权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 检查用户是否拥有指定角色
     */
    boolean hasRole(Long userId, String roleCode);

    /**
     * 获取所有启用的权限（供角色分配页面查询）
     */
    List<Permission> listAllPermissions();
}
