package com.enterprise.ai.document.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档更新请求 DTO
 */
@Data
public class DocumentUpdateDTO {

    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    private String tags;

    @Size(max = 500, message = "摘要长度不能超过 500 个字符")
    private String summary;

    private Integer status;
}
