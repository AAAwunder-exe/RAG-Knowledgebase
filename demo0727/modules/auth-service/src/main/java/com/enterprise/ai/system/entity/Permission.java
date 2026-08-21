package com.enterprise.ai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.ai.common.entity.BaseEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

    /** 权限名称 */
    @TableField("permission_name")
    private String permissionName;

    /** 权限编码（唯一标识） */
    @TableField("permission_code")
    private String permissionCode;

    /** 权限类型：menu-菜单，button-按钮，api-接口 */
    @TableField("permission_type")
    private String permissionType;

    /** 父级 ID */
    @TableField("parent_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 排序 */
    @TableField("sort")
    private Integer sort;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 状态：0-禁用，1-启用 */
    @TableField("status")
    private Integer status;
}
