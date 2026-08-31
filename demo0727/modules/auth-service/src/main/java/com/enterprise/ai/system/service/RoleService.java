package com.enterprise.ai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.system.entity.Role;

import java.util.List;

/**
 * 角色管理服务接口
 */
public interface RoleService {

    /**
     * 分页查询角色列表
     */
    Page<Role> pageRoles(Page<Role> page, String roleName);

    /**
     * 获取所有角色
     */
    List<Role> listRoles();

    /**
     * 根据 ID 获取角色
     */
    Role getRoleById(Long id);

    /**
     * 创建角色
     */
    Role createRole(Role role);

    /**
     * 更新角色
     */
    Role updateRole(Long id, Role role);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 获取角色的权限 ID 列表
     */
    List<Long> getRolePermissionIds(Long roleId);
}
