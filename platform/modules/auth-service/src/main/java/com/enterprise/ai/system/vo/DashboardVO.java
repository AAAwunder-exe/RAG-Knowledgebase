package com.enterprise.ai.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 仪表盘统计数据
 */
@Data
@Builder
@Schema(description = "仪表盘统计数据")
public class DashboardVO {

    @Schema(description = "用户总数")
    private long userCount;

    @Schema(description = "文档总数")
    private long documentCount;

    @Schema(description = "知识库总数")
    private long knowledgeBaseCount;
}