import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { refreshSession } from '@/api/request'

/** 路由表定义 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'Odometer' },
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/views/ai/index.vue'),
        meta: { title: 'AI 问答', icon: 'ChatDotRound' },
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库管理', icon: 'FolderOpened' },
      },
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/document/index.vue'),
        meta: { title: '文档管理', icon: 'Document' },
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled', parent: '权限管理' },
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/permission/index.vue'),
        meta: { title: '菜单权限', icon: 'Lock', parent: '权限管理' },
      },
      {
        path: 'user-role',
        name: 'UserRole',
        component: () => import('@/views/user-role/index.vue'),
        meta: { title: '用户角色绑定', icon: 'Connection', parent: '权限管理' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: '系统设置', icon: 'Setting' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/** 仅超级管理员可访问的页面（需与后端 @PreAuthorize("hasRole('ADMIN')") 保持一致） */
const adminPaths = ['/user', '/knowledge', '/document', '/role', '/permission', '/user-role', '/settings']

/** 全局前置守卫 - 登录鉴权 + 静默续期 + 角色权限控制 */
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 已登录不允许进入登录页
  if (to.path === '/login') {
    userStore.isLoggedIn ? next('/') : next()
    return
  }

  // 页面刷新后内存无 accessToken，但存在 refreshToken → 静默续期（B1 入口）
  if (!userStore.accessToken && userStore.refreshToken) {
    await refreshSession()
  }

  // 未登录跳转登录页
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 角色权限控制：非超级管理员访问管理页面时跳回首页
  if (adminPaths.includes(to.path) && !userStore.roles.includes('ROLE_ADMIN')) {
    next('/dashboard')
    return
  }

  next()
})

export default router
