package com.enterprise.ai.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.document.entity.Document;
import com.enterprise.ai.document.mapper.DocumentMapper;
import com.enterprise.ai.document.vo.DocumentVO;
import com.enterprise.ai.search.dto.SearchRequestDTO;
import com.enterprise.ai.search.service.SearchService;
import com.enterprise.ai.search.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 * 第一阶段：使用 MySQL 模糊搜索
 * 预留：未来可接入 Elasticsearch 或向量数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final DocumentMapper documentMapper;

    @Override
    public Page<SearchResultVO> search(SearchRequestDTO request) {
        String keyword = request.getKeyword();
        if (!StringUtils.hasText(keyword)) {
            return new Page<>(request.getCurrent(), request.getSize(), 0);
        }

        Page<Document> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();

        // 根据搜索范围构建条件
        String scope = request.getScope() != null ? request.getScope() : "all";
        switch (scope) {
            case "title":
                wrapper.like(Document::getTitle, keyword);
                break;
            case "content":
                wrapper.like(Document::getContent, keyword);
                break;
            case "all":
            default:
                wrapper.and(w -> w
                    .like(Document::getTitle, keyword)
                    .or()
                    .like(Document::getContent, keyword)
                );
                break;
        }

        // 知识库筛选
        if (request.getKnowledgeId() != null) {
            wrapper.eq(Document::getKnowledgeId, request.getKnowledgeId());
        }

        // 类型筛选
        if (StringUtils.hasText(request.getType())) {
            wrapper.eq(Document::getType, request.getType());
        }

        wrapper.eq(Document::getStatus, 1);
        wrapper.orderByDesc(Document::getCreateTime);

        Page<Document> result = documentMapper.selectPage(page, wrapper);
        return convertToSearchResultPage(result, keyword);
    }

    @Override
    public Page<SearchResultVO> searchByKeyword(String keyword, Integer current, Integer size) {
        SearchRequestDTO dto = new SearchRequestDTO();
        dto.setKeyword(keyword);
        dto.setCurrent(current);
        dto.setSize(size);
        return search(dto);
    }

    @Override
    public Page<SearchResultVO> searchInKnowledgeBase(Long knowledgeId, String keyword, Integer current, Integer size) {
        SearchRequestDTO dto = new SearchRequestDTO();
        dto.setKeyword(keyword);
        dto.setKnowledgeId(knowledgeId);
        dto.setCurrent(current);
        dto.setSize(size);
        return search(dto);
    }

    @Override
    public Page<SearchResultVO> searchByTitle(String keyword, Integer current, Integer size) {
        SearchRequestDTO dto = new SearchRequestDTO();
        dto.setKeyword(keyword);
        dto.setScope("title");
        dto.setCurrent(current);
        dto.setSize(size);
        return search(dto);
    }

    @Override
    public Page<SearchResultVO> searchByContent(String keyword, Integer current, Integer size) {
        SearchRequestDTO dto = new SearchRequestDTO();
        dto.setKeyword(keyword);
        dto.setScope("content");
        dto.setCurrent(current);
        dto.setSize(size);
        return search(dto);
    }

    private Page<SearchResultVO> convertToSearchResultPage(Page<Document> page, String keyword) {
        Page<SearchResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SearchResultVO> records = page.getRecords().stream()
            .map(doc -> {
                SearchResultVO result = new SearchResultVO();
                result.setDocument(convertToDocumentVO(doc));
                result.setHighlight(buildHighlight(doc, keyword));
                result.setScore(calculateScore(doc, keyword));
                return result;
            })
            .collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    private DocumentVO convertToDocumentVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }

    private String buildHighlight(Document doc, String keyword) {
        // 简单实现：如果标题包含关键词，返回标题；否则返回内容的匹配部分
        if (StringUtils.hasText(doc.getTitle()) && doc.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
            return doc.getTitle();
        }
        if (StringUtils.hasText(doc.getContent())) {
            int index = doc.getContent().toLowerCase().indexOf(keyword.toLowerCase());
            if (index >= 0) {
                int start = Math.max(0, index - 50);
                int end = Math.min(doc.getContent().length(), index + keyword.length() + 50);
                return doc.getContent().substring(start, end) + "...";
            }
        }
        return doc.getSummary() != null ? doc.getSummary() : "";
    }

    private Double calculateScore(Document doc, String keyword) {
        double score = 0.0;
        if (StringUtils.hasText(doc.getTitle()) && doc.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
            score += 1.0; // 标题匹配权重高
        }
        if (StringUtils.hasText(doc.getContent()) && doc.getContent().toLowerCase().contains(keyword.toLowerCase())) {
            score += 0.5; // 内容匹配
        }
        return score;
    }
}
