import request from './request'
import type { Role, Permission, PageResult, PageQuery } from '@/types'

/** 分页查询角色列表 */
export function pageRoles(params: PageQuery) {
  return request.get<PageResult<Role>, PageResult<Role>>('/roles/page', { params })
}

/** 获取所有角色 */
export function listRoles() {
  return request.get<Role[], Role[]>('/roles/list')
}

/** 根据 ID 获取角色 */
export function getRoleById(id: string) {
  return request.get<Role, Role>(`/roles/${id}`)
}

/** 创建角色 */
export function createRole(data: Partial<Role>) {
  return request.post<Role, Role>('/roles', data)
}

/** 更新角色 */
export function updateRole(id: string, data: Partial<Role>) {
  return request.put<Role, Role>(`/roles/${id}`, data)
}

/** 删除角色 */
export function deleteRole(id: string) {
  return request.delete(`/roles/${id}`)
}

/** 获取角色的权限 ID 列表 */
export function getRolePermissions(id: string) {
  return request.get<string[], string[]>(`/roles/${id}/permissions`)
}

/** 为角色分配权限 */
export function assignRolePermissions(id: string, permissionIds: string[]) {
  return request.post(`/roles/${id}/permissions`, permissionIds)
}

/** 获取所有启用的权限列表（供角色分配使用） */
export function listPermissions() {
  return request.get<Permission[], Permission[]>('/roles/permissions/list')
}
