import request from './request'
import type {
  PageResult,
  PageQuery,
  KnowledgeBase,
  KnowledgeBaseCreateDTO,
  KnowledgeBaseUpdateDTO,
} from '@/types'

/** 分页查询知识库列表（管理员） */
export function pageKnowledgeBases(params: PageQuery) {
  return request.get<PageResult<KnowledgeBase>, PageResult<KnowledgeBase>>('/knowledge-bases/page', { params })
}

/** 根据 ID 获取知识库 */
export function getKnowledgeBaseById(id: string) {
  return request.get<KnowledgeBase, KnowledgeBase>(`/knowledge-bases/${id}`)
}

/** 创建知识库 */
export function createKnowledgeBase(data: KnowledgeBaseCreateDTO) {
  return request.post<KnowledgeBase, KnowledgeBase>('/knowledge-bases', data)
}

/** 更新知识库 */
export function updateKnowledgeBase(id: string, data: KnowledgeBaseUpdateDTO) {
  return request.put<KnowledgeBase, KnowledgeBase>(`/knowledge-bases/${id}`, data)
}

/** 删除知识库 */
export function deleteKnowledgeBase(id: string) {
  return request.delete(`/knowledge-bases/${id}`)
}
