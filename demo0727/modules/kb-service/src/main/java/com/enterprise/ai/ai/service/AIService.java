package com.enterprise.ai.ai.service;

import com.enterprise.ai.ai.dto.AskQuestionDTO;
import com.enterprise.ai.ai.vo.AnswerVO;

import java.util.List;

/**
 * AI 服务接口
 * 向量化由 Python RAG 引擎（bge-small-zh-v1.5）在内部完成，后端不暴露 Embedding 接口
 */
public interface AIService {

    /**
     * AI 问答
     * 根据问题在知识库中检索相关文档，生成回答
     *
     * @param question 问题请求
     * @return AI 回答
     */
    AnswerVO askQuestion(AskQuestionDTO question);

    /**
     * 获取可用的模型列表
     *
     * @return 模型列表
     */
    List<String> getAvailableModels();
}
