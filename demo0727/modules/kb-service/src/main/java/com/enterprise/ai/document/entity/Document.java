package com.enterprise.ai.document.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    /** 知识库 ID */
    @TableField("knowledge_id")
    private Long knowledgeId;

    /** 文档标题 */
    @TableField("title")
    private String title;

    /** 文件路径 */
    @TableField("file_path")
    private String filePath;

    /** 原始文件名 */
    @TableField("original_name")
    private String originalName;

    /** 文件类型：pdf, word, markdown, txt */
    @TableField("type")
    private String type;

    /** 文件大小（字节） */
    @TableField("size")
    private Long size;

    /** 上传人 ID */
    @TableField("creator_id")
    private Long creatorId;

    /** 上传人姓名 */
    @TableField("creator_name")
    private String creatorName;

    /** 状态：0-禁用，1-正常 */
    @TableField("status")
    private Integer status;

    /** 分类标签 */
    @TableField("tags")
    private String tags;

    /** 摘要/描述 */
    @TableField("summary")
    private String summary;

    /** 全文内容（用于搜索） */
    @TableField("content")
    private String content;
}
