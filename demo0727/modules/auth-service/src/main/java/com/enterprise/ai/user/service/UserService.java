package com.enterprise.ai.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.user.dto.PasswordUpdateDTO;
import com.enterprise.ai.user.dto.UserAdminUpdateDTO;
import com.enterprise.ai.user.dto.UserCreateDTO;
import com.enterprise.ai.user.dto.UserLoginDTO;
import com.enterprise.ai.user.dto.UserRegisterDTO;
import com.enterprise.ai.user.dto.UserUpdateDTO;
import com.enterprise.ai.user.vo.LoginVO;
import com.enterprise.ai.user.vo.RefreshTokenVO;
import com.enterprise.ai.user.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    UserVO register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     */
    LoginVO login(UserLoginDTO loginDTO);

    /**
     * 刷新 Token
     */
    RefreshTokenVO refreshToken(String refreshToken);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser();

    /**
     * 根据 ID 获取用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 更新用户信息
     */
    UserVO updateUser(UserUpdateDTO updateDTO);

    /**
     * 修改密码
     */
    void updatePassword(PasswordUpdateDTO passwordDTO);

    /**
     * 启用/禁用用户
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 分页查询用户列表（含角色编码）
     */
    Page<UserVO> pageUsers(Page<?> page, String username, String realName, Integer status);

    /**
     * 管理员创建用户
     */
    UserVO createUser(UserCreateDTO createDTO);

    /**
     * 管理员更新用户
     */
    UserVO adminUpdateUser(Long id, UserAdminUpdateDTO updateDTO);

    /**
     * 删除用户（逻辑删除，并清理角色绑定）
     */
    void deleteUser(Long id);
}
