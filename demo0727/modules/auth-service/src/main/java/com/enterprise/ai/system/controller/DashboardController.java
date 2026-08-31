package com.enterprise.ai.system.controller;

import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.system.service.DashboardService;
import com.enterprise.ai.system.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器
 */
@Tag(name = "仪表盘", description = "首页统计接口")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public Result<DashboardVO> getStats() {
        return Result.success(dashboardService.getStats());
    }
}