import request from './request'
import type { UserVO, UserUpdateDTO, UserCreateDTO, UserAdminUpdateDTO, PageResult, PageQuery } from '@/types'

/** 获取当前用户信息 */
export function getCurrentUser() {
  return request.get<UserVO, UserVO>('/users/me')
}

/** 根据 ID 获取用户信息 */
export function getUserById(id: string) {
  return request.get<UserVO, UserVO>(`/users/${id}`)
}

/** 更新当前用户信息 */
export function updateCurrentUser(data: UserUpdateDTO) {
  return request.put<UserVO, UserVO>('/users/me', data)
}

/** 修改密码 */
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put('/users/me/password', data)
}

/** 启用/禁用用户状态 */
export function updateUserStatus(id: string, status: number) {
  return request.put(`/users/${id}/status?status=${status}`)
}

/** 分页查询用户列表 */
export function pageUsers(params: PageQuery) {
  return request.get<PageResult<UserVO>, PageResult<UserVO>>('/users/page', { params })
}

/** 创建用户 */
export function createUser(data: UserCreateDTO) {
  return request.post<UserVO, UserVO>('/users', data)
}

/** 更新用户 */
export function updateUser(id: string, data: UserAdminUpdateDTO) {
  return request.put<UserVO, UserVO>(`/users/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id: string) {
  return request.delete(`/users/${id}`)
}
