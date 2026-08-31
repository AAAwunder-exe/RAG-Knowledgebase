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
import com.enterprise.ai.system.vo.MenuNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    /**
     * api 接口权限 -> 对应模块的 menu 菜单权限 code。
     * 勾选某模块的接口权限时自动带出该模块菜单，实现"有接口权限即可见对应菜单"。
     */
    private static final Map<String, String> API_TO_MENU = new HashMap<>();
    static {
        API_TO_MENU.put("api:user:view", "menu:user");
        API_TO_MENU.put("api:user:update", "menu:user");
        API_TO_MENU.put("api:user:password", "menu:user");
        API_TO_MENU.put("api:role:view", "menu:role");
        API_TO_MENU.put("api:role:manage", "menu:role");
        API_TO_MENU.put("api:permission:view", "menu:permission-list");
        API_TO_MENU.put("api:permission:manage", "menu:permission-list");
        API_TO_MENU.put("api:knowledge:view", "menu:knowledge");
        API_TO_MENU.put("api:knowledge:create", "menu:knowledge");
        API_TO_MENU.put("api:knowledge:edit", "menu:knowledge");
        API_TO_MENU.put("api:knowledge:delete", "menu:knowledge");
        API_TO_MENU.put("api:document:view", "menu:document");
        API_TO_MENU.put("api:document:upload", "menu:document");
        API_TO_MENU.put("api:document:edit", "menu:document");
        API_TO_MENU.put("api:document:delete", "menu:document");
        API_TO_MENU.put("api:search", "menu:document");
        API_TO_MENU.put("api:ai:ask", "menu:ai");
        API_TO_MENU.put("api:ai:embedding", "menu:ai");
    }

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
    public List<String> getUserPermissionCodes(Long userId) {
        return getUserPermissions(userId).stream()
            .map(Permission::getPermissionCode)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 构建当前用户可见的菜单树。
     * 取用户角色绑定的 menu 类型权限，按 parent_id 组装成树（仅保留一级父节点）
     */
    @Override
    public List<MenuNode> getUserMenus(Long userId) {
        // 取全部启用权限，避免对角色权限的子集递归（父菜单也会随之自动可见）
        List<MenuNode> allMenus = permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermissionType, "menu")
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSort))
            .stream()
            .map(this::toMenuNode)
            .collect(Collectors.toList());

        // 用户拥有的菜单权限 ID 集合
        List<Permission> owned = getUserPermissions(userId);
        if (owned.isEmpty()) {
            return new ArrayList<>();
        }
        // 若用户为超级管理员，直接透传全部菜单
        boolean isAdmin = getUserRoles(userId).stream()
            .anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleCode()));
        java.util.Set<Long> ownedIdSet;
        if (isAdmin) {
            ownedIdSet = allMenus.stream().map(MenuNode::getId).collect(Collectors.toSet());
        } else {
            ownedIdSet = owned.stream().map(Permission::getId).collect(Collectors.toSet());
        }

        // 组装树：父节点也必须可见（分组菜单），否则子菜单无处挂载
        List<MenuNode> roots = new ArrayList<>();
        java.util.Map<Long, MenuNode> nodeById = allMenus.stream()
            .collect(Collectors.toMap(MenuNode::getId, n -> n, (a, b) -> a));
        for (MenuNode node : allMenus) {
            if (!ownedIdSet.contains(node.getId())
                && !(node.getParentId() != null && ownedIdSet.contains(node.getParentId()))) {
                continue;
            }
            if (node.getParentId() != null) {
                MenuNode parent = nodeById.get(node.getParentId());
                if (parent != null && ownedIdSet.contains(parent.getId())) {
                    parent.getChildren().add(node);
                    continue;
                }
            }
            roots.add(node);
        }
        return roots;
    }

    private MenuNode toMenuNode(Permission p) {
        MenuNode node = new MenuNode();
        node.setId(p.getId());
        node.setName(p.getPermissionName());
        node.setCode(p.getPermissionCode());
        node.setParentId(p.getParentId());
        node.setPath(p.getPath());
        node.setComponent(p.getComponent());
        node.setIcon(p.getIcon());
        node.setSort(p.getSort());
        return node;
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

        if (permissionIds != null && !permissionIds.isEmpty()) {
            // 命中 api 接口权限时，自动带出对应模块的 menu 菜单权限，
            // 保证"有某模块的接口权限，就能在侧边栏看到该模块菜单"
            Set<Long> effectiveIds = new LinkedHashSet<>(permissionIds);
            List<Permission> picked = permissionMapper.selectBatchIds(permissionIds);
            Set<String> menuCodes = picked.stream()
                .map(permission -> API_TO_MENU.get(permission.getPermissionCode()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (!menuCodes.isEmpty()) {
                permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                            .in(Permission::getPermissionCode, menuCodes)
                            .eq(Permission::getPermissionType, "menu"))
                    .forEach(menu -> effectiveIds.add(menu.getId()));
            }

            // 批量插入新的权限关联（含自动带出的菜单权限）
            List<RolePermission> rolePermissions = effectiveIds.stream()
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

    @Override
    public List<Permission> listAllPermissions() {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSort));
    }
}
