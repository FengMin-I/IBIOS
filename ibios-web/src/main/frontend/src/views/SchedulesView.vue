<template>
  <div class="schedules">
    <header class="page-header">
      <h1 class="title">定时任务</h1>
      <span class="task-count">{{ tasks.length }} 个任务</span>
    </header>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="tasks.length === 0" class="empty">
      <p>暂无定时任务</p>
      <p class="empty-hint">在 Profile 的 schedules 配置中添加定时任务</p>
    </div>

    <div v-else class="task-list">
      <div v-for="task in tasks" :key="task.taskId" class="task-card">
        <div class="task-header">
          <div class="task-title">
            <span class="task-id">{{ task.taskId }}</span>
            <span class="task-profile">{{ task.profileName }}</span>
          </div>
          <div class="task-actions">
            <button
              class="btn btn-primary"
              :disabled="task._running"
              @click="runNow(task)"
            >
              {{ task._running ? '执行中...' : '立即执行' }}
            </button>
            <button
              :class="['btn', task.enabled ? 'btn-danger' : 'btn-success']"
              @click="toggleEnabled(task)"
            >
              {{ task.enabled ? '停用' : '启用' }}
            </button>
          </div>
        </div>

        <div class="task-meta">
          <div class="meta-item">
            <span class="meta-label">Cron</span>
            <code class="meta-value">{{ task.cron }}</code>
          </div>
          <div class="meta-item">
            <span class="meta-label">时区</span>
            <span class="meta-value">{{ task.zone }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">下次触发</span>
            <span class="meta-value">{{ formatTime(task.nextRunAt) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">上次结果</span>
            <span :class="['status-badge', task.lastStatus]">
              {{ task.lastStatus || '未执行' }}
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">执行次数</span>
            <span class="meta-value">{{ task.runCount }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">状态</span>
            <span :class="['enabled-badge', task.enabled ? 'on' : 'off']">
              {{ task.enabled ? '已启用' : '已停用' }}
            </span>
          </div>
        </div>

        <div class="task-message">
          <span class="meta-label">消息</span>
          <p>{{ task.message }}</p>
        </div>

        <div v-if="task._executions && task._executions.length > 0" class="executions">
          <h3 class="exec-title">执行历史</h3>
          <table class="exec-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>会话</th>
                <th>结果</th>
                <th>耗时</th>
                <th>错误</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="exec in task._executions" :key="exec.startedAt" :class="{ failed: !exec.success }">
                <td>{{ formatTime(exec.startedAt) }}</td>
                <td class="session-id">{{ exec.sessionId || '-' }}</td>
                <td>
                  <span :class="['status-badge', exec.success ? 'success' : 'failed']">
                    {{ exec.success ? '成功' : '失败' }}
                  </span>
                </td>
                <td>{{ exec.durationMs != null ? exec.durationMs + 'ms' : '-' }}</td>
                <td class="error-msg">{{ exec.errorMessage || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <button
          v-if="!task._executionsLoaded"
          class="btn btn-ghost"
          @click="loadExecutions(task)"
        >
          查看执行历史
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const tasks = ref([])
const loading = ref(true)

async function fetchTasks() {
  try {
    const res = await fetch('/api/v1/schedules')
    if (res.ok) {
      const data = await res.json()
      tasks.value = (data.data || []).map(t => ({ ...t, _running: false, _executions: null, _executionsLoaded: false }))
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

async function runNow(task) {
  task._running = true
  try {
    const res = await fetch(`/api/v1/schedules/${task.taskId}/run`, { method: 'POST' })
    if (res.ok) {
      await fetchTasks()
    }
  } catch (e) {
    // ignore
  } finally {
    task._running = false
  }
}

async function toggleEnabled(task) {
  try {
    const res = await fetch(`/api/v1/schedules/${task.taskId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ enabled: !task.enabled })
    })
    if (res.ok) {
      task.enabled = !task.enabled
    }
  } catch (e) {
    // ignore
  }
}

async function loadExecutions(task) {
  try {
    const res = await fetch(`/api/v1/schedules/${task.taskId}/executions`)
    if (res.ok) {
      const data = await res.json()
      task._executions = data.data || []
      task._executionsLoaded = true
    }
  } catch (e) {
    // ignore
  }
}

function formatTime(iso) {
  if (!iso) return '-'
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch (e) {
    return iso
  }
}

onMounted(fetchTasks)
</script>

<style scoped>
.schedules {
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

.task-count {
  font-size: 0.875rem;
  color: var(--text-muted);
  padding: 4px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.loading, .empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}

.empty-hint {
  font-size: 0.875rem;
  margin-top: 8px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px;
}

.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-id {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.task-profile {
  font-size: 0.8125rem;
  color: var(--accent);
  padding: 2px 8px;
  background: rgba(255, 106, 0, 0.1);
  border-radius: 4px;
}

.task-actions {
  display: flex;
  gap: 8px;
}

.task-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
  white-space: nowrap;
}

.meta-value {
  font-size: 0.875rem;
  color: var(--text-primary);
}

code.meta-value {
  font-family: 'JetBrains Mono', monospace;
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.8125rem;
}

.task-message {
  padding: 12px;
  background: var(--bg-hover);
  border-radius: 6px;
  margin-bottom: 16px;
}

.task-message p {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.status-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.status-badge.success {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success, #22c55e);
}

.status-badge.failed {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.enabled-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.enabled-badge.on {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success, #22c55e);
}

.enabled-badge.off {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.btn {
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 0.8125rem;
  cursor: pointer;
  transition: all 0.2s;
  background: transparent;
  color: var(--text-secondary);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--accent);
  color: white;
  border-color: var(--accent);
}

.btn-success {
  color: var(--success, #22c55e);
  border-color: var(--success, #22c55e);
}

.btn-danger {
  color: #ef4444;
  border-color: #ef4444;
}

.btn-ghost {
  border: none;
  color: var(--accent);
  padding: 4px 0;
}

.btn-ghost:hover {
  text-decoration: underline;
}

.executions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.exec-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.exec-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.exec-table th {
  text-align: left;
  padding: 8px;
  color: var(--text-muted);
  font-weight: 500;
  border-bottom: 1px solid var(--border);
}

.exec-table td {
  padding: 8px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
}

.exec-table tr.failed td {
  background: rgba(239, 68, 68, 0.03);
}

.session-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
}

.error-msg {
  color: #ef4444;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
