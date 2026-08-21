package com.enterprise.ai.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用初始化配置
 */
@Slf4j
@Component
public class AppInitializer {

    @Value("${document.upload.path:./uploads}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        // 创建上传目录
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建上传目录: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建上传目录失败", e);
        }

        log.info("========================================");
        log.info("  企业级 AI 知识管理平台 启动成功");
        log.info("  Enterprise AI Knowledge Platform");
        log.info("========================================");
    }
}
