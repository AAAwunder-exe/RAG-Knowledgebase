/**
 * 全局类型定义
 * 与后端 DTO/VO 对应
 */

/** 统一响应格式 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页响应（MyBatis Plus Page 结构） */
export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 用户信息 VO */
export interface UserVO {
  id: string
  username: string
  realName: string
  email: string
  phone: string
  avatar: string
  status: number // 0-禁用 1-启用
  createTime: string
  roles: string[]
}

/** 登录响应 VO */
export interface LoginVO {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserVO
}

/** Token 刷新响应 VO */
export interface RefreshTokenVO {
  accessToken: string
  tokenType: string
  expiresIn: number
}

/** 登录请求 DTO */
export interface UserLoginDTO {
  username: string
  password: string
  captchaUuid?: string
  captchaCode?: string
}

/** 验证码响应 VO */
export interface CaptchaVO {
  uuid: string
  imgBase64: string
}

/** 创建用户 DTO（管理员） */
export interface UserCreateDTO {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  status?: number
}

/** 更新用户 DTO（管理员） */
export interface UserAdminUpdateDTO {
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
}

/** 用户更新 DTO */
export interface UserUpdateDTO {
  realName?: string
  email?: string
  phone?: string
  avatar?: string
}

/** 角色实体 */
export interface Role {
  id: string
  roleName: string
  roleCode: string
  description: string
  sort: number
  status: number
  createTime?: string
  updateTime?: string
}

/** 权限实体 */
export interface Permission {
  id: string
  permissionName: string
  permissionCode: string
  permissionType: string // menu | button | api
  parentId: string
  sort: number
  description: string
  status: number
  createTime?: string
  updateTime?: string
}

/** 查询参数 */
export interface PageQuery {
  current?: number
  size?: number
  [key: string]: any
}

/** 知识库 VO（id 为 Long 经 ToStringSerializer 序列化，前端按字符串处理） */
export interface KnowledgeBase {
  id: string
  name: string
  description?: string
  creatorId?: string
  creatorName?: string
  status: number // 0-禁用 1-启用
  documentCount: number
  createTime?: string
  updateTime?: string
}

/** 创建知识库 DTO */
export interface KnowledgeBaseCreateDTO {
  name: string
  description?: string
}

/** 更新知识库 DTO */
export interface KnowledgeBaseUpdateDTO {
  name?: string
  description?: string
  status?: number
}

/** 文档 VO */
export interface Document {
  id: string
  knowledgeId: string
  title: string
  originalName: string
  type: string // 小写扩展名：pdf/doc/docx/md/markdown/txt
  size: number // 字节
  creatorName?: string
  status: number // 0-禁用 1-启用
  tags?: string
  summary?: string
  createTime?: string
  updateTime?: string
}

/** 文档上传参数（除文件外的表单字段） */
export interface DocumentUploadParams {
  knowledgeId: string
  title?: string
  tags?: string
  summary?: string
}

/** AI 问答请求 DTO */
export interface AskQuestionDTO {
  question: string
  knowledgeId?: number
  model?: string
  /** 多轮对话历史（本次登录保留，切页不丢） */
  history?: Array<{ role: string; content: string }>
}

/** AI 参考文档 */
export interface ReferenceDoc {
  documentId?: number
  title?: string
  score?: number
  snippet: string
}

/** AI 问答响应 VO */
export interface AnswerVO {
  answer: string
  usage: number
  references: ReferenceDoc[]
  model: string
}
