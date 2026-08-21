package com.enterprise.ai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ai.system.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper 接口
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
