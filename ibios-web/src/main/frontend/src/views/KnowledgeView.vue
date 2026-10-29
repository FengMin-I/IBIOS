<template>
  <div class="knowledge">
    <header class="page-header">
      <h1 class="title">知识库</h1>
      <span class="count">0 个</span>
    </header>

    <table class="knowledge-table">
      <thead>
        <tr>
          <th>名称</th>
          <th>描述</th>
          <th>文档数</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="bases.length === 0">
          <td colspan="3" class="empty">暂无知识库</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const bases = ref([])
</script>

<style scoped>
.knowledge {
  max-width: 960px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.count {
  font-size: 0.875rem;
  color: var(--text-muted);
  padding: 4px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}
</style>
