import request from './request'
import type { AnswerVO, AskQuestionDTO } from '@/types'

/** AI 问答（RAG 调用 vLLM 较慢，超时给 2 分钟） */
export function askQuestion(data: AskQuestionDTO) {
  return request.post<AnswerVO, AnswerVO>('/ai/ask', data, { timeout: 120000 })
}

/** 获取可用模型列表 */
export function getAvailableModels() {
  return request.get<string[], string[]>('/ai/models')
}
