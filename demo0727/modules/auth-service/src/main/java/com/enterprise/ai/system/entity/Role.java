package com.enterprise.ai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {

    /** 角色名称 */
    @TableField("role_name")
    private String roleName;

    /** 角色编码（唯一标识） */
    @TableField("role_code")
    private String roleCode;

    /** 角色描述 */
    @TableField("description")
    private String description;

    /** 排序 */
    @TableField("sort")
    private Integer sort;

    /** 状态：0-禁用，1-启用 */
    @TableField("status")
    private Integer status;
}
