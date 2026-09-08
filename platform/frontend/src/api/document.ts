import request from './request'
import type { PageResult, PageQuery, Document } from '@/types'

/** 分页查询知识库下的文档 */
export function pageDocuments(params: PageQuery) {
  return request.get<PageResult<Document>, PageResult<Document>>('/documents', { params })
}

/** 根据 ID 获取文档详情 */
export function getDocumentById(id: string) {
  return request.get<Document, Document>(`/documents/${id}`)
}

/**
 * 上传文档（multipart/form-data）
 * 注意：request.ts 全局 Content-Type 默认为 application/json，此处需逐请求覆盖为 multipart，
 * 否则后端无法解析 file 字段；上传大文件时全局 15s 超时不够，单独放宽到 120s
 */
export function uploadDocument(formData: FormData) {
  return request.post<Document, Document>('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

/** 删除文档 */
export function deleteDocument(id: string) {
  return request.delete(`/documents/${id}`)
}
