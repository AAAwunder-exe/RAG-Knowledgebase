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
        <template v-for="item in menuItems" :key="item.id">
          <!-- 含子菜单的分组 -->
          <el-sub-menu v-if="isGroup(item)" :index="item.path || item.code">
            <template #title>
              <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
              <span>{{ item.name }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.id"
              :index="child.path"
            >
              <el-icon><component :is="child.icon || 'Menu'" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 单个菜单项 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
            <span>{{ item.name }}</span>
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
import type { MenuNode } from '@/types'

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

const userStore = useUserStore()

/** 动态菜单树：由后端 /me/access 返回（基于角色绑定的 menu 权限） */
const menuItems = computed<MenuNode[]>(() => userStore.menus)

/** 判断节点是否为分组：含子菜单即可视为分组（父分组自身不需要可跳转路径） */
function isGroup(node: MenuNode) {
  return !!node.children?.length
}
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
