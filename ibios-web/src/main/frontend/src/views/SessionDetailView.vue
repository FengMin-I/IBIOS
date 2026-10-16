<template>
  <div class="session-detail">
    <header class="detail-header">
      <router-link to="/sessions" class="back-link">&larr; 返回会话列表</router-link>
      <div class="session-meta">
        <span class="session-id mono">{{ sessionId }}</span>
        <span v-if="profileName" class="profile-tag">{{ profileName }}</span>
        <span v-if="messages.length" class="msg-count">{{ messages.length }} 条消息</span>
      </div>
    </header>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="messages.length === 0" class="empty">暂无消息</div>

    <div v-else class="message-list">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['message-row', msg.role === 'USER' ? 'user' : 'assistant']"
      >
        <div v-if="msg.role === 'USER'" class="message-bubble user-bubble">
          <div class="bubble-label">用户</div>
          <div class="bubble-content">{{ msg.content }}</div>
        </div>

        <div v-else-if="msg.role === 'TOOL'" class="tool-call standalone-tool">
          <span class="tool-label">工具</span>
          <span class="tool-name mono">{{ msg.toolName || 'unknown' }}</span>
          <div class="tool-result mono">{{ msg.content }}</div>
        </div>

        <template v-else>
          <div class="message-bubble assistant-bubble">
            <div class="bubble-label">助手</div>
            <div v-if="msg.content" class="bubble-content">{{ msg.content }}</div>
            <div v-else class="bubble-empty">(空)</div>
          </div>

          <div v-if="msg.toolName" class="tool-call">
            <span class="tool-label">工具</span>
            <span class="tool-name mono">{{ msg.toolName }}</span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const sessionId = ref(route.params.id)
const profileName = ref('')
const messages = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const response = await fetch(`/api/v1/sessions/${sessionId.value}`)
    if (!response.ok) throw new Error('Failed to load session')
    const data = await response.json()
    const session = data.data
    profileName.value = session.profileName || ''
    messages.value = (session.messages || []).map((m) => ({
      role: m.role,
      content: m.content,
      toolName: m.toolName || null
    }))
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.session-detail {
  max-width: 900px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.back-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  transition: all 0.2s;
}

.back-link:hover {
  color: var(--text-primary);
  border-color: var(--accent);
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.session-id {
  font-size: 14px;
  color: var(--text-secondary);
}

.profile-tag {
  background: var(--accent);
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
}

.msg-count {
  font-size: 13px;
  color: var(--text-secondary);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-row.user {
  align-items: flex-end;
}

.message-row.assistant {
  align-items: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
}

.bubble-label {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  opacity: 0.7;
}

.bubble-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-empty {
  font-size: 13px;
  color: var(--text-secondary);
  font-style: italic;
}

.user-bubble {
  background: #5c2a1a;
  color: #f0d0c0;
}

.assistant-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-primary);
}

.tool-call {
  margin-left: 4px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.standalone-tool {
  align-items: flex-start;
  margin-left: 0;
}

.tool-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.tool-name {
  color: var(--accent);
  font-weight: 600;
}

.tool-result {
  width: 100%;
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px solid var(--border);
  font-size: 12px;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.mono {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.loading,
.error,
.empty {
  color: var(--text-secondary);
  padding: 40px 0;
  text-align: center;
}

.error {
  color: #e74c3c;
}
</style>
