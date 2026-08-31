package com.enterprise.ai.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态菜单节点 VO
 * 用于向前端返回当前用户可见的菜单树，前端据此渲染侧边栏菜单与路由。
 */
@Data
@Schema(description = "动态菜单节点")
public class MenuNode {

    @Schema(description = "菜单 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "权限编码，如 menu:user")
    private String code;

    @Schema(description = "父级菜单 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @Schema(description = "前端路由路径，如 /knowledge")
    private String path;

    @Schema(description = "前端组件路径，如 /knowledge/index.vue")
    private String component;

    @Schema(description = "菜单图标（Element Plus 图标名）")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子菜单")
    private List<MenuNode> children = new ArrayList<>();
}