/** Dashboard 仪表盘 mock 数据（后续接入真实接口后可移除） */

export interface DashboardStats {
  userCount: number
  documentCount: number
  knowledgeBaseCount: number
  todayVisitCount: number
}

export interface VisitTrend {
  dates: string[]
  visits: number[]
  documents: number[]
}

export interface RecentAction {
  id: number
  user: string
  action: string
  time: string
}

export interface Notice {
  id: number
  title: string
  time: string
  content: string
}

export const mockDashboardStats: DashboardStats = {
  userCount: 128,
  documentCount: 1024,
  knowledgeBaseCount: 12,
  todayVisitCount: 356,
}

export const mockVisitTrend: VisitTrend = {
  dates: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  visits: [120, 200, 150, 80, 270, 300, 260],
  documents: [20, 40, 30, 50, 60, 35, 45],
}

export const mockRecentActions: RecentAction[] = [
  { id: 1, user: '张三', action: '上传了文档《操作系统原理》', time: '2 分钟前' },
  { id: 2, user: '李四', action: '创建了知识库《计算机网络》', time: '15 分钟前' },
  { id: 3, user: '王五', action: '向 AI 提问了“什么是死锁”', time: '1 小时前' },
  { id: 4, user: '赵六', action: '更新了文档《数据结构》', time: '3 小时前' },
  { id: 5, user: '钱七', action: '删除了文档《旧版教材》', time: '昨天' },
]

export const mockNotices: Notice[] = [
  {
    id: 1,
    title: '系统升级公告',
    time: '2026-08-10',
    content: 'AI 知识平台将于今日完成 RAG 检索增强升级，问答响应更精准。',
  },
  {
    id: 2,
    title: '知识库维护通知',
    time: '2026-08-09',
    content: '本周日 02:00-04:00 将进行知识库索引重建，期间问答服务可能短暂不可用。',
  },
]
