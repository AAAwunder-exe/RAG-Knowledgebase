<template>
  <div class="page-container">
    <div class="user-role-layout">
      <!-- 左侧用户列表 -->
      <div class="user-panel">
        <div class="panel-header">
          <span class="panel-title">用户列表</span>
        </div>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名或姓名"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <div class="user-list">
          <div
            v-for="user in filteredUsers"
            :key="user.id"
            class="user-item"
            :class="{ active: selectedUserId === user.id }"
            @click="selectUser(user.id)"
          >
            <el-avatar :size="36" class="user-avatar">
              {{ user.realName.charAt(0) }}
            </el-avatar>
            <div class="user-info">
              <span class="user-name">{{ user.realName }}</span>
              <span class="user-account">{{ user.username }}</span>
            </div>
            <el-tag v-if="user.status === 0" size="small" type="danger" effect="light">禁用</el-tag>
          </div>
        </div>
      </div>

      <!-- 右侧角色绑定 -->
      <div class="role-panel">
        <div class="panel-header">
          <span class="panel-title">
            角色分配 - {{ selectedUser?.realName || '请选择用户' }}
          </span>
        </div>

        <div v-if="selectedUser" class="role-content">
          <el-checkbox-group v-model="selectedRoleIds" class="role-group">
            <div
              v-for="role in allRoles"
              :key="role.id"
              class="role-item"
            >
              <el-checkbox :value="role.id">
                <div class="role-detail">
                  <span class="role-name">{{ role.roleName }}</span>
                  <span class="role-desc">{{ role.description }}</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>

          <div class="role-actions">
            <el-button type="primary" @click="handleSave">保存分配</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>

        <el-empty v-else description="请从左侧选择用户" class="empty-state" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { pageUsers } from '@/api/user'
import { listRoles } from '@/api/role'
import { getUserRoles, assignUserRoles } from '@/api/permission'
import type { UserVO, Role } from '@/types'

/** 用户列表数据 */
const users = ref<{ id: string; username: string; realName: string; status: number }[]>([])

/** 所有角色 */
const allRoles = ref<Role[]>([])

/** 搜索关键词 */
const searchKeyword = ref('')
/** 选中的用户 ID */
const selectedUserId = ref<string>()
/** 选中的角色 ID 列表 */
const selectedRoleIds = ref<string[]>([])

/** 过滤后的用户列表 */
const filteredUsers = computed(() => {
  if (!searchKeyword.value) return users.value
  const kw = searchKeyword.value.toLowerCase()
  return users.value.filter(
    (u) => u.username.toLowerCase().includes(kw) || (u.realName || '').includes(kw)
  )
})

/** 当前选中的用户 */
const selectedUser = computed(() => users.value.find((u) => u.id === selectedUserId.value))

/** 加载用户列表和角色 */
async function loadData() {
  const [userRes, roleRes] = await Promise.all([
    pageUsers({ current: 1, size: 200 }),
    listRoles(),
  ])
  users.value = (userRes.records as UserVO[]).map((u) => ({
    id: u.id,
    username: u.username,
    realName: u.realName,
    status: u.status,
  }))
  allRoles.value = roleRes
}

/** 选择用户时加载其角色 */
async function selectUser(userId: string) {
  selectedUserId.value = userId
  const roles = await getUserRoles(userId)
  selectedRoleIds.value = roles.map((r) => r.id)
}

/** 保存角色分配 */
async function handleSave() {
  if (!selectedUserId.value) return
  await assignUserRoles(selectedUserId.value, selectedRoleIds.value)
  ElMessage.success('角色分配成功')
}

/** 重置选择 */
function handleReset() {
  if (selectedUserId.value) selectUser(selectedUserId.value)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-role-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  height: calc(100vh - var(--header-height) - 40px);
}

/* 左侧用户面板 */
.user-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.search-input {
  margin: 12px 16px;
  width: calc(100% - 32px);
}

.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: var(--bg-hover);
}

.user-item.active {
  background: var(--primary-bg);
}

.user-avatar {
  background-color: var(--primary-color);
  color: #fff;
  font-size: 14px;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.user-account {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 右侧角色面板 */
.role-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.role-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.role-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-item {
  padding: 12px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  transition: border-color 0.2s;
}

.role-item:hover {
  border-color: var(--primary-light);
}

.role-detail {
  display: inline-flex;
  flex-direction: column;
}

.role-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.role-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.role-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .user-role-layout {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>
