<template>
  <div class="search-form">
    <el-form :inline="true" :model="queryData" class="form-inline">
      <el-form-item
        v-for="field in fields"
        :key="field.prop"
        :label="field.label"
      >
        <!-- 输入框 -->
        <el-input
          v-if="field.type === 'input'"
          v-model="queryData[field.prop]"
          :placeholder="field.placeholder || `请输入${field.label}`"
          clearable
          style="width: 200px"
          @keyup.enter="$emit('search')"
        />
        <!-- 下拉选择 -->
        <el-select
          v-else-if="field.type === 'select'"
          v-model="queryData[field.prop]"
          :placeholder="field.placeholder || `请选择${field.label}`"
          clearable
          style="width: 160px"
        >
          <el-option
            v-for="opt in field.options"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <!-- 日期范围 -->
        <el-date-picker
          v-else-if="field.type === 'daterange'"
          v-model="queryData[field.prop]"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item class="form-actions">
        <el-button type="primary" @click="$emit('search')">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>重置
        </el-button>
        <slot name="extra" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

export interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'daterange'
  placeholder?: string
  options?: { label: string; value: any }[]
}

const props = defineProps<{
  fields: SearchField[]
  modelValue: Record<string, any>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
  search: []
  reset: []
}>()

// 内部查询数据副本
const queryData = reactive({ ...props.modelValue })

watch(queryData, (val) => {
  emit('update:modelValue', { ...val })
})

// 外部 modelValue 变化（如路由恢复、父组件主动赋值）时同步到内部查询条件，
// 保证下拉/输入框显示与父组件状态一致
watch(
  () => props.modelValue,
  (val) => {
    if (!val) return
    Object.keys(val).forEach((key) => {
      if (queryData[key] !== val[key]) queryData[key] = val[key]
    })
  },
  { deep: true }
)

/** 重置查询条件（仅清筛选字段，保留 current/size 等分页参数） */
function handleReset() {
  props.fields.forEach((field) => {
    queryData[field.prop] = ''
  })
  emit('reset')
}
</script>

<style scoped>
.search-form {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 18px 20px 0 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}

.form-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.form-actions {
  margin-left: auto;
}
</style>
