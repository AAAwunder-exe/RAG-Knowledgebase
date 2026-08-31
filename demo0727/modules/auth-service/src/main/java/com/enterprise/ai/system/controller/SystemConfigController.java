package com.enterprise.ai.system.controller;

import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置控制器
 */
@Tag(name = "系统配置", description = "系统设置管理接口")
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @PreAuthorize("hasAuthority('menu:settings')")
    @Operation(summary = "获取全部系统配置")
    @GetMapping
    public Result<Map<String, String>> getAllConfigs() {
        return Result.success(systemConfigService.getAllConfigs());
    }

    @PreAuthorize("hasAuthority('menu:settings')")
    @Operation(summary = "保存系统配置")
    @PutMapping
    public Result<Void> saveConfigs(@RequestBody Map<String, String> configs) {
        systemConfigService.saveConfigs(configs);
        return Result.success();
    }

    @Operation(summary = "获取公开系统配置（登录页展示使用，仅返回基础信息）")
    @GetMapping("/public")
    public Result<Map<String, String>> getPublicConfigs() {
        Map<String, String> all = systemConfigService.getAllConfigs();
        Map<String, String> publicConfigs = all.entrySet().stream()
            .filter(e -> e.getKey().startsWith("system."))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        return Result.success(publicConfigs);
    }
}
