package com.enterprise.ai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.system.entity.Permission;

import java.util.List;

/**
 * 权限管理服务接口
 */
public interface PermissionMenuService {

    /**
     * 分页查询权限列表
     */
    Page<Permission> pagePermissions(Page<Permission> page, String permissionName);

    /**
     * 获取所有权限（树形结构）
     */
    List<Permission> listPermissions();

    /**
     * 根据 ID 获取权限
     */
    Permission getPermissionById(Long id);

    /**
     * 创建权限
     */
    Permission createPermission(Permission permission);

    /**
     * 更新权限
     */
    Permission updatePermission(Long id, Permission permission);

    /**
     * 删除权限
     */
    void deletePermission(Long id);
}
