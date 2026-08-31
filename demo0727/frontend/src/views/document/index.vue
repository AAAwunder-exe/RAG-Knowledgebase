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
        <el-button type="primary" plain @click="handleOpenUpload">
          <el-icon><Upload /></el-icon>上传文档
        </el-button>
      </template>
    </SearchForm>

    <!-- 未选择知识库时的提示 -->
    <el-alert
      v-if="!queryData.knowledgeId"
      title="请先在上方选择所属知识库，再查看/上传文档"
      type="info"
      :closable="false"
      class="kb-tip"
    />

    <!-- 数据表格 -->
    <BaseTable
      :data="tableData"
      :columns="columns"
      :loading="loading"
      action-width="90"
    >
      <template #size="{ row }">
        {{ formatSize(row.size) }}
      </template>
      <template #actions="{ row }">
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

    <!-- 上传文档弹窗 -->
    <el-dialog
      v-model="uploadVisible"
      title="上传文档"
      width="560px"
      :close-on-click-modal="false"
      @close="handleUploadClose"
    >
      <el-form :model="uploadForm" label-width="100px" ref="uploadFormRef">
        <el-form-item label="所属知识库" required>
          <el-select
            v-model="uploadForm.knowledgeId"
            placeholder="请选择知识库"
            style="width: 100%"
          >
            <el-option
              v-for="kb in kbOptions"
              :key="kb.value"
              :label="kb.label"
              :value="kb.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-exceed="handleFileExceed"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 pdf / doc / docx / md / markdown / txt，最大 50MB</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="标题">
          <el-input v-model="uploadForm.title" placeholder="留空则使用文件名" clearable />
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="uploadForm.tags" placeholder="多个标签用逗号分隔，可选" clearable />
        </el-form-item>

        <el-form-item label="摘要">
          <el-input v-model="uploadForm.summary" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import SearchForm, { type SearchField } from '@/components/SearchForm.vue'
import BaseTable, { type TableColumn } from '@/components/BaseTable.vue'
import Pagination from '@/components/Pagination.vue'
import { pageDocuments, uploadDocument, deleteDocument } from '@/api/document'
import { pageKnowledgeBases } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()

const userStore = useUserStore()

/** 操作前校验权限：无权限时弹提示并阻止 */
function requirePermission(code: string): boolean {
  if (!userStore.hasPermission(code)) {
    ElMessage.warning('权限不足：暂无该操作权限，请联系管理员分配')
    return false
  }
  return true
}

/** 查询条件 */
const queryData = reactive({
  knowledgeId: '',
  keyword: '',
  current: 1,
  size: 10,
})

/** 知识库选项（供搜索下拉 + 上传弹窗共用） */
const kbOptions = ref<{ label: string; value: string }[]>([])

/** 搜索表单字段（知识库选项异步加载，需响应式） */
const searchFields = computed<SearchField[]>(() => [
  {
    prop: 'knowledgeId',
    label: '所属知识库',
    type: 'select',
    options: kbOptions.value,
  },
  { prop: 'keyword', label: '文档关键词', type: 'input' },
])

/** 表格配置 */
const columns: TableColumn[] = [
  { prop: 'originalName', label: '文件名', minWidth: 240 },
  { prop: 'title', label: '标题', minWidth: 140 },
  { prop: 'type', label: '类型', width: 90, align: 'center' },
  { prop: 'size', label: '大小', slot: 'size', width: 110, align: 'center' },
  { prop: 'creatorName', label: '上传人', minWidth: 110 },
  { prop: 'createTime', label: '上传时间', type: 'date', width: 170 },
]

/** 表格数据 */
const tableData = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

/** 查询数据（knowledgeId 必填，未选择则不请求） */
async function fetchData() {
  if (!queryData.knowledgeId) {
    tableData.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: queryData.current || 1,
      size: queryData.size || 10,
      knowledgeId: queryData.knowledgeId,
    }
    if (queryData.keyword) params.keyword = queryData.keyword
    const res = await pageDocuments(params)
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

/** 同步知识库选择到 URL，刷新后自动恢复 */
function syncKbToUrl() {
  const kbId = queryData.knowledgeId || ''
  const cur = (route.query.knowledgeId as string) || ''
  if (cur !== kbId) {
    router.replace({ query: kbId ? { knowledgeId: kbId } : {} })
  }
}

/** 知识库选择变化：更新 URL 并自动查询（选即查）；清空则清空列表 */
watch(
  () => queryData.knowledgeId,
  (newVal, oldVal) => {
    if (newVal === oldVal) return
    if (newVal) {
      queryData.current = 1
      syncKbToUrl()
      fetchData()
    } else {
      tableData.value = []
      total.value = 0
      syncKbToUrl()
    }
  }
)

/** 加载知识库选项 */
async function loadKbOptions() {
  try {
    const res = await pageKnowledgeBases({ current: 1, size: 100 })
    kbOptions.value = res.records.map((kb) => ({ label: kb.name, value: kb.id }))
  } catch {
    // 错误已由拦截器统一提示
  }
}

/** 字节大小格式化 */
function formatSize(bytes: number) {
  if (bytes === null || bytes === undefined) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

/** 删除文档 */
async function handleDelete(row: any) {
  if (!requirePermission('api:document:delete')) return
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${row.originalName}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 取消
  }
}

// ========== 上传逻辑 ==========

const uploadVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref<UploadInstance>()
const uploadFormRef = ref()
const uploadForm = reactive<Record<string, any>>({
  knowledgeId: '',
  file: null,
  title: '',
  tags: '',
  summary: '',
})

/** 打开上传弹窗：未选知识库则打开后仍需选择；已选则带入当前知识库 */
function handleOpenUpload() {
  if (!requirePermission('api:document:upload')) return
  uploadForm.knowledgeId = queryData.knowledgeId || ''
  uploadForm.file = null
  uploadForm.title = ''
  uploadForm.tags = ''
  uploadForm.summary = ''
  uploadVisible.value = true
}

/** 选择文件 */
function handleFileChange(uploadFile: UploadFile) {
  uploadForm.file = uploadFile.raw
}

/** 移除文件 */
function handleFileRemove() {
  uploadForm.file = null
}

/** 超出数量限制（limit=1） */
function handleFileExceed() {
  uploadRef.value?.clearFiles()
  uploadForm.file = null
  ElMessage.warning('只能上传一个文件，请先移除后再选择')
}

/** 上传 */
async function handleUpload() {
  if (!uploadForm.knowledgeId) {
    ElMessage.warning('请选择所属知识库')
    return
  }
  if (!uploadForm.file) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  const formData = new FormData()
  formData.append('file', uploadForm.file)
  formData.append('knowledgeId', uploadForm.knowledgeId)
  if (uploadForm.title) formData.append('title', uploadForm.title)
  if (uploadForm.tags) formData.append('tags', uploadForm.tags)
  if (uploadForm.summary) formData.append('summary', uploadForm.summary)

  uploading.value = true
  try {
    await uploadDocument(formData)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    uploadRef.value?.clearFiles()
    // 若当前未在查看该知识库，切换过去
    if (!queryData.knowledgeId || queryData.knowledgeId !== uploadForm.knowledgeId) {
      queryData.knowledgeId = uploadForm.knowledgeId
      queryData.current = 1
    }
    fetchData()
  } finally {
    uploading.value = false
  }
}

/** 关闭上传弹窗：清空文件列表与校验状态 */
function handleUploadClose() {
  uploadRef.value?.clearFiles()
  uploadFormRef.value?.clearValidate()
}

// 初始加载：先拉知识库选项，再从 URL 恢复上次选择（赋值会触发 watch 自动查询）
onMounted(async () => {
  await loadKbOptions()
  const kbId = (route.query.knowledgeId as string) || ''
  if (kbId) {
    queryData.knowledgeId = kbId
    queryData.current = 1
  }
})
</script>

<style scoped>
.kb-tip {
  margin-bottom: 16px;
}
</style>
