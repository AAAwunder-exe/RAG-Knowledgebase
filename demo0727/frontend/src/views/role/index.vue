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
          <el-icon><Plus /></el-icon>新增角色
        </el-button>
      </template>
    </SearchForm>

    <!-- 表格 -->
    <BaseTable
      :data="tableData"
      :columns="columns"
      :loading="loading"
      action-width="240"
    >
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button link type="primary" size="small" @click="handlePermission(row)">
          分配权限
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

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="450px">
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        :props="{ label: 'permissionName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedKeys"
        class="perm-tree"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormItemRule } from 'element-plus'
import SearchForm from '@/components/SearchForm.vue'
import BaseTable, { type TableColumn } from '@/components/BaseTable.vue'
import Pagination from '@/components/Pagination.vue'
import DialogForm, { type FormField } from '@/components/DialogForm.vue'
import { pageRoles, createRole, updateRole, deleteRole, getRolePermissions, assignRolePermissions } from '@/api/role'
import { listPermissions } from '@/api/permission'
import type { Role, Permission } from '@/types'

const queryData = reactive({
  roleName: '',
  current: 1,
  size: 10,
})

const searchFields = [
  { prop: 'roleName', label: '角色名称', type: 'input' as const },
]

const columns: TableColumn[] = [
  { prop: 'roleName', label: '角色名称', minWidth: 140 },
  { prop: 'roleCode', label: '角色编码', minWidth: 140 },
  { prop: 'description', label: '描述', minWidth: 200 },
  { prop: 'sort', label: '排序', width: 80, align: 'center' },
  { prop: 'status', label: '状态', type: 'status', width: 80, align: 'center' },
  { prop: 'createTime', label: '创建时间', type: 'date', width: 170 },
]

const tableData = ref<Role[]>([])
const total = ref(0)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await pageRoles(queryData)
    tableData.value = res.records as Role[]
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryData.current = 1
  fetchData()
}

// ========== 弹窗 ==========
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const dialogFormData = reactive<Record<string, any>>({
  roleName: '', roleCode: '', description: '', sort: 1, status: 1,
})

const dialogFields: FormField[] = [
  { prop: 'roleName', label: '角色名称', type: 'input' },
  { prop: 'roleCode', label: '角色编码', type: 'input' },
  { prop: 'description', label: '描述', type: 'textarea' },
  { prop: 'sort', label: '排序', type: 'number', min: 1 },
  {
    prop: 'status', label: '状态', type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 },
    ],
  },
]

const dialogRules: Record<string, FormItemRule[]> = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

function handleAdd() {
  dialogTitle.value = '新增角色'
  Object.assign(dialogFormData, { id: undefined, roleName: '', roleCode: '', description: '', sort: 1, status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: Role) {
  dialogTitle.value = '编辑角色'
  Object.assign(dialogFormData, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  const data = {
    roleName: dialogFormData.roleName,
    roleCode: dialogFormData.roleCode,
    description: dialogFormData.description,
    sort: dialogFormData.sort,
    status: dialogFormData.status,
  }
  if (dialogFormData.id) {
    await updateRole(dialogFormData.id, data)
    ElMessage.success('编辑成功')
  } else {
    await createRole(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row: Role) {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${row.roleName}」吗？`, '删除确认', {
      confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error',
    })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* 取消 */ }
}

// ========== 分配权限 ==========
const permDialogVisible = ref(false)
const checkedKeys = ref<string[]>([])
const currentRoleId = ref<string>()
const permissionTree = ref<any[]>([])

/** 加载权限树（懒加载，首次打开弹窗前拉取） */
async function loadPermissionTree() {
  if (permissionTree.value.length > 0) return
  const perms: Permission[] = await listPermissions()
  // 顶层 parentId 为 null 或 0，用 !p.parentId 过滤
  permissionTree.value = perms
    .filter((p) => !p.parentId)
    .map((p) => ({
      ...p,
      children: perms.filter((c) => c.parentId === p.id),
    }))
}

async function handlePermission(row: Role) {
  currentRoleId.value = row.id
  await loadPermissionTree()
  // 回填已分配权限
  checkedKeys.value = await getRolePermissions(row.id)
  permDialogVisible.value = true
}

async function handlePermSubmit() {
  await assignRolePermissions(currentRoleId.value!, checkedKeys.value)
  ElMessage.success('权限分配成功')
  permDialogVisible.value = false
}

fetchData()
</script>

<style scoped>
.perm-tree {
  max-height: 400px;
  overflow-y: auto;
}
</style>
