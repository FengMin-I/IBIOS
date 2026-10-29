<template>
  <div class="providers">
    <header class="page-header">
      <h1 class="title">Provider</h1>
      <span class="provider-count">{{ providers.length }} 个 Provider</span>
      <button class="btn btn-primary create-btn" @click="openCreate">+ 新建 Provider</button>
    </header>

    <p class="hint">
      Agent 的 AGENT.md 里 <code>provider.name</code> 引用这里的名称。改完即时生效，无需重启；
      被 Agent 引用的 Provider 不能删除。
    </p>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="providers.length === 0" class="empty">
      <p>暂无 Provider</p>
      <p class="empty-hint">点上面「+ 新建 Provider」，或检查 application.yaml 的 ibios.providers 是否已播种</p>
    </div>

    <table v-else class="provider-table">
      <thead>
        <tr>
          <th>name</th>
          <th>type</th>
          <th>model</th>
          <th>base-url</th>
          <th>api-key</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="provider in providers" :key="provider.id">
          <td class="mono name-cell">{{ provider.name }}</td>
          <td><span class="type-badge">{{ provider.type }}</span></td>
          <td class="mono">{{ provider.model || '—' }}</td>
          <td class="mono base-url-cell">{{ provider.baseUrl || '—' }}</td>
          <td class="key-cell">
            <code class="mono key-value">{{ revealed.has(provider.id) ? revealed.get(provider.id) : (provider.apiKey || '未配置') }}</code>
            <button v-if="provider.apiKey" class="btn-reveal" @click="toggleReveal(provider)">
              {{ revealed.has(provider.id) ? '隐藏' : '显示' }}
            </button>
          </td>
          <td class="actions-cell">
            <button class="btn" @click="openEdit(provider)">编辑</button>
            <button class="btn btn-danger" @click="removeProvider(provider)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <ModalComponent v-model="showDialog" :title="editing ? '编辑 Provider' : '新建 Provider'">
      <div class="form-group">
        <label>名称</label>
        <input
          v-model="form.name"
          class="form-input"
          :disabled="!!editing"
          placeholder="如：deepseek、dashscope"
        />
        <p v-if="editing" class="field-hint">名称是 Agent 的引用键，创建后不可修改</p>
      </div>
      <div class="form-group">
        <label>类型</label>
        <select v-model="form.type" class="form-input">
          <option v-for="type in providerTypes" :key="type" :value="type">{{ type }}</option>
        </select>
        <p class="field-hint">dashscope 走阿里云原生协议；mock 为本地假模型；其余按 OpenAI 兼容协议</p>
      </div>
      <div class="form-group">
        <label>API Key</label>
        <input
          v-model="form.apiKey"
          class="form-input mono"
          type="password"
          autocomplete="off"
          :placeholder="editing ? '留空则保持原 key 不变' : 'sk-...'"
        />
      </div>
      <div class="form-group">
        <label>Base URL</label>
        <input
          v-model="form.baseUrl"
          class="form-input mono"
          placeholder="OpenAI 兼容必填，如 https://api.deepseek.com"
        />
      </div>
      <div class="form-group">
        <label>默认 Model</label>
        <input v-model="form.model" class="form-input mono" placeholder="如 deepseek-chat" />
      </div>
      <div v-if="dialogError" class="error-msg">{{ dialogError }}</div>
      <div class="dialog-actions">
        <button class="btn" @click="closeDialog">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="saveProvider">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </ModalComponent>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ModalComponent from '../components/ModalComponent.vue'

const providerTypes = ['dashscope', 'openai', 'mock']
const providers = ref([])
const revealed = ref(new Map())
const loading = ref(true)
const errorMessage = ref(null)
const showDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const dialogError = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return { name: '', type: 'openai', apiKey: '', baseUrl: '', model: '' }
}

async function fetchProviders() {
  loading.value = true
  try {
    const res = await fetch('/api/v1/providers')
    if (res.ok) {
      const data = await res.json()
      providers.value = data.data || []
    } else {
      const data = await res.json().catch(() => ({}))
      errorMessage.value = data.message || `加载失败（${res.status}）`
    }
  } catch (e) {
    errorMessage.value = '加载失败，请检查网络'
  } finally {
    loading.value = false
  }
}

async function toggleReveal(provider) {
  if (revealed.value.has(provider.id)) {
    revealed.value.delete(provider.id)
    return
  }
  try {
    const res = await fetch(`/api/v1/providers/${provider.id}`)
    if (res.ok) {
      const data = await res.json()
      revealed.value.set(provider.id, data.data?.apiKey || '')
    }
  } catch (e) {
    errorMessage.value = '读取详情失败，请检查网络'
  }
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  dialogError.value = null
  showDialog.value = true
}

function openEdit(provider) {
  editing.value = provider
  form.value = {
    name: provider.name,
    type: provider.type || 'openai',
    apiKey: '',
    baseUrl: provider.baseUrl || '',
    model: provider.model || ''
  }
  dialogError.value = null
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
  dialogError.value = null
}

async function saveProvider() {
  saving.value = true
  dialogError.value = null
  const isEdit = editing.value != null
  const url = isEdit ? `/api/v1/providers/${editing.value.id}` : '/api/v1/providers'
  const body = isEdit ? { ...form.value, name: undefined } : form.value
  try {
    const res = await fetch(url, {
      method: isEdit ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      showDialog.value = false
      revealed.value = new Map()
      await fetchProviders()
    } else {
      dialogError.value = data.message || `保存失败（${res.status}）`
    }
  } catch (e) {
    dialogError.value = '保存失败，请检查网络'
  } finally {
    saving.value = false
  }
}

async function removeProvider(provider) {
  if (!window.confirm(`确认删除 Provider ${provider.name}？`)) {
    return
  }
  errorMessage.value = null
  try {
    const res = await fetch(`/api/v1/providers/${provider.id}`, { method: 'DELETE' })
    if (res.ok) {
      revealed.value.delete(provider.id)
      await fetchProviders()
    } else {
      const data = await res.json().catch(() => ({}))
      errorMessage.value = data.message || `删除失败（${res.status}）`
    }
  } catch (e) {
    errorMessage.value = '删除失败，请检查网络'
  }
}

onMounted(fetchProviders)
</script>

<style scoped>
.providers {
  max-width: 1100px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.provider-count {
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

.hint {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin: 0 0 20px;
}

.hint code {
  font-family: 'JetBrains Mono', monospace;
  color: var(--text-secondary);
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

.provider-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
}

.provider-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.provider-table td {
  padding: 12px 16px;
  font-size: 0.875rem;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

.provider-table tr:last-child td {
  border-bottom: none;
}

.mono {
  font-family: 'JetBrains Mono', monospace;
}

.name-cell {
  font-weight: 600;
  color: var(--text-primary);
}

.base-url-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-badge {
  font-size: 0.75rem;
  color: var(--accent);
  padding: 2px 8px;
  background: rgba(255, 106, 0, 0.1);
  border-radius: 10px;
  font-family: 'JetBrains Mono', monospace;
}

.key-cell {
  white-space: nowrap;
}

.key-value {
  font-size: 0.8125rem;
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 3px;
}

.btn-reveal {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0 0 0 8px;
  white-space: nowrap;
}

.btn-reveal:hover {
  text-decoration: underline;
}

.actions-cell {
  white-space: nowrap;
  display: flex;
  gap: 8px;
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

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-hint {
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
