package com.enterprise.ai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ai.system.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联 Mapper 接口
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 根据角色 ID 删除所有权限关联
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId} AND deleted = 0")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关联
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.roleId}, #{item.permissionId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<RolePermission> list);
}
