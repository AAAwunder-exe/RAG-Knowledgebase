package com.enterprise.ai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色权限关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class RolePermission extends BaseEntity {

    /** 角色 ID */
    @TableField("role_id")
    private Long roleId;

    /** 权限 ID */
    @TableField("permission_id")
    private Long permissionId;
}
