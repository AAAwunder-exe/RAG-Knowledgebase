<template>
  <div class="base-table">
    <!-- 表格上方工具栏（插槽） -->
    <div v-if="$slots.toolbar" class="table-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 表格主体 -->
    <el-table
      :data="data"
      v-loading="loading"
      :border="border"
      :stripe="stripe"
      style="width: 100%"
      :row-key="rowKey"
      @selection-change="$emit('selection-change', $event)"
    >
      <!-- 多选列 -->
      <el-table-column v-if="selection" type="selection" width="48" />

      <!-- 序号列 -->
      <el-table-column v-if="showIndex" type="index" label="序号" width="64" align="center" />

      <!-- 动态列 -->
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth || 120"
        :align="col.align || 'left'"
        :fixed="col.fixed"
        :show-overflow-tooltip="col.showOverflowTooltip !== false"
      >
        <template #default="{ row, $index }">
          <!-- 自定义插槽渲染 -->
          <slot v-if="col.slot" :name="col.slot" :row="row" :index="$index" />
          <!-- 状态标签 -->
          <el-tag
            v-else-if="col.type === 'status'"
            :type="row[col.prop] === 1 ? 'success' : 'danger'"
            :effect="'light'"
            size="small"
          >
            {{ row[col.prop] === 1 ? '启用' : '禁用' }}
          </el-tag>
          <!-- 日期格式化 -->
          <span v-else-if="col.type === 'date'">{{ formatDate(row[col.prop]) }}</span>
          <!-- 标签组 -->
          <template v-else-if="col.type === 'tags'">
            <el-tag
              v-for="tag in row[col.prop]"
              :key="tag"
              size="small"
              class="mr-4"
            >{{ tag }}</el-tag>
          </template>
          <!-- 默认文本 -->
          <span v-else>{{ row[col.prop] ?? '-' }}</span>
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column
        v-if="$slots.actions"
        label="操作"
        :width="actionWidth"
        fixed="right"
        align="center"
      >
        <template #default="{ row, $index }">
          <slot name="actions" :row="row" :index="$index" />
        </template>
      </el-table-column>

      <!-- 空数据 -->
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { formatDateUtil } from '@/utils/format'

export interface TableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
  type?: 'status' | 'date' | 'tags'
  slot?: string
  showOverflowTooltip?: boolean
}

withDefaults(
  defineProps<{
    data: any[]
    columns: TableColumn[]
    loading?: boolean
    selection?: boolean
    showIndex?: boolean
    border?: boolean
    stripe?: boolean
    rowKey?: string
    actionWidth?: number | string
  }>(),
  {
    loading: false,
    selection: false,
    showIndex: true,
    border: false,
    stripe: true,
    rowKey: 'id',
    actionWidth: 180,
  }
)

defineEmits<{
  'selection-change': [selection: any[]]
}>()

function formatDate(val: string) {
  return formatDateUtil(val)
}
</script>

<style scoped>
.base-table {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 4px 0;
  box-shadow: var(--shadow-sm);
}

.table-toolbar {
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mr-4 {
  margin-right: 4px;
}

:deep(.el-table) {
  --el-table-border-color: var(--border-light);
}

:deep(.el-table th.el-table__cell) {
  background-color: var(--bg-hover);
  color: var(--text-regular);
  font-weight: 500;
}
</style>
