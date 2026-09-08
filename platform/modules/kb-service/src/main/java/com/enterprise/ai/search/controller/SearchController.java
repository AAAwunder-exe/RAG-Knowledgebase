package com.enterprise.ai.search.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.search.dto.SearchRequestDTO;
import com.enterprise.ai.search.service.SearchService;
import com.enterprise.ai.search.vo.SearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器
 */
@Tag(name = "文档搜索", description = "文档搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "关键词搜索（全局）")
    @GetMapping
    public Result<Page<SearchResultVO>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long knowledgeId,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        SearchRequestDTO dto = new SearchRequestDTO();
        dto.setKeyword(keyword);
        dto.setKnowledgeId(knowledgeId);
        dto.setScope(scope);
        dto.setType(type);
        dto.setCurrent(current);
        dto.setSize(size);
        
        return Result.success(searchService.search(dto));
    }

    @Operation(summary = "POST 搜索（支持复杂条件）")
    @PostMapping
    public Result<Page<SearchResultVO>> searchPost(@RequestBody SearchRequestDTO request) {
        return Result.success(searchService.search(request));
    }

    @Operation(summary = "标题搜索")
    @GetMapping("/title")
    public Result<Page<SearchResultVO>> searchByTitle(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(searchService.searchByTitle(keyword, current, size));
    }

    @Operation(summary = "内容搜索")
    @GetMapping("/content")
    public Result<Page<SearchResultVO>> searchByContent(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(searchService.searchByContent(keyword, current, size));
    }

    @Operation(summary = "在指定知识库中搜索")
    @GetMapping("/knowledge/{knowledgeId}")
    public Result<Page<SearchResultVO>> searchInKnowledgeBase(
            @PathVariable Long knowledgeId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(searchService.searchInKnowledgeBase(knowledgeId, keyword, current, size));
    }
}
