package com.enterprise.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 知识服务 - 应用主入口（知识库/文档/搜索/AI 问答/RAG 对接）
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.enterprise.ai.**.mapper")
public class KbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbServiceApplication.class, args);
    }
}
