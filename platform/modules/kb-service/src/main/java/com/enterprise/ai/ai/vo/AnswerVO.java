package com.enterprise.ai.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 问答响应 VO
 */
@Data
public class AnswerVO {

    /** AI 回答内容 */
    private String answer;

    /** 使用的 Token 数量 */
    private Integer usage;

    /** 参考的文档列表 */
    private List<ReferenceDoc> references;

    /** 模型名称 */
    private String model;

    @Data
    public static class ReferenceDoc {
        /** 文档 ID */
        private Long documentId;
        /** 文档标题 */
        private String title;
        /** 相关度分数 */
        private Double score;
        /** 相关内容片段 */
        private String snippet;
    }
}
