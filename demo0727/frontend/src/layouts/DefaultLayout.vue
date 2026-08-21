<template>
  <div class="layout-container">
    <!-- 左侧菜单栏 -->
    <Sidebar :collapsed="appStore.sidebarCollapsed" />

    <!-- 右侧主区域 -->
    <div class="layout-main" :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }">
      <!-- 顶部导航栏 -->
      <Header />

      <!-- 内容区域 -->
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import Sidebar from '@/components/Sidebar.vue'
import Header from '@/components/Header.vue'

const appStore = useAppStore()
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.28s ease;
  overflow: hidden;
}

.layout-main.sidebar-collapsed {
  margin-left: var(--sidebar-collapsed-width);
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  background-color: var(--bg-page);
}
</style>
