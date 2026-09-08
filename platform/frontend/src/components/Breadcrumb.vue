<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item :to="{ path: '/' }">
      <el-icon><HomeFilled /></el-icon>
    </el-breadcrumb-item>
    <template v-for="item in breadcrumbItems" :key="item.path">
      <el-breadcrumb-item v-if="item.title">
        {{ item.title }}
      </el-breadcrumb-item>
    </template>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

/** 根据路由 meta 生成面包屑 */
const breadcrumbItems = computed(() => {
  const items: { title: string; path: string }[] = []
  const parent = route.meta.parent as string | undefined

  // 如果有父级分组，先显示父级
  if (parent) {
    items.push({ title: parent, path: '' })
  }

  // 当前页面标题
  if (route.meta.title) {
    items.push({ title: route.meta.title as string, path: route.path })
  }

  return items
})
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.breadcrumb :deep(.el-breadcrumb__item) {
  display: flex;
  align-items: center;
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--text-secondary);
  font-weight: 400;
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary);
  font-weight: 500;
}
</style>
