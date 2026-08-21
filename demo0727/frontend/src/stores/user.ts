import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserVO } from '@/types'
import { useAiStore } from '@/stores/ai'

/** refreshToken 持久化 key（与 accessToken 分开存储） */
const REFRESH_TOKEN_KEY = 'refresh_token'

/**
 * 用户状态管理
 * 管理 Token、用户信息、登录/登出逻辑
 *
 * 安全约定：
 * - accessToken 只存内存（Pinia），不落 localStorage，降低 XSS 泄露风险
 * - refreshToken 单独持久化到 localStorage，用于 401 自动续期 / 页面刷新静默续期
 * - userInfo 保留 localStorage（首屏 Header/菜单立即有值），静默续期后由 /users/me 覆盖为最新
 */
export const useUserStore = defineStore('user', () => {
  // Access Token：仅内存态
  const accessToken = ref<string>('')
  // Refresh Token：单独持久化
  const refreshToken = ref<string>(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  // 用户信息
  const userInfo = ref<UserVO | null>(
    localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null
  )

  /** 是否已登录（内存中存在 accessToken 即视为已登录） */
  const isLoggedIn = computed(() => !!accessToken.value)

  /** 用户角色 */
  const roles = computed(() => userInfo.value?.roles || [])

  /** 设置 Access Token（仅内存） */
  function setToken(t: string) {
    accessToken.value = t
  }

  /** 设置 Refresh Token（持久化到 localStorage） */
  function setRefreshToken(t: string) {
    refreshToken.value = t
    if (t) {
      localStorage.setItem(REFRESH_TOKEN_KEY, t)
    } else {
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    }
  }

  /** 设置用户信息 */
  function setUserInfo(info: UserVO) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  /** 登录成功后设置数据 */
  function loginSuccess(accessToken: string, refreshToken: string, user: UserVO) {
    setToken(accessToken)
    setRefreshToken(refreshToken)
    setUserInfo(user)
  }

  /** 退出登录 - 清除状态 */
  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem('userInfo')
    localStorage.removeItem('token') // 清理旧版本残留的 accessToken key
    // 登出时清空 AI 对话记录（避免下一个登录账号看到上一个账号的历史）
    const aiStore = useAiStore()
    aiStore.clearMessages()
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    roles,
    setToken,
    setRefreshToken,
    setUserInfo,
    loginSuccess,
    logout,
  }
})
