package com.enterprise.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务 - 应用主入口（用户/登录/验证码/RBAC/系统配置/Dashboard）
 *
 * @author Enterprise AI Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.enterprise.ai.**.mapper")
public class AiKnowledgePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiKnowledgePlatformApplication.class, args);
    }
}
