package com.enterprise.ai.document.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.common.result.Result;
import com.enterprise.ai.document.dto.DocumentUpdateDTO;
import com.enterprise.ai.document.dto.DocumentUploadDTO;
import com.enterprise.ai.document.service.DocumentService;
import com.enterprise.ai.document.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理控制器
 */
@Tag(name = "文档管理", description = "文档上传、查询、更新、删除等接口")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('menu:document')")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('api:document:upload')")
    public Result<DocumentVO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeId") Long knowledgeId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String summary) {
        
        DocumentUploadDTO dto = new DocumentUploadDTO();
        dto.setKnowledgeId(knowledgeId);
        dto.setTitle(title);
        dto.setTags(tags);
        dto.setSummary(summary);
        
        return Result.success(documentService.uploadDocument(dto, file));
    }

    @Operation(summary = "分页查询知识库下的文档")
    @GetMapping
    public Result<Page<DocumentVO>> pageDocuments(
            @RequestParam Long knowledgeId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(documentService.pageDocumentsByKnowledgeId(knowledgeId, current, size, keyword));
    }

    @Operation(summary = "根据 ID 获取文档详情")
    @GetMapping("/{id}")
    public Result<DocumentVO> getDocumentById(@PathVariable Long id) {
        return Result.success(documentService.getDocumentById(id));
    }

    @Operation(summary = "获取文档内容")
    @GetMapping("/{id}/content")
    public Result<String> getDocumentContent(@PathVariable Long id) {
        return Result.success(documentService.getDocumentContent(id));
    }

    @Operation(summary = "更新文档信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('api:document:edit')")
    public Result<DocumentVO> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateDTO dto) {
        return Result.success(documentService.updateDocument(id, dto));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('api:document:delete')")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success();
    }
}
