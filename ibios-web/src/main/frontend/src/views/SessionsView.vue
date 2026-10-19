<template>
  <div>
    <h1>会话列表</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="sessions.length === 0" class="empty">暂无会话</div>
    <table v-else>
      <thead>
        <tr>
          <th>Session ID</th>
          <th>Profile</th>
          <th>消息数</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="session in sessions" :key="session.sessionId">
          <td>
            <router-link :to="`/sessions/${session.sessionId}`" class="mono link">
              {{ session.sessionId }}
            </router-link>
          </td>
          <td>{{ session.profileName }}</td>
          <td>{{ session.messages?.length || 0 }}</td>
          <td>
            <button class="btn-delete" @click="deleteSession(session.sessionId)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const sessions = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/sessions')
    if (!response.ok) throw new Error('Failed to load sessions')
    const data = await response.json()
    sessions.value = data.data || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

async function deleteSession(sessionId) {
  if (!confirm(`确认删除会话 "${sessionId}"？`)) return
  try {
    const res = await fetch(`/api/v1/sessions/${sessionId}`, { method: 'DELETE' })
    if (res.ok) {
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    }
  } catch (e) {
    // silent
  }
}
</script>

<style scoped>
.mono {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.link {
  color: var(--accent);
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
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

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  padding: 12px 16px;
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 14px;
  border-bottom: 1px solid var(--border);
}

td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
  vertical-align: middle;
}

.btn-delete {
  padding: 5px 14px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete:hover {
  border-color: var(--error);
  color: var(--error);
}
</style>
