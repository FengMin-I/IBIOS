<template>
  <div>
    <h1>Provider</h1>
    <div v-if="providers.length === 0" class="empty">暂无 Provider</div>
    <div v-else class="card">
      <div class="provider-list">
        <div v-for="provider in providers" :key="provider.name" class="provider-item">
          <div class="provider-name">{{ provider.name }}</div>
          <span class="status-badge" :class="provider.connected ? 'connected' : 'disconnected'">
            {{ provider.connected ? '已连通' : '未配置' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const providers = ref([])

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/info')
    if (response.ok) {
      const data = await response.json()
      providers.value = data.data?.providers || []
    }
  } catch (e) {
    // 静默失败
  }
})
</script>

<style scoped>
.provider-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.provider-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: var(--bg-primary);
  border-radius: 6px;
}

.provider-name {
  font-weight: 600;
  color: var(--text-primary);
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.8125rem;
  font-weight: 500;
}

.status-badge.connected {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success);
}

.status-badge.disconnected {
  background: rgba(102, 102, 102, 0.1);
  color: var(--text-muted);
}
</style>
