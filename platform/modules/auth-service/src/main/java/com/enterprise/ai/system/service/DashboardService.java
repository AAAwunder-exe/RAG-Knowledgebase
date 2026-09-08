package com.enterprise.ai.system.service;

import com.enterprise.ai.system.vo.DashboardVO;

/**
 * 仪表盘服务接口
 */
public interface DashboardService {

    /**
     * 获取仪表盘统计数据
     */
    DashboardVO getStats();
}