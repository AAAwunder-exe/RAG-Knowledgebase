package com.enterprise.ai.ai.service.impl;

import com.enterprise.ai.ai.dto.AskQuestionDTO;
import com.enterprise.ai.ai.service.AIService;
import com.enterprise.ai.ai.vo.AnswerVO;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 服务实现类
 * 通过 HTTP 调用 Python RAG 引擎（FAISS + BM25 + Reranker + Qwen3.6）
 * RAG 服务地址见 application.yml: ai.rag.base-url
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Value("${ai.rag.base-url:http://localhost:8001}")
    private String ragBaseUrl;

    @Value("${ai.rag.timeout:120000}")
    private int timeoutMillis;

    @Value("${ai.rag.top-k:3}")
    private int topK;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(timeoutMillis);
        this.restTemplate = new RestTemplate(factory);
        log.info("RAG 客户端初始化完成: base-url={}, timeout={}ms", ragBaseUrl, timeoutMillis);
    }

    @Override
    public AnswerVO askQuestion(AskQuestionDTO question) {
        log.info("AI 问答请求: {}", question.getQuestion());

        // 构造 RAG 请求（kb_id 可选，指定后只在该知识库内检索；history 作为多轮上下文）
        Map<String, Object> request = new HashMap<>();
        request.put("question", question.getQuestion());
        request.put("top_k", topK);
        request.put("kb_id", question.getKnowledgeId());
        if (question.getHistory() != null && !question.getHistory().isEmpty()) {
            request.put("history", question.getHistory());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // 调用 Python RAG 服务（本地服务偶发超时，重试 1 次）
        RagResponse response = null;
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                response = restTemplate.postForObject(
                        ragBaseUrl + "/ask",
                        entity,
                        RagResponse.class
                );
                break;
            } catch (Exception e) {
                lastError = e;
                log.warn("调用 RAG 服务失败（第 {} 次，共 2 次）: {}", attempt, e.getMessage());
            }
        }
        if (response == null) {
            log.error("调用 RAG 服务失败（重试后仍失败）: {}",
                    lastError == null ? "unknown" : lastError.getMessage());
            throw new BusinessException(ResultCode.FAILURE, "AI 问答服务暂时不可用，请稍后重试");
        }

        if (response.getAnswer() == null) {
            throw new BusinessException(ResultCode.FAILURE, "AI 问答服务返回空响应");
        }

        // 映射到 AnswerVO
        AnswerVO answer = new AnswerVO();
        answer.setAnswer(response.getAnswer());
        answer.setModel("Qwen3.6-35B-A3B + RAG");
        answer.setUsage(0);

        // 将检索到的教材片段映射为参考文档
        List<AnswerVO.ReferenceDoc> refs = new ArrayList<>();
        if (response.getContexts() != null) {
            for (String ctx : response.getContexts()) {
                AnswerVO.ReferenceDoc ref = new AnswerVO.ReferenceDoc();
                ref.setSnippet(ctx);
                refs.add(ref);
            }
        }
        answer.setReferences(refs);

        log.info("AI 问答完成: cached={}, contexts={}",
                response.getCached(), refs.size());
        return answer;
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "Qwen3.6-35B-A3B (本地 vLLM + RAG)",
                "BAAI/bge-small-zh-v1.5 (向量化)",
                "BAAI/bge-reranker-v2-m3 (精排)"
        );
    }

    /**
     * RAG 服务响应体（对应 main.py 的 AskResponse）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RagResponse {
        private String answer;
        private List<String> contexts;
        private Boolean cached;
    }
}
