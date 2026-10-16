<template>
  <div class="overview">
    <header class="overview-header">
      <div class="header-left">
        <h1 class="title">IBIOS</h1>
        <span class="status-badge">
          <span class="status-dot"></span>
          运行中
        </span>
        <span class="version">v0.1.0 · 开发预览</span>
      </div>
    </header>

    <p class="subtitle">
      装在你自己基础设施上的分布式 AI Agent 操作系统 —— 统一底座运行多个业务 Agent
    </p>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-number">{{ stats.agentCount }}</div>
        <div class="stat-label">Agent</div>
        <div class="stat-desc">已配置的 Profile</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.toolCount }}</div>
        <div class="stat-label">内置 Tool</div>
        <div class="stat-desc">文件 / Shell / HTTP / 记忆 ...</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.activeSessions }}</div>
        <div class="stat-label">活跃会话</div>
        <div class="stat-desc">待接入实时统计</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ stats.providerCount }}</div>
        <div class="stat-label">Provider</div>
        <div class="stat-desc">{{ stats.providerStatus }}</div>
      </div>
    </div>

    <h2 class="section-title">五大核心能力</h2>
    <div class="capabilities-grid">
      <div class="capability-card">
        <div class="capability-number">1</div>
        <div class="capability-content">
          <h3>对接 LLM</h3>
          <p>显式 Provider 映射，多家协议统一</p>
        </div>
      </div>
      <div class="capability-card">
        <div class="capability-number">2</div>
        <div class="capability-content">
          <h3>ReAct 循环</h3>
          <p>自实现推理-行动循环，完全可控</p>
        </div>
      </div>
      <div class="capability-card">
        <div class="capability-number">3</div>
        <div class="capability-content">
          <h3>Memory</h3>
          <p>跨对话长期记忆，成长可积累</p>
        </div>
      </div>
      <div class="capability-card">
        <div class="capability-number">4</div>
        <div class="capability-content">
          <h3>Plugin Tool</h3>
          <p>内置 Tool + MCP，强制沙箱白名单</p>
        </div>
      </div>
      <div class="capability-card">
        <div class="capability-number">5</div>
        <div class="capability-content">
          <h3>Web Service</h3>
          <p>REST API + 管理台对外门面</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const stats = ref({
  agentCount: 0,
  toolCount: 0,
  activeSessions: '—',
  providerCount: 0,
  providerStatus: '待检测'
})

onMounted(async () => {
  try {
    const [profilesRes, toolsRes] = await Promise.all([
      fetch('/api/v1/profiles'),
      fetch('/api/v1/tools')
    ])
    if (profilesRes.ok) {
      const profilesData = await profilesRes.json()
      stats.value.agentCount = (profilesData.data || []).length
    }
    if (toolsRes.ok) {
      const toolsData = await toolsRes.json()
      stats.value.toolCount = (toolsData.data || []).length
    }
    if (stats.value.agentCount > 0) {
      stats.value.providerStatus = '已连通'
    }
  } catch (e) {
    // 静态降级，保留默认值
  }
})
</script>

<style scoped>
.overview {
  max-width: 960px;
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: 20px;
  font-size: 0.875rem;
  color: var(--success);
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: var(--success);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.version {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.subtitle {
  font-size: 1rem;
  color: var(--text-secondary);
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 40px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 24px;
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--accent);
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.stat-desc {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.capabilities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.capability-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.capability-number {
  width: 32px;
  height: 32px;
  background: var(--accent);
  color: #000;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.capability-content h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.capability-content p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}
</style>
