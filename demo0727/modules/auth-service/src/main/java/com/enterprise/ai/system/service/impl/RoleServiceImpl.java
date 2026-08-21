package com.enterprise.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import com.enterprise.ai.system.entity.Role;
import com.enterprise.ai.system.entity.RolePermission;
import com.enterprise.ai.system.mapper.RoleMapper;
import com.enterprise.ai.system.mapper.RolePermissionMapper;
import com.enterprise.ai.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public Page<Role> pageRoles(Page<Role> page, String roleName) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(roleName)) {
            wrapper.like(Role::getRoleName, roleName);
        }
        wrapper.orderByDesc(Role::getCreateTime);
        return roleMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
            .eq(Role::getStatus, 1)
            .orderByAsc(Role::getSort));
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        role.setStatus(1);
        roleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional
    public Role updateRole(Long id, Role role) {
        Role existRole = roleMapper.selectById(id);
        if (existRole == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        role.setId(id);
        roleMapper.updateById(role);
        return roleMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        // 禁止删除超级管理员角色，防止系统失去最高权限入口
        if ("ROLE_ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "不能删除超级管理员角色");
        }
        roleMapper.deleteById(id);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        return rolePermissions.stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toList());
    }
}
