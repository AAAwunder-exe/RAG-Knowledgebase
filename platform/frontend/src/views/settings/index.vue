<template>
  <div class="page-container">
    <!-- 基础配置（唯一真实生效的配置项：系统名称/描述/备案信息） -->
    <div v-loading="loading" class="settings-card">
      <el-form :model="basicConfig" label-width="140px" class="settings-form">
        <el-form-item label="系统名称">
          <el-input v-model="basicConfig.systemName" style="max-width: 400px" />
        </el-form-item>
        <el-form-item label="系统描述">
          <el-input
            v-model="basicConfig.description"
            type="textarea"
            :rows="3"
            style="max-width: 400px"
          />
        </el-form-item>
        <el-form-item label="备案信息">
          <el-input v-model="basicConfig.icp" placeholder="如：京ICP备xxxxxxxx号" style="max-width: 400px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, saveSystemConfig } from '@/api/system'

const loading = ref(true)
const saving = ref(false)

/** 基础配置 */
const basicConfig = reactive({
  systemName: '',
  description: '',
  icp: '',
})

/** 将后端配置加载到表单 */
function applyConfig(cfg: Record<string, string>) {
  basicConfig.systemName = cfg['system.name'] ?? ''
  basicConfig.description = cfg['system.description'] ?? ''
  basicConfig.icp = cfg['system.icp'] ?? ''
}

/** 将表单序列化为后端配置 Map */
function buildConfig(): Record<string, string> {
  return {
    'system.name': basicConfig.systemName,
    'system.description': basicConfig.description,
    'system.icp': basicConfig.icp,
  }
}

/** 保存配置 */
async function handleSave() {
  if (saving.value) return
  saving.value = true
  try {
    await saveSystemConfig(buildConfig())
    ElMessage.success('保存成功')
    // 同步系统名称到浏览器标题
    if (basicConfig.systemName) {
      document.title = basicConfig.systemName
    }
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  // 数据加载完成前由 v-loading 遮罩覆盖，避免刷新时闪现默认文案
  try {
    const cfg = await getSystemConfig()
    applyConfig(cfg)
    if (basicConfig.systemName) {
      document.title = basicConfig.systemName
    }
  } catch {
    // 加载失败时保留空值，避免闪现不存在的配置
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.settings-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 20px;
  box-shadow: var(--shadow-sm);
  min-height: calc(100vh - var(--header-height) - 40px);
}

.settings-form {
  max-width: 600px;
}
</style>
