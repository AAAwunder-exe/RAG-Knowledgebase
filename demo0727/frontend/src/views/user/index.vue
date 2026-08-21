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
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
      </template>
    </SearchForm>

    <!-- 数据表格 -->
    <BaseTable
      :data="tableData"
      :columns="columns"
      :loading="loading"
      action-width="200"
    >
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button
          link
          :type="row.status === 1 ? 'warning' : 'success'"
          size="small"
          @click="handleToggleStatus(row)"
        >
          {{ row.status === 1 ? '禁用' : '启用' }}
        </el-button>
        <el-button link type="danger" size="small" @click="handleDelete(row)">
          删除
        </el-button>
      </template>
    </BaseTable>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:current="queryData.current"
      v-model:size="queryData.size"
      @change="fetchData"
    />

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
import Pagination from '@/components/Pagination.vue'
import DialogForm, { type FormField } from '@/components/DialogForm.vue'
import { pageUsers, createUser, updateUser, updateUserStatus, deleteUser } from '@/api/user'

/** 查询条件 */
const queryData = reactive({
  username: '',
  realName: '',
  status: '',
  current: 1,
  size: 10,
})

const searchFields = [
  { prop: 'username', label: '用户名', type: 'input' as const },
  { prop: 'realName', label: '姓名', type: 'input' as const },
  {
    prop: 'status', label: '状态', type: 'select' as const,
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 },
    ],
  },
]

/** 表格配置 */
const columns: TableColumn[] = [
  { prop: 'username', label: '用户名', minWidth: 120 },
  { prop: 'realName', label: '真实姓名', minWidth: 120 },
  { prop: 'email', label: '邮箱', minWidth: 200 },
  { prop: 'phone', label: '手机号', minWidth: 130 },
  { prop: 'roles', label: '角色', type: 'tags', minWidth: 120 },
  { prop: 'status', label: '状态', type: 'status', width: 80, align: 'center' },
  { prop: 'createTime', label: '创建时间', type: 'date', width: 170 },
]

/** 表格数据 */
const tableData = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

/** 查询数据 */
async function fetchData() {
  loading.value = true
  try {
    const res = await pageUsers(queryData)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  queryData.current = 1
  fetchData()
}

// ========== 弹窗逻辑 ==========

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const dialogFormData = reactive<Record<string, any>>({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  status: 1,
})

/** 弹窗字段：新增时显示密码，编辑时不显示 */
const dialogFields = computed<FormField[]>(() => {
  const base: FormField[] = [
    { prop: 'username', label: '用户名', type: 'input' },
    ...(dialogFormData.id ? [] : [{ prop: 'password', label: '密码', type: 'password' as const }]),
    { prop: 'realName', label: '真实姓名', type: 'input' },
    { prop: 'email', label: '邮箱', type: 'input' },
    { prop: 'phone', label: '手机号', type: 'input' },
    {
      prop: 'status', label: '状态', type: 'select',
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
    },
  ]
  return base
})

const dialogRules: Record<string, FormItemRule[]> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在 6-20 个字符之间', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

/** 新增 */
function handleAdd() {
  dialogTitle.value = '新增用户'
  Object.assign(dialogFormData, {
    id: undefined, username: '', password: '', realName: '', email: '', phone: '', status: 1,
  })
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row: any) {
  dialogTitle.value = '编辑用户'
  Object.assign(dialogFormData, { ...row, password: '' })
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  const data = {
    username: dialogFormData.username,
    password: dialogFormData.password,
    realName: dialogFormData.realName,
    email: dialogFormData.email,
    phone: dialogFormData.phone,
    status: dialogFormData.status,
  }
  if (dialogFormData.id) {
    // 编辑：不传用户名和密码
    const { username, password, ...updateData } = data
    await updateUser(dialogFormData.id, updateData)
    ElMessage.success('编辑成功')
  } else {
    await createUser(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchData()
}

/** 切换状态 */
async function handleToggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定要${newStatus === 1 ? '启用' : '禁用'}用户「${row.username}」吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await updateUserStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success('操作成功')
  } catch {
    // 取消
  }
}

/** 删除 */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.username}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 取消
  }
}

// 初始加载
fetchData()
</script>
