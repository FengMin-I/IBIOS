<template>
  <div class="notify-channels">
    <header class="page-header">
      <h1 class="title">Notify 渠道</h1>
      <span class="channel-count">{{ channels.length }} 个渠道</span>
      <button class="btn btn-primary create-btn" @click="openCreate">+ 新建渠道</button>
    </header>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="channels.length === 0" class="empty">
      <p>暂无 Notify 渠道</p>
      <p class="empty-hint">Agent 通过 notify 工具按渠道名称发送消息，例如 team-webhook</p>
    </div>

    <div v-else class="channel-list">
      <div v-for="channel in channels" :key="channel.id" class="channel-card">
        <div class="channel-header">
          <div class="channel-title">
            <span class="channel-name">{{ channel.name }}</span>
            <span class="type-badge">{{ channel.type }}</span>
          </div>
          <div class="channel-actions">
            <button class="btn" @click="openEdit(channel)">编辑</button>
            <button class="btn btn-danger" @click="removeChannel(channel)">删除</button>
          </div>
        </div>

        <div class="channel-meta">
          <div class="meta-item">
            <span class="meta-label">URL</span>
            <code class="meta-value url-value">{{ displayUrl(channel) }}</code>
            <button
              v-if="channel.url"
              class="btn-reveal"
              @click="toggleReveal(channel.id)"
            >
              {{ revealed.has(channel.id) ? '隐藏' : '显示' }}
            </button>
          </div>
          <div class="meta-item">
            <span class="meta-label">描述</span>
            <span class="meta-value">{{ channel.description || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <ModalComponent v-model="showDialog" :title="editing ? '编辑 Notify 渠道' : '新建 Notify 渠道'">
      <div class="form-group">
        <label>名称</label>
        <input v-model="form.name" class="form-input" placeholder="如：team-webhook" />
      </div>
      <div class="form-group">
        <label>类型</label>
        <select v-model="form.type" class="form-input">
          <option v-for="type in channelTypes" :key="type" :value="type">{{ type }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>Webhook URL</label>
        <input v-model="form.url" class="form-input" placeholder="https://..." />
      </div>
      <div class="form-group">
        <label>描述</label>
        <input v-model="form.description" class="form-input" placeholder="可选，说明渠道用途" />
      </div>
      <div v-if="dialogError" class="error-msg">{{ dialogError }}</div>
      <div class="dialog-actions">
        <button class="btn" @click="closeDialog">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="saveChannel">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </ModalComponent>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ModalComponent from '../components/ModalComponent.vue'

const channelTypes = ['feishu', 'wecom', 'dingtalk', 'webhook']
const channels = ref([])
const revealed = ref(new Set())
const loading = ref(true)
const errorMessage = ref(null)
const showDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const dialogError = ref(null)
const form = ref({ name: '', type: 'webhook', url: '', description: '' })

async function fetchChannels() {
  try {
    const res = await fetch('/api/v1/notify-channels')
    if (res.ok) {
      const data = await res.json()
      channels.value = data.data || []
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function maskUrl(url) {
  if (!url) return '-'
  try {
    const parsed = new URL(url)
    return `${parsed.protocol}//${parsed.host}/••••••`
  } catch (e) {
    return url.length > 12 ? url.slice(0, 12) + '••••••' : '••••••'
  }
}

function displayUrl(channel) {
  if (!channel.url) return '-'
  return revealed.value.has(channel.id) ? channel.url : maskUrl(channel.url)
}

function toggleReveal(id) {
  if (revealed.value.has(id)) {
    revealed.value.delete(id)
  } else {
    revealed.value.add(id)
  }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', type: 'webhook', url: '', description: '' }
  dialogError.value = null
  showDialog.value = true
}

function openEdit(channel) {
  editing.value = channel
  form.value = {
    name: channel.name,
    type: channel.type,
    url: channel.url || '',
    description: channel.description || ''
  }
  dialogError.value = null
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
  dialogError.value = null
}

async function saveChannel() {
  saving.value = true
  dialogError.value = null
  errorMessage.value = null
  const isEdit = editing.value != null
  const url = isEdit
    ? `/api/v1/notify-channels/${editing.value.id}`
    : '/api/v1/notify-channels'
  try {
    const res = await fetch(url, {
      method: isEdit ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      showDialog.value = false
      await fetchChannels()
    } else {
      dialogError.value = data.message || `保存失败（${res.status}）`
    }
  } catch (e) {
    dialogError.value = '保存失败，请检查网络'
  } finally {
    saving.value = false
  }
}

async function removeChannel(channel) {
  if (!window.confirm(`确认删除渠道 ${channel.name}？`)) {
    return
  }
  errorMessage.value = null
  try {
    const res = await fetch(`/api/v1/notify-channels/${channel.id}`, { method: 'DELETE' })
    if (res.ok) {
      await fetchChannels()
    } else {
      const data = await res.json().catch(() => ({}))
      errorMessage.value = data.message || `删除失败（${res.status}）`
    }
  } catch (e) {
    errorMessage.value = '删除失败，请检查网络'
  }
}

onMounted(fetchChannels)
</script>

<style scoped>
.notify-channels {
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

.channel-count {
  font-size: 0.875rem;
  color: var(--text-muted);
  padding: 4px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.create-btn {
  margin-left: auto;
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

.channel-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.channel-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px;
}

.channel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.channel-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.type-badge {
  font-size: 0.75rem;
  color: var(--accent);
  padding: 2px 8px;
  background: rgba(255, 106, 0, 0.1);
  border-radius: 10px;
  font-family: 'JetBrains Mono', monospace;
}

.channel-actions {
  display: flex;
  gap: 8px;
}

.channel-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
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

.url-value {
  font-family: 'JetBrains Mono', monospace;
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.8125rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-reveal {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0;
  white-space: nowrap;
}

.btn-reveal:hover {
  text-decoration: underline;
}

.error-banner {
  margin-top: 16px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border-radius: 6px;
  font-size: 0.875rem;
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

.btn-danger {
  color: #ef4444;
  border-color: #ef4444;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  background: var(--bg-hover);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 0.875rem;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--accent);
}

.error-msg {
  color: #ef4444;
  font-size: 0.8125rem;
  margin-bottom: 12px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
}
</style>
