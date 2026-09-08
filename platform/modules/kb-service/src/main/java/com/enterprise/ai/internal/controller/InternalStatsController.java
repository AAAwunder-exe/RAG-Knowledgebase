package com.enterprise.ai.internal.controller;

import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.document.mapper.DocumentMapper;
import com.enterprise.ai.knowledge.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 内部接口：供 auth-service 跨服务聚合 Dashboard 统计
 * 仅服务间调用（/internal/** 已放行），不对外暴露端口
 */
@RestController
@RequestMapping("/internal/dashboard")
@RequiredArgsConstructor
public class InternalStatsController {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @GetMapping("/stats")
    public Result<Map<String, Long>> getStats() {
        return Result.success(Map.of(
            "documentCount", documentMapper.selectCount(null),
            "knowledgeBaseCount", knowledgeBaseMapper.selectCount(null)
        ));
    }
}
