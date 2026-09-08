package com.enterprise.ai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * AI 问答请求 DTO
 */
@Data
public class AskQuestionDTO {

    /** 问题内容 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 知识库 ID（可选，指定在哪个知识库中回答） */
    private Long knowledgeId;

    /** 对话历史（多轮对话时使用） */
    private List<ChatMessage> history;

    /** 模型名称（可选，使用默认配置） */
    private String model;

    /** 最大 Token 数量 */
    private Integer maxTokens;

    @Data
    public static class ChatMessage {
        /** 角色：user, assistant */
        private String role;
        /** 消息内容 */
        private String content;
    }
}
