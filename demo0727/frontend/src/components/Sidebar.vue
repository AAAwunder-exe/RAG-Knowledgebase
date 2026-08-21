<template>
  <aside class="sidebar" :class="{ collapsed }">
    <!-- Logo 区域 -->
    <div class="sidebar-logo">
      <el-icon :size="28" color="var(--primary-color)"><Cpu /></el-icon>
      <span v-show="!collapsed" class="logo-text">{{ systemName }}</span>
    </div>

    <!-- 菜单 -->
    <el-scrollbar class="sidebar-menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <template v-for="item in menuItems" :key="item.path">
          <!-- 含子菜单的分组 -->
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 单个菜单项 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSystemConfig } from '@/api/system'
import { useUserStore } from '@/stores/user'

defineProps<{ collapsed: boolean }>()

const route = useRoute()

/** 系统名称（默认值，配置加载后覆盖） */
const systemName = ref('AI 知识平台')

/** 当前激活的菜单 */
const activeMenu = computed(() => route.path)

/** 加载系统名称并同步浏览器标题 */
onMounted(async () => {
  try {
    const cfg = await getSystemConfig()
    if (cfg['system.name']) {
      systemName.value = cfg['system.name']
      document.title = cfg['system.name']
    }
  } catch {
    // 加载失败时保留默认值
  }
})

/** 菜单配置 - 与路由表对应；adminOnly 标记仅超级管理员可见 */
interface MenuItem {
  path: string
  title: string
  icon: string
  adminOnly?: boolean
  children?: MenuItem[]
}

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('ROLE_ADMIN'))

const allMenuItems: MenuItem[] = [
  { path: '/dashboard', title: '首页', icon: 'Odometer' },
  { path: '/ai', title: 'AI 问答', icon: 'ChatDotRound' },
  { path: '/user', title: '用户管理', icon: 'User', adminOnly: true },
  { path: '/knowledge', title: '知识库管理', icon: 'FolderOpened', adminOnly: true },
  { path: '/document', title: '文档管理', icon: 'Document', adminOnly: true },
  {
    path: '/permission-group',
    title: '权限管理',
    icon: 'Lock',
    adminOnly: true,
    children: [
      { path: '/role', title: '角色管理', icon: 'UserFilled' },
      { path: '/permission', title: '菜单权限', icon: 'Lock' },
      { path: '/user-role', title: '用户角色绑定', icon: 'Connection' },
    ],
  },
  { path: '/settings', title: '系统设置', icon: 'Setting', adminOnly: true },
]

/** 按角色过滤菜单：普通用户仅保留首页、AI 问答 */
const menuItems = computed<MenuItem[]>(() => {
  if (isAdmin.value) return allMenuItems
  return allMenuItems.filter((item) => !item.adminOnly)
})
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: width 0.28s ease;
  z-index: 1000;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar-logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.sidebar-menu-scroll {
  flex: 1;
  overflow: hidden;
}

.sidebar-menu {
  border-right: none;
  padding: 8px 0;
}

/* 菜单项样式微调 */
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: var(--radius-md);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: var(--primary-bg);
  color: var(--primary-color);
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: var(--bg-hover);
}
</style>
