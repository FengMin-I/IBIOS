<template>
  <div>
    <h1>Agent</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="agents.length === 0" class="empty">暂无 Agent</div>
    <table v-else>
      <thead>
        <tr>
          <th>名称</th>
          <th>描述</th>
          <th>Provider</th>
          <th>Model</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="agent in agents" :key="agent.name">
          <td class="mono">{{ agent.name }}</td>
          <td>{{ agent.description || '-' }}</td>
          <td>{{ agent.provider }}</td>
          <td class="mono">{{ agent.model }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const agents = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/profiles')
    if (response.ok) {
      const data = await response.json()
      agents.value = data.data || []
    }
  } catch (e) {
    // 静默失败
  } finally {
    loading.value = false
  }
})
</script>
