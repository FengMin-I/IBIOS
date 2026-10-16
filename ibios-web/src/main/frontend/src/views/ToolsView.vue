<template>
  <div>
    <h1>Tool 列表</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="tools.length === 0" class="empty">暂无 Tool</div>
    <table v-else>
      <thead>
        <tr>
          <th>名称</th>
          <th>描述</th>
          <th>Input Schema</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tool in tools" :key="tool.name">
          <td class="mono">{{ tool.name }}</td>
          <td>{{ tool.description }}</td>
          <td class="mono schema">{{ tool.inputSchema }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const tools = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/tools')
    if (!response.ok) throw new Error('Failed to load tools')
    const data = await response.json()
    tools.value = data.data || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.schema {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
