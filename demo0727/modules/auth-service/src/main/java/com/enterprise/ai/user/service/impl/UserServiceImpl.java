package com.enterprise.ai.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.captcha.service.CaptchaService;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import com.enterprise.ai.security.context.SecurityContextUtils;
import com.enterprise.ai.security.jwt.JwtTokenProvider;
import com.enterprise.ai.system.entity.Role;
import com.enterprise.ai.system.entity.UserRole;
import com.enterprise.ai.system.mapper.RoleMapper;
import com.enterprise.ai.system.mapper.UserRoleMapper;
import com.enterprise.ai.system.service.PermissionService;
import com.enterprise.ai.user.dto.PasswordUpdateDTO;
import com.enterprise.ai.user.dto.UserAdminUpdateDTO;
import com.enterprise.ai.user.dto.UserCreateDTO;
import com.enterprise.ai.user.dto.UserLoginDTO;
import com.enterprise.ai.user.dto.UserRegisterDTO;
import com.enterprise.ai.user.dto.UserUpdateDTO;
import com.enterprise.ai.user.entity.User;
import com.enterprise.ai.user.mapper.UserMapper;
import com.enterprise.ai.user.service.UserService;
import com.enterprise.ai.user.vo.LoginVO;
import com.enterprise.ai.user.vo.RefreshTokenVO;
import com.enterprise.ai.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final SecurityContextUtils securityContextUtils;
    private final CaptchaService captchaService;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionService permissionService;

    @Value("${security.jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Value("${security.jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final int MAX_LOGIN_FAIL_COUNT = 10;
    private static final int LOCK_MINUTES = 30;

    @Override
    @Transactional
    public UserVO register(UserRegisterDTO registerDTO) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1);
        user.setLoginFailCount(0);
        userMapper.insert(user);

        log.info("用户注册成功: {}", user.getUsername());
        return convertToVO(user);
    }

    @Override
    @Transactional
    public LoginVO login(UserLoginDTO loginDTO) {
        // 先校验验证码（在查用户之前，防止用户名枚举/时序攻击）
        captchaService.validate(loginDTO.getCaptchaUuid(), loginDTO.getCaptchaCode());

        // 查找用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }

        // 检查账户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 检查账户锁定状态
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            handleLoginFail(user);
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 登录成功，重置失败计数
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        // 生成 Token（内嵌角色，供网关透传与方法级鉴权。角色变更需等 token 过期/刷新后才生效，学习项目可接受）
        List<String> roleCodes = permissionService.getUserRoles(user.getId()).stream()
            .map(Role::getRoleCode)
            .collect(Collectors.toList());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roleCodes);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername(), roleCodes);

        // 将 Refresh Token 存入 Redis
        redisTemplate.opsForValue().set(
            "refresh:token:" + user.getId(), 
            refreshToken, 
            refreshTokenExpiration, 
            TimeUnit.MILLISECONDS
        );

        log.info("用户登录成功: {}", user.getUsername());

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setExpiresIn(jwtExpiration / 1000);
        loginVO.setUser(convertToVO(user));
        return loginVO;
    }

    @Override
    public RefreshTokenVO refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 检查 Redis 中的 Refresh Token
        String storedToken = redisTemplate.opsForValue().get("refresh:token:" + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }

        // 生成新的 Access Token（沿用 refresh token 内嵌的 roles；
        // 兼容改造前的旧 refresh token——无 roles claim 时回退查库）
        List<String> roles = jwtTokenProvider.getRolesFromToken(refreshToken);
        if (roles.isEmpty()) {
            roles = permissionService.getUserRoles(userId).stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
        }
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username, roles);

        RefreshTokenVO vo = new RefreshTokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setExpiresIn(jwtExpiration / 1000);
        return vo;
    }

    @Override
    public void logout() {
        Long userId = securityContextUtils.getCurrentUserId();
        if (userId != null) {
            redisTemplate.delete("refresh:token:" + userId);
        }
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = securityContextUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    @Transactional
    public UserVO updateUser(UserUpdateDTO updateDTO) {
        Long userId = securityContextUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }

        BeanUtils.copyProperties(updateDTO, user);
        userMapper.updateById(user);

        return convertToVO(user);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateDTO passwordDTO) {
        Long userId = securityContextUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }

        // 验证原密码
        if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userMapper.updateById(user);
        log.info("用户密码修改成功: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        // 禁止禁用超级管理员账户，防止系统失去最高权限入口
        if (status != null && status == 0 && hasRoleCode(id, "ROLE_ADMIN")) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "不能禁用超级管理员账户");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public Page<UserVO> pageUsers(Page<?> page, String username, String realName, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(User::getRealName, realName);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);

        // 批量填充角色编码，避免 N+1 查询
        Map<Long, List<String>> userIdRolesMap = loadRoleCodesMap(
            userPage.getRecords().stream().map(User::getId).collect(Collectors.toList()));

        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
            .map(u -> convertToVO(u, userIdRolesMap))
            .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public UserVO createUser(UserCreateDTO createDTO) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, createDTO.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        BeanUtils.copyProperties(createDTO, user);
        user.setPassword(passwordEncoder.encode(createDTO.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setLoginFailCount(0);
        userMapper.insert(user);

        log.info("管理员创建用户: {}", user.getUsername());
        return convertToVO(user);
    }

    @Override
    @Transactional
    public UserVO adminUpdateUser(Long id, UserAdminUpdateDTO updateDTO) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        if (updateDTO.getRealName() != null) user.setRealName(updateDTO.getRealName());
        if (updateDTO.getEmail() != null) user.setEmail(updateDTO.getEmail());
        if (updateDTO.getPhone() != null) user.setPhone(updateDTO.getPhone());
        if (updateDTO.getAvatar() != null) user.setAvatar(updateDTO.getAvatar());
        if (updateDTO.getStatus() != null) user.setStatus(updateDTO.getStatus());
        userMapper.updateById(user);

        log.info("管理员更新用户: id={}", id);
        return convertToVO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        // 禁止删除当前登录账户
        Long currentUserId = securityContextUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "不能删除当前登录账户");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        // 禁止删除拥有超级管理员角色的用户，防止系统失去最高权限入口
        if (hasRoleCode(id, "ROLE_ADMIN")) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "不能删除超级管理员账户");
        }
        // @TableLogic 逻辑删除
        userMapper.deleteById(id);
        // 清理角色绑定
        userRoleMapper.deleteByUserId(id);
        // 删除角色缓存，避免删除用户后角色仍被缓存
        redisTemplate.delete(PermissionService.ROLE_CACHE_KEY_PREFIX + id);
        log.info("删除用户: id={}, username={}", id, user.getUsername());
    }

    /**
     * 判断用户是否拥有指定角色编码（用于保护超级管理员账户）
     */
    private boolean hasRoleCode(Long userId, String roleCode) {
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return false;
        }
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds).stream()
            .anyMatch(role -> roleCode.equals(role.getRoleCode()));
    }

    /**
     * 批量加载多个用户的角色编码映射
     */
    private Map<Long, List<String>> loadRoleCodesMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查用户角色关联
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查角色，建立 roleId -> roleCode 映射
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, String> roleCodeMap = roleMapper.selectBatchIds(roleIds).stream()
            .collect(Collectors.toMap(Role::getId, Role::getRoleCode, (a, b) -> a));

        // userId -> roleCodes
        return userRoles.stream()
            .collect(Collectors.groupingBy(
                UserRole::getUserId,
                Collectors.mapping(ur -> roleCodeMap.getOrDefault(ur.getRoleId(), ""), Collectors.toList())
            ));
    }

    private void handleLoginFail(User user) {
        int failCount = user.getLoginFailCount() != null ? user.getLoginFailCount() : 0;
        failCount++;
        user.setLoginFailCount(failCount);

        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            log.warn("用户账户被锁定: {}, 锁定时间: {}分钟", user.getUsername(), LOCK_MINUTES);
        }

        userMapper.updateById(user);
    }

    private UserVO convertToVO(User user) {
        return convertToVO(user, loadRoleCodesMap(Collections.singletonList(user.getId())));
    }

    private UserVO convertToVO(User user, Map<Long, List<String>> userIdRolesMap) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        List<String> roles = userIdRolesMap.get(user.getId());
        vo.setRoles(roles != null ? roles : Collections.emptyList());
        return vo;
    }
}
