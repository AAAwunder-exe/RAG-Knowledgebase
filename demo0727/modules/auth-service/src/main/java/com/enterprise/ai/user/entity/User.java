package com.enterprise.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /** 用户名 */
    @TableField("username")
    private String username;

    /** 密码（加密存储） */
    @TableField("password")
    private String password;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName;

    /** 邮箱 */
    @TableField("email")
    private String email;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 头像 URL */
    @TableField("avatar")
    private String avatar;

    /** 状态：0-禁用，1-启用 */
    @TableField("status")
    private Integer status;

    /** 登录失败次数 */
    @TableField("login_fail_count")
    private Integer loginFailCount;

    /** 账户锁定时间 */
    @TableField("locked_until")
    private LocalDateTime lockedUntil;
}
