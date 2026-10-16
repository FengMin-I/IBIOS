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
</style>
