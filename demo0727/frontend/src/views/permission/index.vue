<template>
  <div class="page-container">
    <!-- 查询表单 -->
    <SearchForm
      :model-value="queryData"
      @update:model-value="(val) => Object.assign(queryData, val)"
      :fields="searchFields"
      @search="handleSearch"
      @reset="handleSearch"
    >
      <template #extra>
        <el-button type="primary" plain @click="handleAdd">
          <el-icon><Plus /></el-icon>新增权限
        </el-button>
      </template>
    </SearchForm>

    <!-- 表格 -->
    <BaseTable
      :data="tableData"
      :columns="columns"
      :loading="loading"
      action-width="160"
      row-key="id"
    >
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
        <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
      </template>
    </BaseTable>

    <!-- 新增/编辑弹窗 -->
    <DialogForm
      v-model="dialogVisible"
      :title="dialogTitle"
      :fields="dialogFields"
      :rules="dialogRules"
      :form-data="dialogFormData"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormItemRule } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import BaseTable, { type TableColumn } from '@/components/BaseTable.vue'
import DialogForm, { type FormField } from '@/components/DialogForm.vue'
import { listPermissions, createPermission, updatePermission, deletePermission } from '@/api/permission'
import type { Permission } from '@/types'

const queryData = reactive({
  permissionName: '',
})

const searchFields = [
  { prop: 'permissionName', label: '权限名称', type: 'input' as const },
]

const columns: TableColumn[] = [
  { prop: 'permissionName', label: '权限名称', minWidth: 160 },
  { prop: 'permissionCode', label: '权限编码', minWidth: 140 },
  { prop: 'permissionType', label: '类型', width: 100, align: 'center' },
  { prop: 'description', label: '描述', minWidth: 180 },
  { prop: 'sort', label: '排序', width: 80, align: 'center' },
  { prop: 'status', label: '状态', type: 'status', width: 80, align: 'center' },
]

const tableData = ref<Permission[]>([])
const loading = ref(false)

/** 类型标签映射 */
const typeLabels: Record<string, string> = {
  menu: '菜单',
  button: '按钮',
  api: '接口',
}

async function fetchData() {
  loading.value = true
  try {
    let list = await listPermissions()
    if (queryData.permissionName) {
      list = list.filter((item) => item.permissionName.includes(queryData.permissionName))
    }
    // 转换类型为中文显示
    tableData.value = list.map((item) => ({
      ...item,
      permissionType: typeLabels[item.permissionType] || item.permissionType,
    }))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

// ========== 弹窗 ==========
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限')
const dialogFormData = reactive<Record<string, any>>({
  permissionName: '', permissionCode: '', permissionType: 'menu', parentId: 0, sort: 1, description: '', status: 1,
})

const dialogFields: FormField[] = [
  { prop: 'permissionName', label: '权限名称', type: 'input' },
  { prop: 'permissionCode', label: '权限编码', type: 'input' },
  {
    prop: 'permissionType', label: '权限类型', type: 'select',
    options: [
      { label: '菜单', value: 'menu' },
      { label: '按钮', value: 'button' },
      { label: '接口', value: 'api' },
    ],
  },
  { prop: 'sort', label: '排序', type: 'number', min: 1 },
  { prop: 'description', label: '描述', type: 'textarea' },
  {
    prop: 'status', label: '状态', type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 },
    ],
  },
]

const dialogRules: Record<string, FormItemRule[]> = {
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
}

function handleAdd() {
  dialogTitle.value = '新增权限'
  Object.assign(dialogFormData, {
    id: undefined, permissionName: '', permissionCode: '', permissionType: 'menu', parentId: 0, sort: 1, description: '', status: 1,
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  dialogTitle.value = '编辑权限'
  // 反转类型为英文
  const englishType = Object.entries(typeLabels).find(([, v]) => v === row.permissionType)?.[0] || row.permissionType
  Object.assign(dialogFormData, { ...row, permissionType: englishType })
  dialogVisible.value = true
}

async function handleSubmit() {
  const data = {
    permissionName: dialogFormData.permissionName,
    permissionCode: dialogFormData.permissionCode,
    permissionType: dialogFormData.permissionType,
    parentId: dialogFormData.parentId,
    sort: dialogFormData.sort,
    description: dialogFormData.description,
    status: dialogFormData.status,
  }
  if (dialogFormData.id) {
    await updatePermission(dialogFormData.id, data)
    ElMessage.success('编辑成功')
  } else {
    await createPermission(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除权限「${row.permissionName}」吗？`, '删除确认', {
      confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error',
    })
    await deletePermission(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* 取消 */ }
}

fetchData()
</script>
