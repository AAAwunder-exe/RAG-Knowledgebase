import request from './request'

/** 获取全部系统配置（登录后使用） */
export function getSystemConfig() {
  return request.get<Record<string, string>, Record<string, string>>('/system/config')
}

/** 保存系统配置 */
export function saveSystemConfig(data: Record<string, string>) {
  return request.put('/system/config', data)
}

/** 获取公开系统配置（登录页展示使用，仅基础信息） */
export function getPublicSystemConfig() {
  return request.get<Record<string, string>, Record<string, string>>('/system/config/public')
}
