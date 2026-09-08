package com.enterprise.ai.system.service.impl;

import com.enterprise.ai.system.service.DashboardService;
import com.enterprise.ai.system.vo.DashboardVO;
import com.enterprise.ai.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 仪表盘服务实现
 *
 * 跨服务聚合（唯一服务间调用）：
 * - userCount：本地查库
 * - documentCount / knowledgeBaseCount：调用 kb-service 的 /internal/dashboard/stats
 * - kb-service 不可用时降级返回 0（Dashboard 不因下游故障失败）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** kb-service 内部接口地址（KB_SERVICE_URL 环境变量覆盖） */
    @Value("${kb.internal.base-url:http://localhost:8082}")
    private String kbBaseUrl;

    @Override
    public DashboardVO getStats() {
        long userCount = userMapper.selectCount(null);
        long[] kbCounts = fetchKbStats();
        return DashboardVO.builder()
                .userCount(userCount)
                .documentCount(kbCounts[0])
                .knowledgeBaseCount(kbCounts[1])
                .build();
    }

    /**
     * 调用 kb-service 内部接口获取文档/知识库计数
     * 返回 {documentCount, knowledgeBaseCount}，失败返回 {0, 0}
     */
    private long[] fetchKbStats() {
        try {
            String url = kbBaseUrl + "/internal/dashboard/stats";
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data != null) {
                return new long[]{
                        data.path("documentCount").asLong(0),
                        data.path("knowledgeBaseCount").asLong(0)
                };
            }
            log.warn("kb-service 内部接口未返回 data，body={}", body);
        } catch (Exception e) {
            log.warn("调用 kb-service Dashboard 统计失败，降级返回 0：{}", e.getMessage());
        }
        return new long[]{0, 0};
    }
}
