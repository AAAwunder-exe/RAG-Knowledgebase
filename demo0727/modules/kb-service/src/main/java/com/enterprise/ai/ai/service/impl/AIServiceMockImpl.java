package com.enterprise.ai.ai.service.impl;

import com.enterprise.ai.ai.dto.AskQuestionDTO;
import com.enterprise.ai.ai.service.AIService;
import com.enterprise.ai.ai.vo.AnswerVO;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI 服务 Mock 实现（已停用）
 * 真实实现见 {@link AIServiceImpl}，通过 HTTP 调用 Python RAG 引擎。
 * 此类保留作为参考，未加 @Service 注解，不会被 Spring 管理。
 */
@Slf4j
public class AIServiceMockImpl implements AIService {

    @Override
    public AnswerVO askQuestion(AskQuestionDTO question) {
        // TODO: 未来接入真正的 AI 模型
        // 实现步骤：
        // 1. 使用向量数据库检索相关文档
        // 2. 将相关文档作为上下文
        // 3. 调用大模型生成回答
        // 4. 返回回答和参考文档

        log.info("AI 问答请求（Mock）: {}", question.getQuestion());

        AnswerVO answer = new AnswerVO();
        answer.setAnswer("抱歉，AI 问答功能正在开发中，敬请期待！");
        answer.setUsage(0);
        answer.setModel("mock-model");
        answer.setReferences(new ArrayList<>());

        return answer;
    }

    @Override
    public List<String> getAvailableModels() {
        return Arrays.asList(
            "deepseek-chat",
            "deepseek-coder",
            "gpt-4",
            "claude-3"
        );
    }
}
