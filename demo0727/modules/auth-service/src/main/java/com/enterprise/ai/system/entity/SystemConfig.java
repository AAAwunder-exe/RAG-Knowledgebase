package com.enterprise.ai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体（键值对形式存储）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SystemConfig extends BaseEntity {

    /** 配置键 */
    @TableField("config_key")
    private String configKey;

    /** 配置值 */
    @TableField("config_value")
    private String configValue;

    /** 备注 */
    @TableField("remark")
    private String remark;
}
