<template>
  <div>
    <h1>Profile 列表</h1>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="profiles.length === 0" class="empty">暂无 Profile</div>
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
        <tr v-for="profile in profiles" :key="profile.name">
          <td class="mono">{{ profile.name }}</td>
          <td>{{ profile.description || '-' }}</td>
          <td>{{ profile.provider }}</td>
          <td class="mono">{{ profile.model }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const profiles = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/profiles')
    if (!response.ok) throw new Error('Failed to load profiles')
    const data = await response.json()
    profiles.value = data.data || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>
