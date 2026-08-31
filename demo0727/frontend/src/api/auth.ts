import request from './request'
import type { LoginVO, UserVO, UserLoginDTO } from '@/types'

/** 用户登录 */
export function login(data: UserLoginDTO) {
  return request.post<LoginVO, LoginVO>('/auth/login', data)
}

/** 用户注册 */
export function register(data: any) {
  return request.post<UserVO, UserVO>('/auth/register', data)
}

/** 退出登录 */
export function logout() {
  return request.post('/auth/logout')
}

/** 刷新 Token */
export function refreshToken(token: string) {
  return request.post('/auth/refresh', null, {
    headers: { Authorization: `Bearer ${token}` },
  })
}
