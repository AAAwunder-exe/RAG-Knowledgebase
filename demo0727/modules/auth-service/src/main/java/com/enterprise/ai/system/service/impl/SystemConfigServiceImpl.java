package com.enterprise.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.enterprise.ai.system.entity.SystemConfig;
import com.enterprise.ai.system.mapper.SystemConfigMapper;
import com.enterprise.ai.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public Map<String, String> getAllConfigs() {
        return systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getConfigKey))
            .stream()
            .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue, (a, b) -> a));
    }

    @Override
    @Transactional
    public void saveConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }

        Map<String, SystemConfig> existing = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().in(SystemConfig::getConfigKey, configs.keySet()))
            .stream()
            .collect(Collectors.toMap(SystemConfig::getConfigKey, Function.identity(), (a, b) -> a));

        configs.forEach((key, value) -> {
            SystemConfig config = existing.get(key);
            if (config == null) {
                config = new SystemConfig();
                config.setId(IdWorker.getId());
                config.setConfigKey(key);
                config.setConfigValue(value);
                systemConfigMapper.insert(config);
            } else {
                config.setConfigValue(value);
                systemConfigMapper.updateById(config);
            }
        });

        log.info("系统配置已保存: {} 项", configs.size());
    }
}
