<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      class="dialog-form"
    >
      <el-form-item
        v-for="field in fields"
        :key="field.prop"
        :label="field.label"
        :prop="field.prop"
      >
        <!-- 输入框 -->
        <el-input
          v-if="field.type === 'input' || field.type === 'password'"
          v-model="formData[field.prop]"
          :type="field.type === 'password' ? 'password' : 'text'"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="field.disabled"
          show-password
          clearable
        />
        <!-- 文本域 -->
        <el-input
          v-else-if="field.type === 'textarea'"
          v-model="formData[field.prop]"
          type="textarea"
          :rows="field.rows || 3"
          :placeholder="field.placeholder || `请输入${field.label}`"
        />
        <!-- 下拉选择 -->
        <el-select
          v-else-if="field.type === 'select'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder || `请选择${field.label}`"
          style="width: 100%"
          clearable
        >
          <el-option
            v-for="opt in field.options"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <!-- 数字输入 -->
        <el-input-number
          v-else-if="field.type === 'number'"
          v-model="formData[field.prop]"
          :min="field.min"
          :max="field.max"
          style="width: 100%"
        />
        <!-- 开关 -->
        <el-switch
          v-else-if="field.type === 'switch'"
          v-model="formData[field.prop]"
          :active-value="1"
          :inactive-value="0"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import type { FormInstance, FormItemRule } from 'element-plus'

/** 表单字段配置 */
export interface FormField {
  prop: string
  label: string
  type: 'input' | 'password' | 'textarea' | 'select' | 'number' | 'switch'
  placeholder?: string
  disabled?: boolean
  options?: { label: string; value: any }[]
  min?: number
  max?: number
  rows?: number
}

const props = defineProps<{
  modelValue: boolean
  title: string
  fields: FormField[]
  rules?: Record<string, FormItemRule[]>
  formData: Record<string, any>
  width?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: []
}>()

const visible = ref(props.modelValue)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 同步外部控制
watch(
  () => props.modelValue,
  (val) => { visible.value = val }
)
watch(visible, (val) => emit('update:modelValue', val))

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitting.value = true
    emit('submit')
  } catch {
    // 校验不通过
  } finally {
    submitting.value = false
  }
}

/** 关闭弹窗时重置表单校验 */
function handleClose() {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.dialog-form {
  padding: 10px 20px 0 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
