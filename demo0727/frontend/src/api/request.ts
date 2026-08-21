import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { Result, RefreshTokenVO, UserVO } from '@/types'

/** Axios 实例 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 自定义请求配置：
 * - _retried: 该请求已因 401 重放过一次，再次 401 直接登出，防无限循环
 * - _skipAuthRefresh: 刷新/用户信息等内部请求，自身 401 时不再触发续期（防死锁）
 */
type AuthConfig = InternalAxiosRequestConfig & {
  _retried?: boolean
  _skipAuthRefresh?: boolean
}

// ---- 会话续期状态（模块级单例，供路由守卫与 401 拦截器共用并发去重） ----
let isRefreshing = false
let sessionPromise: Promise<boolean> | null = null
let pendingQueue: Array<(token: string | null) => void> = []

/** 请求拦截器 - 从内存 store 携带 Access Token */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 调用方显式指定了 Authorization（如 /auth/refresh 传 refreshToken）则不做覆盖
    if (!config.headers?.Authorization) {
      const store = useUserStore()
      if (store.accessToken && config.headers) {
        config.headers.Authorization = `Bearer ${store.accessToken}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 判断是否为“请求被取消/中止”错误（页面刷新、路由跳转、浏览器中断等，非真实故障） */
function isRequestAborted(error: unknown): boolean {
  if (!error) return false
  if (axios.isCancel(error)) return true
  const err = error as { code?: string; message?: string }
  return (
    err.code === 'ERR_CANCELED' ||
    /aborted|canceled/i.test(err.message || '')
  )
}

/**
 * 静默续期会话（单例 promise，并发去重）
 * 成功：更新内存 accessToken，并拉取 /users/me 刷新 userInfo/roles（角色信息实时化）
 * 失败：清空登录态，返回 false
 */
export function refreshSession(): Promise<boolean> {
  const store = useUserStore()
  if (!store.refreshToken) return Promise.resolve(false)
  if (!sessionPromise) {
    sessionPromise = doRefresh().finally(() => {
      sessionPromise = null
    })
  }
  return sessionPromise
}

async function doRefresh(): Promise<boolean> {
  const store = useUserStore()
  try {
    const data = await service.post<RefreshTokenVO, RefreshTokenVO>(
      '/auth/refresh',
      null,
      { headers: { Authorization: `Bearer ${store.refreshToken}` }, _skipAuthRefresh: true } as AuthConfig
    )
    if (!data?.accessToken) {
      store.logout()
      return false
    }
    store.setToken(data.accessToken)

    // 续期成功后刷新用户信息/角色（不阻塞会话，失败保留旧 userInfo）
    try {
      const user = await service.get<UserVO, UserVO>('/users/me', { _skipAuthRefresh: true } as AuthConfig)
      if (user) store.setUserInfo(user)
    } catch {
      // 用户信息刷新失败不阻塞会话
    }
    return true
  } catch {
    store.logout()
    return false
  }
}

/** 清空登录态并跳转登录页 */
function clearAndGoLogin(msg: string) {
  useUserStore().logout()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
  ElMessage.error(msg)
}

/**
 * 统一处理 401（业务码或 HTTP 401）：
 * - 刷新/用户信息等内部请求自身失败 → 直接 reject（由 doRefresh 兜底）
 * - 已重放过一次仍 401 → 登出，防无限循环
 * - 刷新进行中 → 压入等待队列，刷新完成后用新 token 重放
 * - 否则发起一次刷新，成功后重放原请求
 */
async function handleUnauthorized(config: AuthConfig): Promise<any> {
  if (config._skipAuthRefresh) {
    return Promise.reject(new Error('刷新失败'))
  }
  if (config._retried) {
    clearAndGoLogin('登录已过期，请重新登录')
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }
  config._retried = true

  // 已有刷新在进行中：等待完成后重放
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push((token) => {
        if (token) {
          config.headers = config.headers || {}
          config.headers.Authorization = `Bearer ${token}`
          service.request(config).then(resolve).catch(reject)
        } else {
          reject(new Error('登录已过期，请重新登录'))
        }
      })
    })
  }

  isRefreshing = true
  try {
    const ok = await refreshSession()
    const token = useUserStore().accessToken
    pendingQueue.forEach((cb) => cb(ok ? token : null))
    pendingQueue = []
    if (!ok) {
      clearAndGoLogin('登录已过期，请重新登录')
      throw new Error('登录已过期，请重新登录')
    }
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
    return service.request(config) // 重放原请求
  } finally {
    isRefreshing = false
  }
}

/** 响应拦截器 - 统一处理响应和错误 */
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data
    const config = response.config as AuthConfig

    // 业务成功
    if (res.code === 200) {
      return res.data
    }

    // Token 过期或未授权：走自动续期重放
    if (res.code === 401) {
      return handleUnauthorized(config)
    }

    // 刷新/静默续期请求的业务错误不弹窗（避免与登出提示重复）
    if (config._skipAuthRefresh) {
      return Promise.reject(new Error(res.message))
    }

    // 其他业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    // 请求被取消/中止：静默处理，不弹“网络异常”提示、不抛未捕获异常
    if (isRequestAborted(error)) {
      return Promise.resolve(undefined as never)
    }

    // HTTP 错误
    let message = '网络异常，请稍后重试'
    const config = (error?.config || {}) as AuthConfig
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // Token 过期：走自动续期重放
          return handleUnauthorized(config)
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
      }
    }
    // 静默续期请求不弹窗
    if (!config._skipAuthRefresh) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default service
