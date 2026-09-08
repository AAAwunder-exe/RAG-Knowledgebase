import request from './request'

export interface DashboardStats {
  userCount: number
  documentCount: number
  knowledgeBaseCount: number
}

/** 获取仪表盘统计数据 */
export function getDashboardStats() {
  return request.get<DashboardStats, DashboardStats>('/dashboard/stats')
}