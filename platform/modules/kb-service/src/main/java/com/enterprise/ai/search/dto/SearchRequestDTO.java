package com.enterprise.ai.search.dto;

import lombok.Data;

/**
 * 搜索请求 DTO
 */
@Data
public class SearchRequestDTO {

    /** 搜索关键词 */
    private String keyword;

    /** 知识库 ID（可选，不传则搜索所有） */
    private Long knowledgeId;

    /** 搜索范围：title-仅标题，content-仅内容，all-标题+内容 */
    private String scope = "all";

    /** 文档类型筛选（可选） */
    private String type;

    /** 当前页码 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 10;
}
