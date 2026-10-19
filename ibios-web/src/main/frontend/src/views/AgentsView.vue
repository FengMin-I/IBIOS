<template>
  <div class="agent-management">
    <header class="page-header">
      <h1 class="page-title">Agent 管理</h1>
      <button class="btn-create" @click="showCreateDialog = true">+ 创建 Agent</button>
    </header>

    <div v-if="loading" class="loading">加载中...</div>
    <table v-else>
      <thead>
        <tr>
          <th>name</th>
          <th>provider</th>
          <th>model</th>
          <th>定时</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="agent in agents" :key="agent.name">
          <td class="mono name-cell">{{ agent.name }}</td>
          <td>{{ agent.provider }}</td>
          <td class="mono">{{ agent.model }}</td>
          <td class="mono cron-cell">
            <span v-if="agent.schedules && agent.schedules.length">
              {{ agent.schedules.map(s => s.cron).join(' ') }}
            </span>
            <span v-else>–</span>
          </td>
          <td class="actions-cell">
            <button class="btn-trigger" @click="triggerAgent(agent.name)">立即触发</button>
            <button class="btn-edit" @click="editAgent(agent.name)">详情</button>
            <button class="btn-delete" @click="deleteAgent(agent.name)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog card">
        <h2 class="dialog-title">创建 Agent</h2>
        <div class="form-group">
          <label>名称</label>
          <input v-model="newAgent.name" class="form-input" placeholder="如：weather-daily" />
        </div>
        <div class="form-group">
          <label>描述</label>
          <textarea v-model="newAgent.description" class="form-textarea" placeholder="描述这个 Agent 的用途和功能" rows="4"></textarea>
        </div>
        <div v-if="createError" class="error-msg">{{ createError }}</div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="showCreateDialog = false">取消</button>
          <button class="btn-confirm" :disabled="creating" @click="createAgent">
            {{ creating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const agents = ref([])
const loading = ref(true)
const showCreateDialog = ref(false)
const creating = ref(false)
const createError = ref(null)
const newAgent = ref({ name: '', description: '' })

async function loadAgents() {
  try {
    const res = await fetch('/api/v1/agents')
    if (res.ok) {
      const data = await res.json()
      agents.value = data.data || []
    }
  } catch (e) {
    // silent
  } finally {
    loading.value = false
  }
}

async function createAgent() {
  if (!newAgent.value.name.trim() || !newAgent.value.description.trim()) {
    createError.value = '请填写名称和描述'
    return
  }
  creating.value = true
  createError.value = null
  try {
    const res = await fetch('/api/v1/agents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: newAgent.value.name,
        description: newAgent.value.description
      })
    })
    const data = await res.json()
    if (!res.ok) {
      createError.value = data.message || '创建失败'
      return
    }
    showCreateDialog.value = false
    newAgent.value = { name: '', description: '' }
    await loadAgents()
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

function editAgent(name) {
  router.push(`/agents/${name}`)
}

async function deleteAgent(name) {
  if (!confirm(`确认删除 Agent "${name}"？目录将移至 archive。`)) return
  try {
    const res = await fetch(`/api/v1/agents/${name}`, { method: 'DELETE' })
    if (res.ok) await loadAgents()
  } catch (e) {
    // silent
  }
}

async function triggerAgent(name) {
  if (!confirm(`确认立即触发 Agent "${name}"？执行结果将发送到飞书。`)) return
  try {
    const res = await fetch(`/api/v1/agents/${name}/invoke`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: '到点了，按你的说明执行。完成后调用 notify 发送到 team-webhook。' })
    })
    if (res.ok) {
      const data = await res.json()
      alert('Agent 已触发并执行完成，结果已发送到飞书。请查看会话记录。')
    } else {
      const data = await res.json()
      alert('触发失败：' + (data.message || '未知错误'))
    }
  } catch (e) {
    alert('触发失败：' + e.message)
  }
}

onMounted(loadAgents)
</script>

<style scoped>
.agent-management {
  max-width: 1100px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.btn-create {
  padding: 8px 20px;
  background: var(--accent);
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-create:hover {
  opacity: 0.9;
}

.error-msg {
  color: var(--error);
  font-size: 14px;
  margin-bottom: 16px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  padding: 12px 16px;
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 14px;
  border-bottom: 1px solid var(--border);
}

td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
  vertical-align: middle;
}

.name-cell {
  font-weight: 600;
  color: var(--text-primary);
}

.cron-cell {
  color: var(--text-secondary);
  font-size: 13px;
}

.actions-cell {
  white-space: nowrap;
}

.btn-edit {
  padding: 5px 14px;
  background: transparent;
  border: 1px solid var(--accent);
  border-radius: 4px;
  color: var(--accent);
  font-size: 13px;
  margin-right: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit:hover {
  background: var(--accent);
  color: white;
}

.btn-trigger {
  padding: 5px 14px;
  background: transparent;
  border: 1px solid var(--success, #10b981);
  border-radius: 4px;
  color: var(--success, #10b981);
  font-size: 13px;
  margin-right: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-trigger:hover {
  background: var(--success, #10b981);
  color: white;
}

.btn-delete {
  padding: 5px 14px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete:hover {
  border-color: var(--error);
  color: var(--error);
}

.mono {
  font-family: 'JetBrains Mono', monospace;
}

.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: 480px;
  padding: 24px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--accent);
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.btn-cancel {
  padding: 8px 20px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  border-color: var(--text-secondary);
  color: var(--text-primary);
}

.btn-confirm {
  padding: 8px 20px;
  background: var(--accent);
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-confirm:hover {
  opacity: 0.9;
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
