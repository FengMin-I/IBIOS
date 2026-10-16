<template>
  <nav class="sidebar">
    <div class="logo">
      <img src="/logo.svg" alt="IBIOS" class="logo-img" />
    </div>
    <ul class="nav-list">
      <li v-for="item in navItems" :key="item.path">
        <router-link :to="item.path" :class="{ active: $route.path === item.path }">
          {{ item.label }}
        </router-link>
      </li>
    </ul>
  </nav>
</template>

<script setup>
const navItems = [
  { path: '/', label: '概览' },
  { path: '/agents', label: 'Agent' },
  { path: '/providers', label: 'Provider' },
  { path: '/tools', label: 'Tool' },
  { path: '/sandbox', label: 'Sandbox 白名单' },
  { path: '/memory', label: '长期记忆' },
  { path: '/info', label: '运行状态' },
  { path: '/schedules', label: '定时任务' },
  { path: '/sessions', label: '会话' }
]
</script>

<style scoped>
.sidebar {
  width: 240px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  padding: 24px 0;
  display: flex;
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px 24px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 16px;
}

.logo-img {
  height: 32px;
  width: auto;
}

.nav-list {
  list-style: none;
}

.nav-list li {
  margin: 4px 12px;
}

.nav-list a {
  display: block;
  padding: 10px 12px;
  color: var(--text-secondary);
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-list a:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.nav-list a.active {
  background: var(--accent);
  color: white;
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }
}
</style>
