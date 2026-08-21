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

function load(): AiChatMessage[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AiChatMessage[]) : []
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
    messages.value.push(msg)
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