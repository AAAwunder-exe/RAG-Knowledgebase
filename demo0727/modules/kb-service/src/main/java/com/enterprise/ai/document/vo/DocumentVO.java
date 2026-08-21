package com.enterprise.ai.document.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档信息 VO
 */
@Data
public class DocumentVO {

    /** 文档 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 知识库 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long knowledgeId;

    /** 文档标题 */
    private String title;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型 */
    private String type;

    /** 文件大小（字节） */
    private Long size;

    /** 上传人 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    /** 上传人姓名 */
    private String creatorName;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 分类标签 */
    private String tags;

    /** 摘要 */
    private String summary;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
