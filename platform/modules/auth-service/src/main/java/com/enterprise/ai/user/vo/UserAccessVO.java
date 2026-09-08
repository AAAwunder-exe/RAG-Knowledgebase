package com.enterprise.ai.user.vo;

import com.enterprise.ai.system.vo.MenuNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 当前用户的访问权限信息 VO
 * 包含动态菜单树与权限码集合，供前端渲染菜单、控制路由与按钮显隐。
 */
@Data
@Schema(description = "当前用户访问权限信息")
public class UserAccessVO {

    @Schema(description = "动态菜单树")
    private List<MenuNode> menus;

    @Schema(description = "权限码集合（含 menu: 前缀与 api: 前缀）")
    private List<String> permissions;

    public UserAccessVO() {
    }

    public UserAccessVO(List<MenuNode> menus, List<String> permissions) {
        this.menus = menus;
        this.permissions = permissions;
    }
}