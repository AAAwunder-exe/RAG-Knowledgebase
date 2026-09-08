<template>
  <div class="ai-container">
    <!-- 顶部标题栏 -->
    <div class="ai-header">
      <h2>AI 智能问答</h2>
      <span class="ai-subtitle">基于教材知识库的 RAG 检索增强问答</span>
    </div>

    <!-- 对话区域 -->
    <div class="chat-area" ref="chatArea">
      <div v-if="messages.length === 0" class="empty-hint">
        <el-icon :size="48"><ChatDotRound /></el-icon>
        <p>输入你的学科问题，AI 将基于教材知识库为你解答</p>
        <div class="quick-questions">
          <el-button
            v-for="q in quickQuestions"
            :key="q"
            size="small"
            round
            @click="sendQuickQuestion(q)"
          >{{ q }}</el-button>
        </div>
      </div>

      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['chat-msg', msg.role]"
      >
        <div class="msg-avatar">
          <el-icon v-if="msg.role === 'user'" :size="20"><User /></el-icon>
          <el-icon v-else :size="20"><ChatDotRound /></el-icon>
        </div>
        <div class="msg-body">
          <div class="msg-content">
            <template v-if="msg.role === 'user'">{{ msg.content }}</template>
            <template v-else>
              <div class="answer-text" v-html="formatAnswer(msg.content)"></div>
              <el-collapse v-if="msg.references && msg.references.length > 0" class="ref-collapse">
                <el-collapse-item :title="`参考教材片段（${msg.references.length}）`">
                  <div
                    v-for="(ref, i) in msg.references"
                    :key="i"
                    class="ref-item"
                  >
                    <span class="ref-index">片段 {{ i + 1 }}</span>
                    <pre class="ref-snippet">{{ ref.snippet }}</pre>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </template>
          </div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="chat-msg assistant">
        <div class="msg-avatar">
          <el-icon :size="20"><ChatDotRound /></el-icon>
        </div>
        <div class="msg-body">
          <div class="msg-content loading-content">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在检索教材并思考...</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入你的学科问题，按 Enter 发送，Shift+Enter 换行"
        resize="none"
        @keydown.enter.exact.prevent="send"
        :disabled="loading"
      />
      <el-button
        type="primary"
        :loading="loading"
        @click="send"
        :disabled="!inputText.trim()"
      >
        <el-icon v-if="!loading"><Promotion /></el-icon>
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { ChatDotRound, User, Promotion, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { askQuestion } from '@/api/ai'
import { useAiStore } from '@/stores/ai'
import type { AnswerVO, ReferenceDoc } from '@/types'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  references?: ReferenceDoc[]
}

const inputText = ref('')
const loading = ref(false)
// 对话记录存 Pinia store（本次登录内存保留，切换页面不丢失）
const aiStore = useAiStore()
const { messages } = storeToRefs(aiStore)
const chatArea = ref<HTMLElement>()

const quickQuestions = [
  '什么是死锁？',
  '解释进程和线程的区别',
  '什么是虚拟内存？',
  '简述页面置换算法',
]

function formatAnswer(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

async function scrollToEnd() {
  await nextTick()
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}

async function sendQuickQuestion(q: string) {
  inputText.value = q
  await send()
}

async function send() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  aiStore.addMessage({ role: 'user', content: question })
  inputText.value = ''
  loading.value = true
  await scrollToEnd()

  try {
    const res: AnswerVO = await askQuestion({
      question,
      // 传最近的多轮历史作为上下文（不含刚加入的这条提问）
      history: aiStore.getHistory(),
    })
    aiStore.addMessage({
      role: 'assistant',
      content: res.answer,
      references: res.references,
    })
  } catch (err: any) {
    aiStore.addMessage({
      role: 'assistant',
      content: `回答失败：${err?.message || '请稍后重试'}`,
    })
  } finally {
    loading.value = false
    await scrollToEnd()
  }
}
</script>

<style scoped>
.ai-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

.ai-header {
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.ai-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}
.ai-subtitle {
  font-size: 12px;
  color: #909399;
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}
.empty-hint p {
  margin: 16px 0;
  font-size: 14px;
}
.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chat-msg {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 85%;
}
.chat-msg.user {
  flex-direction: row-reverse;
  margin-left: auto;
}
.msg-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e6f0ff;
  color: #4a7afe;
}
.chat-msg.user .msg-avatar {
  background: #e8ffea;
  color: #00b42a;
}
.msg-body {
  flex: 1;
  min-width: 0;
}
.msg-content {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  background: #fff;
  border: 1px solid #e4e7ed;
}
.chat-msg.user .msg-content {
  background: #4a7afe;
  color: #fff;
  border-color: #4a7afe;
}
.answer-text {
  word-break: break-word;
}
.loading-content {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.ref-collapse {
  margin-top: 12px;
  border-top: 1px dashed #e4e7ed;
}
.ref-item {
  margin-bottom: 8px;
}
.ref-index {
  font-size: 12px;
  color: #4a7afe;
  font-weight: 600;
}
.ref-snippet {
  margin: 4px 0 0;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}
.input-area .el-button {
  align-self: flex-end;
}
</style>
