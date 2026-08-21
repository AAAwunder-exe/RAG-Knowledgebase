package com.enterprise.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import com.enterprise.ai.system.entity.Permission;
import com.enterprise.ai.system.entity.Role;
import com.enterprise.ai.system.entity.RolePermission;
import com.enterprise.ai.system.entity.UserRole;
import com.enterprise.ai.system.mapper.PermissionMapper;
import com.enterprise.ai.system.mapper.RoleMapper;
import com.enterprise.ai.system.mapper.RolePermissionMapper;
import com.enterprise.ai.system.mapper.UserRoleMapper;
import com.enterprise.ai.system.service.PermissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<Role> getUserRoles(Long userId) {
        String cacheKey = ROLE_CACHE_KEY_PREFIX + userId;

        // 优先读缓存，避免每请求查 user_role + role 两张表
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<Role>>() {});
            } catch (Exception e) {
                log.warn("读取角色缓存失败，回源数据库: {}", e.getMessage());
            }
        }

        List<Role> roles = queryRolesFromDb(userId);

        // 空列表也缓存（防穿透），TTL 到期自动过期；角色变更走失效点删除
        try {
            redisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(roles),
                ROLE_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入角色缓存失败: {}", e.getMessage());
        }
        return roles;
    }

    /**
     * 从数据库查询用户的角色列表
     */
    private List<Role> queryRolesFromDb(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());

        return roleMapper.selectBatchIds(roleIds);
    }

    /**
     * 删除用户角色缓存（角色绑定变更后调用）
     */
    private void evictRoleCache(Long userId) {
        redisTemplate.delete(ROLE_CACHE_KEY_PREFIX + userId);
    }

    @Override
    public List<Permission> getUserPermissions(Long userId) {
        List<Role> roles = getUserRoles(userId);
        if (roles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = roles.stream()
            .map(Role::getId)
            .collect(Collectors.toList());

        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));

        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> permissionIds = rolePermissions.stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .collect(Collectors.toList());

        return permissionMapper.selectBatchIds(permissionIds);
    }

    @Override
    public List<Permission> getRolePermissions(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));

        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> permissionIds = rolePermissions.stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toList());

        return permissionMapper.selectBatchIds(permissionIds);
    }

    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        // 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);

        // 批量插入新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = roleIds.stream()
                .map(roleId -> {
                    UserRole userRole = new UserRole();
                    userRole.setId(IdWorker.getId());
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());
            userRoleMapper.batchInsert(userRoles);
        }

        // 角色绑定变更后删除缓存，保证下次请求反映新角色
        evictRoleCache(userId);

        log.info("为用户 {} 分配角色: {}", userId, roleIds);
    }

    @Override
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 批量插入新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                .map(permissionId -> {
                    RolePermission rp = new RolePermission();
                    rp.setId(IdWorker.getId());
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permissionId);
                    return rp;
                })
                .collect(Collectors.toList());
            rolePermissionMapper.batchInsert(rolePermissions);
        }

        log.info("为角色 {} 分配权限: {}", roleId, permissionIds);
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        long count = permissionMapper.selectCount(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermissionCode, permissionCode));
        
        if (count == 0) {
            return false;
        }

        List<Permission> permissions = getUserPermissions(userId);
        return permissions.stream()
            .anyMatch(p -> p.getPermissionCode().equals(permissionCode));
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        List<Role> roles = getUserRoles(userId);
        return roles.stream()
            .anyMatch(r -> r.getRoleCode().equals(roleCode));
    }
}
