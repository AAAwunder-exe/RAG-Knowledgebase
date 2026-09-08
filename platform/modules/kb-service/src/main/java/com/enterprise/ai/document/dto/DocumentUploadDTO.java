package com.enterprise.ai.document.dto;

import lombok.Data;

/**
 * 文档上传请求 DTO
 */
@Data
public class DocumentUploadDTO {

    /** 知识库 ID */
    private Long knowledgeId;

    /** 文档标题（可选，默认使用文件名） */
    private String title;

    /** 分类标签（多个用逗号分隔） */
    private String tags;

    /** 摘要/描述 */
    private String summary;
}
