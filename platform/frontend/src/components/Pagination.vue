<template>
  <div class="pagination-wrapper">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="pageSizes"
      :background="background"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="$emit('change', { current: currentPage, size: pageSize })"
      @current-change="$emit('change', { current: currentPage, size: pageSize })"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    total: number
    current?: number
    size?: number
    pageSizes?: number[]
    background?: boolean
  }>(),
  {
    current: 1,
    size: 10,
    pageSizes: () => [10, 20, 50, 100],
    background: true,
  }
)

const emit = defineEmits<{
  change: [params: { current: number; size: number }]
}>()

const currentPage = ref(props.current)
const pageSize = ref(props.size)

watch(
  () => props.current,
  (val) => { currentPage.value = val }
)
watch(
  () => props.size,
  (val) => { pageSize.value = val }
)
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px 0;
}
</style>
