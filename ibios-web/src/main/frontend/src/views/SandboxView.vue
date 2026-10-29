<template>
  <div class="sandbox">
    <header class="page-header">
      <h1 class="title">Sandbox 白名单</h1>
      <span class="entry-count">{{ entries.length }} 条规则</span>
      <button class="btn btn-primary create-btn" @click="openCreate">+ 新增白名单</button>
    </header>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab"
        :class="{ active: activeType === tab.key }"
        @click="activeType = tab.key"
      >
        {{ tab.label }}
        <span class="tab-count">{{ countOf(tab.key) }}</span>
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="filteredEntries.length === 0" class="empty">
      <p>暂无白名单规则</p>
      <p class="empty-hint">Agent 的文件、Shell、HTTP 操作都会先过这张白名单</p>
    </div>

    <div v-else class="card">
      <table>
        <thead>
          <tr>
            <th class="col-type">类别</th>
            <th>规则</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in filteredEntries" :key="entry.id">
            <td class="col-type">
              <span class="type-badge" :class="'type-' + entry.type.toLowerCase()">
                {{ typeLabel(entry.type) }}
              </span>
            </td>
            <td class="mono">{{ entry.value }}</td>
            <td class="col-action">
              <button class="btn btn-danger" @click="removeEntry(entry)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <ModalComponent v-model="showDialog" title="新增白名单">
      <div class="form-group">
        <label>类别</label>
        <select v-model="form.type" class="form-input">
          <option value="FILE">文件路径</option>
          <option value="SHELL">Shell 命令</option>
          <option value="HTTP">HTTP 域名</option>
        </select>
      </div>
      <div class="form-group">
        <label>规则</label>
        <input v-model="form.value" class="form-input" :placeholder="placeholder" />
        <p class="form-hint">{{ hint }}</p>
      </div>
      <div v-if="dialogError" class="error-msg">{{ dialogError }}</div>
      <div class="dialog-actions">
        <button class="btn" @click="closeDialog">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="saveEntry">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </ModalComponent>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import ModalComponent from '../components/ModalComponent.vue'

const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'FILE', label: '文件路径' },
  { key: 'SHELL', label: 'Shell 命令' },
  { key: 'HTTP', label: 'HTTP 域名' }
]

const typeMeta = {
  FILE: {
    label: '文件',
    placeholder: '如 /data/workspace 或 .ibios',
    hint: '目录前缀匹配，该目录下的子路径自动放行'
  },
  SHELL: {
    label: 'Shell',
    placeholder: '如 python3',
    hint: '按命令首个 token 匹配，参数不受限制'
  },
  HTTP: {
    label: 'HTTP',
    placeholder: '如 api.openweathermap.org 或 *.example.com',
    hint: '支持精确域名与 *. 通配子域名'
  }
}

const entries = ref([])
const activeType = ref('ALL')
const loading = ref(true)
const errorMessage = ref(null)
const showDialog = ref(false)
const saving = ref(false)
const dialogError = ref(null)
const form = ref({ type: 'FILE', value: '' })

const filteredEntries = computed(() =>
  activeType.value === 'ALL'
    ? entries.value
    : entries.value.filter((entry) => entry.type === activeType.value)
)

const placeholder = computed(() => typeMeta[form.value.type].placeholder)
const hint = computed(() => typeMeta[form.value.type].hint)

function typeLabel(type) {
  return typeMeta[type]?.label || type
}

function countOf(type) {
  return type === 'ALL'
    ? entries.value.length
    : entries.value.filter((entry) => entry.type === type).length
}

async function fetchEntries() {
  try {
    const res = await fetch('/api/v1/sandbox/whitelist')
    if (res.ok) {
      const data = await res.json()
      entries.value = data.data || []
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { type: activeType.value === 'ALL' ? 'FILE' : activeType.value, value: '' }
  dialogError.value = null
  errorMessage.value = null
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
  dialogError.value = null
}

async function saveEntry() {
  saving.value = true
  dialogError.value = null
  try {
    const res = await fetch('/api/v1/sandbox/whitelist', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      showDialog.value = false
      await fetchEntries()
    } else {
      dialogError.value = data.message || `保存失败（${res.status}）`
    }
  } catch (e) {
    dialogError.value = '保存失败，请检查网络'
  } finally {
    saving.value = false
  }
}

async function removeEntry(entry) {
  if (!window.confirm(`确认删除规则 ${entry.value}？`)) {
    return
  }
  errorMessage.value = null
  try {
    const res = await fetch(`/api/v1/sandbox/whitelist/${entry.id}`, { method: 'DELETE' })
    if (res.ok) {
      await fetchEntries()
    } else {
      const data = await res.json().catch(() => ({}))
      errorMessage.value = data.message || `删除失败（${res.status}）`
    }
  } catch (e) {
    errorMessage.value = '删除失败，请检查网络'
  }
}

onMounted(fetchEntries)
</script>

<style scoped>
.sandbox {
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

.entry-count {
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

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.8125rem;
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover {
  border-color: var(--accent);
  color: var(--text-primary);
}

.tab.active {
  background: var(--accent);
  border-color: var(--accent);
  color: white;
}

.tab-count {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  opacity: 0.8;
}

.loading,
.empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}

.empty-hint {
  font-size: 0.875rem;
  margin-top: 8px;
}

.card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead th {
  text-align: left;
  padding: 12px 16px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border);
}

tbody td {
  padding: 12px 16px;
  font-size: 0.875rem;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border);
}

tbody tr:last-child td {
  border-bottom: none;
}

.mono {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8125rem;
  word-break: break-all;
}

.col-type {
  width: 120px;
}

.col-action {
  width: 100px;
  text-align: right;
}

.type-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-family: 'JetBrains Mono', monospace;
}

.type-file {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.12);
}

.type-shell {
  color: #34d399;
  background: rgba(52, 211, 153, 0.12);
}

.type-http {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.12);
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

.form-hint {
  margin: 6px 0 0;
  font-size: 0.75rem;
  color: var(--text-muted);
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
