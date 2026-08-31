<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <p class="error-desc">页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
// 404 页面
</script>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page);
}

.error-content {
  text-align: center;
}

.error-code {
  font-size: 72px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}
</style>
