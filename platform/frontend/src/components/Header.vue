<template>
  <header class="layout-header">
    <!-- 左侧：折叠按钮 + 面包屑 -->
    <div class="header-left">
      <el-icon class="collapse-btn" :size="20" @click="appStore.toggleSidebar">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
      <Breadcrumb />
    </div>

    <!-- 右侧：用户信息 -->
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" class="user-avatar">
            {{ avatarText }}
          </el-avatar>
          <span class="user-name">{{ userStore.userInfo?.realName || '用户' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'
import Breadcrumb from './Breadcrumb.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()

/** 头像文字 */
const avatarText = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

/** 下拉菜单命令处理 */
async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      // 先通知后端删除 Redis 中的 refreshToken，再清理本地状态
      try {
        await logoutApi()
      } catch {
        // 后端登出失败（如 token 已过期）不阻塞本地登出
      }
      userStore.logout()
      router.push('/login')
    } catch {
      // 用户取消
    }
  }
}
</script>

<style scoped>
.layout-header {
  height: var(--header-height);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--text-regular);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: var(--primary-color);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--bg-hover);
}

.user-avatar {
  background-color: var(--primary-color);
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
}

/* 响应式：小屏幕隐藏用户名 */
@media (max-width: 768px) {
  .user-name {
    display: none;
  }
}
</style>
