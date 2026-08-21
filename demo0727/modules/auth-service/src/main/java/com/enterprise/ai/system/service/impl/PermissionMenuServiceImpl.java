package com.enterprise.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import com.enterprise.ai.system.entity.Permission;
import com.enterprise.ai.system.mapper.PermissionMapper;
import com.enterprise.ai.system.service.PermissionMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 权限管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class PermissionMenuServiceImpl implements PermissionMenuService {

    private final PermissionMapper permissionMapper;

    @Override
    public Page<Permission> pagePermissions(Page<Permission> page, String permissionName) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(permissionName)) {
            wrapper.like(Permission::getPermissionName, permissionName);
        }
        wrapper.orderByAsc(Permission::getSort);
        return permissionMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Permission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
            .eq(Permission::getStatus, 1)
            .orderByAsc(Permission::getSort));
    }

    @Override
    public Permission getPermissionById(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return permission;
    }

    @Override
    @Transactional
    public Permission createPermission(Permission permission) {
        permission.setStatus(1);
        if (permission.getSort() == null) {
            permission.setSort(0);
        }
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public Permission updatePermission(Long id, Permission permission) {
        Permission existPermission = permissionMapper.selectById(id);
        if (existPermission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        permission.setId(id);
        permissionMapper.updateById(permission);
        return permissionMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        permissionMapper.deleteById(id);
    }
}
