import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ReferenceDoc } from '@/types'

/** AI 对话消息（本次登录内保留，切换页面不丢失、刷新不丢失、登出清空） */
export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
  references?: ReferenceDoc[]
}

/** sessionStorage key：同标签页内刷新仍保留；登出时 clearMessages 清空；关闭标签页自动释放 */
const STORAGE_KEY = 'ai_chat_messages'

/** 保留的最大消息条数（超出丢弃最旧的） */
const MAX_MESSAGE_COUNT = 60
/** 单条消息内容最大长度（超出截断，防止 sessionStorage 溢出） */
const MAX_CONTENT_LENGTH = 20000

/** 规范化单条消息：内容截断 + 归一化，避免字段缺失 */
function normalizeMessage(msg: AiChatMessage): AiChatMessage {
  const content = msg.content.length > MAX_CONTENT_LENGTH
    ? msg.content.slice(0, MAX_CONTENT_LENGTH) + '…'
    : msg.content
  return { role: msg.role, content, references: msg.references }
}

function load(): AiChatMessage[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as AiChatMessage[]
    // 旧数据可能超过上限，读取时同样裁剪，避免 sessionStorage 溢出
    return Array.isArray(parsed)
      ? parsed.slice(-MAX_MESSAGE_COUNT).map(normalizeMessage)
      : []
  } catch {
    return []
  }
}

function save(messages: AiChatMessage[]) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages))
  } catch {
    // 存储失败（如隐私模式配额）时静默忽略，仅本次内存保留
  }
}

export const useAiStore = defineStore('ai', () => {
  const messages = ref<AiChatMessage[]>(load())

  function addMessage(msg: AiChatMessage) {
    messages.value.push(normalizeMessage(msg))
    // 只保留最近 MAX_MESSAGE_COUNT 条，防止会话无限增长撑爆 sessionStorage
    if (messages.value.length > MAX_MESSAGE_COUNT) {
      messages.value = messages.value.slice(messages.value.length - MAX_MESSAGE_COUNT)
    }
    save(messages.value)
  }

  function clearMessages() {
    messages.value = []
    sessionStorage.removeItem(STORAGE_KEY)
  }

  /** 最近的多轮历史（不含最后一条 user 提问），用于传给 RAG 做上下文 */
  function getHistory(): Array<{ role: string; content: string }> {
    return messages.value.slice(0, -1).map((m) => ({ role: m.role, content: m.content }))
  }

  return {
    messages,
    addMessage,
    clearMessages,
    getHistory,
  }
})