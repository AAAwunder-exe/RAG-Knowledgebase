package com.enterprise.ai.ai.controller;

import com.enterprise.ai.ai.dto.AskQuestionDTO;
import com.enterprise.ai.ai.service.AIService;
import com.enterprise.ai.ai.vo.AnswerVO;
import com.enterprise.ai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 服务控制器
 */
@Tag(name = "AI 服务", description = "AI 问答接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @Operation(summary = "AI 问答")
    @PostMapping("/ask")
    public Result<AnswerVO> askQuestion(@Valid @RequestBody AskQuestionDTO question) {
        return Result.success(aiService.askQuestion(question));
    }

    @Operation(summary = "获取可用模型列表")
    @GetMapping("/models")
    public Result<List<String>> getAvailableModels() {
        return Result.success(aiService.getAvailableModels());
    }
}
