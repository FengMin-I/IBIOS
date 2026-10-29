<template>
  <nav class="sidebar">
    <div class="logo">
      <img src="/logo.svg" alt="IBIOS" class="logo-img" />
    </div>
    <ul class="nav-list">
      <li v-for="item in navItems" :key="item.path || item.group">
        <template v-if="item.children">
          <div class="nav-group">
            <div class="group-header" @click="toggleGroup(item.group)">
              <span>{{ item.label }}</span>
              <span class="group-arrow" :class="{ expanded: expandedGroups.has(item.group) }">▼</span>
            </div>
            <ul v-show="expandedGroups.has(item.group)" class="nav-sublist">
              <li v-for="child in item.children" :key="child.path">
                <router-link :to="child.path" :class="{ active: $route.path === child.path }">
                  {{ child.label }}
                </router-link>
              </li>
            </ul>
          </div>
        </template>
        <template v-else>
          <router-link :to="item.path" :class="{ active: $route.path === item.path }">
            {{ item.label }}
          </router-link>
        </template>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { ref } from 'vue'

const expandedGroups = ref(new Set(['os-runtime']))

const navItems = [
  { path: '/', label: '概览' },
  { path: '/agents', label: 'Agent 列表' },
  { path: '/schedules', label: '定时任务' },
  { path: '/skills', label: 'Skill 列表' },
  { path: '/knowledge', label: '知识库' },
  {
    group: 'os-runtime',
    label: 'OS 运行时',
    children: [
      { path: '/sessions', label: '会话列表' },
      { path: '/providers', label: 'Provider 列表' },
      { path: '/tools', label: 'Tool 列表' },
      { path: '/sandbox', label: 'SandBox 列表' },
      { path: '/notify-channels', label: 'Notify 渠道' }
    ]
  }
]

function toggleGroup(group) {
  if (expandedGroups.value.has(group)) {
    expandedGroups.value.delete(group)
  } else {
    expandedGroups.value.add(group)
  }
}
</script>

<style scoped>
.sidebar {
  width: 240px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  align-self: flex-start;
  height: 100vh;
  overflow: hidden;
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
  margin: 0;
  padding: 0;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.nav-list > li {
  margin: 4px 12px;
}

.nav-list a {
  display: block;
  padding: 10px 12px;
  color: var(--text-secondary);
  border-radius: 4px;
  transition: all 0.2s;
  text-decoration: none;
}

.nav-list a:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.nav-list a.active {
  background: var(--accent);
  color: white;
}

.nav-group {
  margin: 4px 0;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  user-select: none;
}

.group-header:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.group-arrow {
  font-size: 10px;
  transition: transform 0.2s;
}

.group-arrow.expanded {
  transform: rotate(0deg);
}

.group-arrow:not(.expanded) {
  transform: rotate(-90deg);
}

.nav-sublist {
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav-sublist li {
  margin: 2px 0;
}

.nav-sublist a {
  padding: 8px 12px 8px 24px;
  font-size: 13px;
}

@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }
}
</style>
