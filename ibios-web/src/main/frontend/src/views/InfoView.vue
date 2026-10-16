<template>
  <div>
    <h1>运行状态</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else>
      <div class="card">
        <h2>系统信息</h2>
        <dl class="info-list">
          <dt>版本</dt>
          <dd class="mono">{{ info.version }}</dd>
        </dl>
      </div>
      <div class="card" style="margin-top: 20px;">
        <h2>健康状态</h2>
        <div class="status-badge" :class="health.status">
          {{ health.status }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const info = ref({})
const health = ref({})
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const [infoRes, healthRes] = await Promise.all([
      fetch('/api/v1/info'),
      fetch('/api/v1/health')
    ])
    if (!infoRes.ok || !healthRes.ok) throw new Error('Failed to load status')
    const infoData = await infoRes.json()
    const healthData = await healthRes.json()
    info.value = infoData.data || {}
    health.value = healthData.data || {}
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.info-list {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
}

.info-list dt {
  color: var(--text-secondary);
  font-weight: 500;
}

.info-list dd {
  color: var(--text-primary);
}

.status-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 4px;
  font-weight: 500;
  text-transform: uppercase;
}

.status-badge.ok {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success);
}

h2 {
  font-size: 1rem;
  margin-bottom: 16px;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
