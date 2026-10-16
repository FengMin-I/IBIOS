<template>
  <div>
    <h1>长期记忆</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="!content" class="empty">记忆为空</div>
    <div v-else class="card">
      <pre class="memory-content">{{ content }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const content = ref(null)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/memory')
    if (!response.ok) throw new Error('Failed to load memory')
    const data = await response.json()
    content.value = data.data?.content
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.memory-content {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--text-primary);
}
</style>
