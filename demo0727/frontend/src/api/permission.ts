import request from './request'
import type { Permission, Role, PageQuery } from '@/types'

/** 分页查询权限列表 */
export function pagePermissions(params: PageQuery) {
  return request.get('/permissions/page', { params })
}

/** 获取所有权限 */
export function listPermissions() {
  return request.get<Permission[], Permission[]>('/permissions/list')
}

/** 创建权限 */
export function createPermission(data: Partial<Permission>) {
  return request.post<Permission, Permission>('/permissions', data)
}

/** 更新权限 */
export function updatePermission(id: string, data: Partial<Permission>) {
  return request.put<Permission, Permission>(`/permissions/${id}`, data)
}

/** 删除权限 */
export function deletePermission(id: string) {
  return request.delete(`/permissions/${id}`)
}

/** 获取用户的角色列表 */
export function getUserRoles(userId: string) {
  return request.get<Role[], Role[]>(`/permissions/users/${userId}/roles`)
}

/** 为用户分配角色 */
export function assignUserRoles(userId: string, roleIds: string[]) {
  return request.post(`/permissions/users/${userId}/roles`, roleIds)
}
