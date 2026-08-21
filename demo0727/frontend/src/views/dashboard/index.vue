<template>
  <div class="page-container">
    <!-- 欢迎语 -->
    <div class="welcome-section">
      <h2 class="welcome-title">
        {{ greeting }}，{{ userStore.userInfo?.realName || '用户' }}
      </h2>
      <p class="welcome-desc">欢迎使用企业 AI 知识管理平台</p>
    </div>

    <!-- 数据统计卡片 -->
    <div class="stats-grid">
      <div
        v-for="item in statCards"
        :key="item.label"
        class="stat-card"
      >
        <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ item.value }}</span>
          <span class="stat-label">{{ item.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getDashboardStats, type DashboardStats } from '@/api/dashboard'

const userStore = useUserStore()

/** 根据当前时间返回问候语 */
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

/** 统计数据 */
const stats = ref<DashboardStats>({ userCount: 0, documentCount: 0, knowledgeBaseCount: 0 })

/** 统计卡片数据 */
const statCards = computed(() => [
  { label: '用户总数', value: stats.value.userCount, icon: 'User', color: '#4a7afe', bg: '#eef3ff' },
  { label: '文档总数', value: stats.value.documentCount, icon: 'Document', color: '#00b42a', bg: '#e8ffea' },
  { label: '知识库数', value: stats.value.knowledgeBaseCount, icon: 'FolderOpened', color: '#ff9a2e', bg: '#fff7e8' },
])

onMounted(async () => {
  try {
    stats.value = await getDashboardStats()
  } catch {
    // 错误已由 request 拦截器处理
  }
})
</script>

<style scoped>
.welcome-section {
  margin-bottom: 20px;
}

.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.welcome-desc {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
