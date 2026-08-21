package com.enterprise.ai.system.service;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {

    /**
     * 获取全部配置（键值对）
     */
    Map<String, String> getAllConfigs();

    /**
     * 保存配置（存在则更新，不存在则新增）
     */
    void saveConfigs(Map<String, String> configs);
}
