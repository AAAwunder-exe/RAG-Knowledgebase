package com.enterprise.ai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ai.system.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联 Mapper 接口
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据用户 ID 删除所有角色关联
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId} AND deleted = 0")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 批量插入用户角色关联
     */
    @Insert("<script>" +
            "INSERT INTO sys_user_role (id, user_id, role_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.userId}, #{item.roleId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<UserRole> list);
}
