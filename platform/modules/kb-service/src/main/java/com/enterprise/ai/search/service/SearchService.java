package com.enterprise.ai.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.search.dto.SearchRequestDTO;
import com.enterprise.ai.search.vo.SearchResultVO;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 执行搜索
     */
    Page<SearchResultVO> search(SearchRequestDTO request);

    /**
     * 关键词搜索（全局）
     */
    Page<SearchResultVO> searchByKeyword(String keyword, Integer current, Integer size);

    /**
     * 在指定知识库中搜索
     */
    Page<SearchResultVO> searchInKnowledgeBase(Long knowledgeId, String keyword, Integer current, Integer size);

    /**
     * 标题搜索
     */
    Page<SearchResultVO> searchByTitle(String keyword, Integer current, Integer size);

    /**
     * 内容搜索
     */
    Page<SearchResultVO> searchByContent(String keyword, Integer current, Integer size);
}
