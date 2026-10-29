<template>
  <div class="agent-detail">
    <header class="detail-header">
      <router-link to="/agents" class="back-link">&larr; 返回 Agent 列表</router-link>
      <h1 class="page-title">Agent 详情：{{ agentName }}</h1>
    </header>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab-btn"
        :class="{ active: activeTab === tab.id }"
        @click="switchTab(tab.id)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="tab-content">
      <!-- 基本信息 Tab -->
      <div v-if="activeTab === 'info'" class="info-panel card">
        <div v-if="agentLoading" class="loading">加载中...</div>
        <div v-else-if="agentInfo" class="info-grid">
          <div class="info-item">
            <span class="info-label">名称</span>
            <span class="info-value mono">{{ agentInfo.name }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">描述</span>
            <span class="info-value">{{ agentInfo.description || '–' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Provider</span>
            <span class="info-value mono">{{ agentInfo.provider }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Model</span>
            <span class="info-value mono">{{ agentInfo.model }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">定时任务</span>
            <span class="info-value mono">
              <template v-if="agentInfo.schedules && agentInfo.schedules.length">
                <div v-for="s in agentInfo.schedules" :key="s.id" class="schedule-item">
                  {{ s.cron }} ({{ s.zone }})
                </div>
              </template>
              <span v-else>–</span>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">目录</span>
            <span class="info-value mono">{{ agentInfo.agentDir || '–' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Scripts</span>
            <span class="info-value">{{ agentInfo.hasScripts ? '有' : '无' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Skills</span>
            <span class="info-value">{{ agentInfo.hasSkills ? '有' : '无' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Reference</span>
            <span class="info-value">{{ agentInfo.hasReference ? '有' : '无' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Memory</span>
            <span class="info-value">{{ agentInfo.hasMemory ? '有' : '无' }}</span>
          </div>
        </div>
      </div>

      <!-- 文件树 Tab -->
      <div v-if="activeTab === 'files'" class="files-panel">
        <div class="workspace-layout">
          <div class="file-tree-panel card">
            <div v-if="treeLoading" class="loading">加载中...</div>
            <div v-else-if="treeRoot" class="tree">
              <tree-node
                :node="treeRoot"
                :selected-path="selectedPath"
                @select="selectFile"
              />
            </div>
          </div>

          <div class="file-viewer-panel">
            <div v-if="!selectedPath" class="file-empty">
              <span class="hint">选择一个文件查看内容</span>
            </div>
            <template v-else>
              <div class="file-header">
                <div class="file-breadcrumb mono">{{ selectedPath }}</div>
                <button class="btn-edit-file" @click="openFileEditModal">编辑</button>
              </div>
              <div class="file-content-wrapper card">
                <pre v-if="fileContent !== null" class="file-content mono">{{ fileContent }}</pre>
                <div v-else-if="fileLoading" class="loading">加载中...</div>
                <div v-else class="error">加载失败</div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 编辑 Agent Tab -->
      <div v-if="activeTab === 'edit'" class="edit-panel card">
        <div class="edit-section">
          <h3 class="section-title">用自然语言重新定义 Agent</h3>
          <p class="section-desc">
            描述你希望这个 Agent 做什么，大模型会按照 AGENT.md 格式规范生成内容。
          </p>
          <button class="btn-generate" @click="showEditModal = true">打开编辑器</button>
        </div>
      </div>

      <!-- 编辑 Agent Modal -->
      <ModalComponent v-model="showEditModal" title="编辑 Agent" width="640px">
        <div class="edit-section">
          <textarea
            v-model="editDescription"
            class="edit-textarea"
            placeholder="例如：这是一个每日科技日报 Agent，每天早上 8 点自动抓取 Hacker News、TechCrunch 等来源的最新科技新闻，整理成摘要并写入文件..."
            rows="6"
          ></textarea>
          <div v-if="editError" class="error-msg">{{ editError }}</div>
          <div class="edit-actions">
            <button class="btn-generate" :disabled="generating" @click="generateAgent">
              {{ generating ? '生成中...' : '生成 AGENT.md' }}
            </button>
          </div>
        </div>
        <div v-if="generatedContent" class="preview-section">
          <h3 class="section-title">生成预览</h3>
          <div class="preview-content-wrapper card">
            <pre class="preview-content mono">{{ generatedContent }}</pre>
          </div>
          <div class="preview-actions">
            <button class="btn-cancel" @click="generatedContent = null">放弃</button>
            <button class="btn-save" :disabled="applying" @click="applyGenerated">
              {{ applying ? '应用中...' : '应用并保存' }}
            </button>
          </div>
        </div>
      </ModalComponent>

      <!-- 记忆 Tab -->
      <div v-if="activeTab === 'memory'" class="memory-panel card">
        <div class="memory-header">
          <h3 class="section-title">Agent 专属记忆</h3>
          <p class="section-desc">
            此记忆仅属于 {{ agentName }}，其他 Agent 不可见。
          </p>
          <button class="btn-generate" @click="openMemoryModal">编辑记忆</button>
        </div>
        <div v-if="memoryLoading" class="loading">加载中...</div>
        <template v-else>
          <pre class="memory-preview mono">{{ memoryContent || '（暂无记忆内容）' }}</pre>
        </template>
      </div>

      <!-- Session 对话 Tab -->
      <div v-if="activeTab === 'sessions'" class="sessions-panel card">
        <div v-if="sessionsLoading" class="loading">加载中...</div>
        <div v-else-if="sessions.length === 0" class="empty-state">
          <span class="hint">该 Agent 暂无会话记录</span>
        </div>
        <div v-else class="sessions-list">
          <div v-for="session in sessions" :key="session.sessionId" class="session-item">
            <div class="session-header">
              <span class="session-id mono">{{ session.sessionId }}</span>
              <span class="session-profile mono">{{ session.profileName }}</span>
            </div>
            <div class="session-messages">
              <div
                v-for="(msg, idx) in session.messages"
                :key="idx"
                class="message"
                :class="'role-' + msg.role"
              >
                <span class="message-role">{{ msg.role }}</span>
                <span class="message-content">{{ msg.content }}</span>
                <span v-if="msg.toolName" class="message-tool mono">[{{ msg.toolName }}]</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 文件编辑 Modal -->
    <ModalComponent v-model="showFileEditModal" :title="'编辑文件：' + (selectedPath || '')" width="720px">
      <textarea
        v-model="editContent"
        class="file-editor mono"
        placeholder="编辑文件内容..."
        rows="20"
      ></textarea>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showFileEditModal = false">取消</button>
        <button class="btn-save" :disabled="saving" @click="saveFile">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </ModalComponent>

    <!-- 记忆编辑 Modal -->
    <ModalComponent v-model="showMemoryModal" title="编辑 Agent 记忆" width="640px">
      <p class="section-desc">
        此记忆仅属于 {{ agentName }}，其他 Agent 不可见。
      </p>
      <div v-if="memoryLoading" class="loading">加载中...</div>
      <template v-else>
        <textarea
          v-model="memoryContent"
          class="memory-editor mono"
          placeholder="在此编辑 Agent 的专属记忆内容..."
          rows="16"
        ></textarea>
        <div v-if="memoryError" class="error-msg">{{ memoryError }}</div>
        <div v-if="memorySaved" class="success-msg">记忆已保存</div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showMemoryModal = false">取消</button>
          <button class="btn-save" :disabled="savingMemory" @click="saveMemory">
            {{ savingMemory ? '保存中...' : '保存记忆' }}
          </button>
        </div>
      </template>
    </ModalComponent>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TreeNode from '../components/TreeNode.vue'
import ModalComponent from '../components/ModalComponent.vue'

const route = useRoute()
const agentName = ref(route.params.name)
const activeTab = ref('info')

const tabs = [
  { id: 'info', label: '基本信息' },
  { id: 'files', label: '文件树' },
  { id: 'edit', label: '编辑 Agent' },
  { id: 'memory', label: '记忆' },
  { id: 'sessions', label: 'Session 对话' }
]

// Agent info
const agentInfo = ref(null)
const agentLoading = ref(true)

// File tree
const treeRoot = ref(null)
const treeLoading = ref(true)
const selectedPath = ref(null)
const fileContent = ref(null)
const fileLoading = ref(false)
const editing = ref(false)
const editContent = ref('')
const saving = ref(false)
const showFileEditModal = ref(false)

// Edit Agent
const editDescription = ref('')
const generating = ref(false)
const editError = ref(null)
const generatedContent = ref(null)
const applying = ref(false)
const showEditModal = ref(false)

// Memory
const memoryContent = ref('')
const memoryLoading = ref(true)
const savingMemory = ref(false)
const memoryError = ref(null)
const memorySaved = ref(false)
const showMemoryModal = ref(false)

// Sessions
const sessions = ref([])
const sessionsLoading = ref(true)

async function loadAgentInfo() {
  agentLoading.value = true
  try {
    const res = await fetch(`/api/v1/agents/${agentName.value}`)
    if (res.ok) {
      const data = await res.json()
      agentInfo.value = data.data
    }
  } catch (e) {
    // silent
  } finally {
    agentLoading.value = false
  }
}

async function loadTree() {
  treeLoading.value = true
  try {
    const res = await fetch(`/api/v1/workspace/tree?path=agents/${agentName.value}`)
    if (res.ok) {
      const data = await res.json()
      treeRoot.value = data.data
      if (data.data && data.data.children) {
        const agentMd = data.data.children.find(c => c.name === 'AGENT.md')
        if (agentMd) {
          selectFile(agentMd.path)
        }
      }
    }
  } catch (e) {
    // silent
  } finally {
    treeLoading.value = false
  }
}

function selectFile(path) {
  selectedPath.value = path
  fileContent.value = null
  editing.value = false
  editContent.value = ''
  loadFile(path)
}

async function loadFile(path) {
  fileLoading.value = true
  try {
    const res = await fetch(`/api/v1/workspace/file?path=${encodeURIComponent(path)}`)
    if (res.ok) {
      const data = await res.json()
      fileContent.value = data.data.content
    } else {
      fileContent.value = null
    }
  } catch (e) {
    fileContent.value = null
  } finally {
    fileLoading.value = false
  }
}

function openFileEditModal() {
  editContent.value = fileContent.value || ''
  showFileEditModal.value = true
}

function openMemoryModal() {
  showMemoryModal.value = true
}

async function saveFile() {
  saving.value = true
  try {
    const res = await fetch('/api/v1/workspace/file', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path: selectedPath.value, content: editContent.value })
    })
    if (res.ok) {
      fileContent.value = editContent.value
      showFileEditModal.value = false
      editContent.value = ''
      await loadTree()
    }
  } catch (e) {
    // silent
  } finally {
    saving.value = false
  }
}

async function generateAgent() {
  if (!editDescription.value.trim()) {
    editError.value = '请填写描述'
    return
  }
  generating.value = true
  editError.value = null
  generatedContent.value = null
  try {
    const res = await fetch(`/api/v1/agents/${agentName.value}/regenerate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ description: editDescription.value })
    })
    const data = await res.json()
    if (!res.ok) {
      editError.value = data.message || '生成失败'
      return
    }
    generatedContent.value = data.data['AGENT.md'] || ''
  } catch (e) {
    editError.value = e.message
  } finally {
    generating.value = false
  }
}

async function applyGenerated() {
  if (!generatedContent.value) return
  applying.value = true
  try {
    const res = await fetch(`/api/v1/agents/${agentName.value}/files`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ files: { 'AGENT.md': generatedContent.value } })
    })
    if (res.ok) {
      generatedContent.value = null
      editDescription.value = ''
      showEditModal.value = false
      await loadAgentInfo()
      await loadTree()
      activeTab.value = 'files'
    }
  } catch (e) {
    // silent
  } finally {
    applying.value = false
  }
}

async function loadMemory() {
  memoryLoading.value = true
  memoryError.value = null
  memorySaved.value = false
  try {
    const res = await fetch(`/api/v1/agents/${agentName.value}/memory`)
    if (res.ok) {
      const data = await res.json()
      memoryContent.value = data.data.content || ''
    }
  } catch (e) {
    memoryError.value = e.message
  } finally {
    memoryLoading.value = false
  }
}

async function saveMemory() {
  savingMemory.value = true
  memoryError.value = null
  memorySaved.value = false
  try {
    const res = await fetch(`/api/v1/agents/${agentName.value}/memory`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: memoryContent.value })
    })
    if (res.ok) {
      memorySaved.value = true
      setTimeout(() => {
        memorySaved.value = false
        showMemoryModal.value = false
      }, 1000)
    } else {
      const data = await res.json()
      memoryError.value = data.message || '保存失败'
    }
  } catch (e) {
    memoryError.value = e.message
  } finally {
    savingMemory.value = false
  }
}

async function loadSessions() {
  sessionsLoading.value = true
  try {
    const res = await fetch('/api/v1/sessions')
    if (res.ok) {
      const data = await res.json()
      const allSessions = data.data || []
      sessions.value = allSessions.filter(s => s.profileName === agentName.value)
    }
  } catch (e) {
    // silent
  } finally {
    sessionsLoading.value = false
  }
}

function switchTab(tabId) {
  activeTab.value = tabId
  if (tabId === 'files' && !treeRoot.value) {
    loadTree()
  }
  if (tabId === 'memory' && !memoryContent.value && !memoryLoading.value) {
    loadMemory()
  }
  if (tabId === 'sessions' && sessions.value.length === 0) {
    loadSessions()
  }
}

onMounted(() => {
  loadAgentInfo()
  loadTree()
  loadMemory()
})
</script>

<style scoped>
.agent-detail {
  max-width: 1200px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.back-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  transition: all 0.2s;
}

.back-link:hover {
  color: var(--text-primary);
  border-color: var(--accent);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.tab-btn {
  padding: 10px 20px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: var(--text-primary);
}

.tab-btn.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}

.tab-content {
  min-height: 400px;
}

/* Info Panel */
.info-panel {
  padding: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
}

.schedule-item {
  margin: 2px 0;
}

/* Files Panel */
.workspace-layout {
  display: flex;
  gap: 20px;
  min-height: 500px;
}

.file-tree-panel {
  width: 280px;
  min-width: 220px;
  padding: 16px;
  flex-shrink: 0;
  overflow-y: auto;
  max-height: 600px;
}

.file-viewer-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.file-breadcrumb {
  font-size: 14px;
  color: var(--text-secondary);
}

.btn-edit-file {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid var(--accent);
  border-radius: 4px;
  color: var(--accent);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit-file:hover {
  background: var(--accent);
  color: white;
}

.file-content-wrapper {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.file-content {
  padding: 20px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary);
  margin: 0;
  max-height: 600px;
  overflow-y: auto;
}

.file-editor {
  width: 100%;
  min-height: 400px;
  padding: 20px;
  background: var(--bg-primary);
  border: none;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  font-family: 'JetBrains Mono', monospace;
}

.file-editor:focus {
  outline: none;
}

.editor-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: flex-end;
}

.file-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Edit Agent Panel */
.edit-panel {
  padding: 24px;
}

.edit-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 16px 0;
}

.edit-textarea {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.edit-textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.edit-textarea::placeholder {
  color: var(--text-muted);
}

.edit-actions {
  margin-top: 16px;
}

.btn-generate {
  padding: 10px 20px;
  background: var(--accent);
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-generate:hover {
  opacity: 0.9;
}

.btn-generate:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.preview-section {
  border-top: 1px solid var(--border);
  padding-top: 24px;
}

.preview-content-wrapper {
  margin: 16px 0;
}

.preview-content {
  padding: 20px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary);
  margin: 0;
  max-height: 500px;
  overflow-y: auto;
}

.preview-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* Memory Panel */
.memory-panel {
  padding: 24px;
}

.memory-header {
  margin-bottom: 16px;
}

.memory-editor {
  width: 100%;
  min-height: 400px;
  padding: 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  font-family: 'JetBrains Mono', monospace;
}

.memory-editor:focus {
  outline: none;
  border-color: var(--accent);
}

.memory-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.memory-preview {
  padding: 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 200px;
  max-height: 500px;
  overflow-y: auto;
  margin-top: 16px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

/* Sessions Panel */
.sessions-panel {
  padding: 20px;
  /* 155px = 内容区上边距 32 + 页头 + 页签，留 35px 呼吸；会话多时面板内部滚动，页面不跟着变长 */
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.session-item {
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 16px;
}

.session-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.session-id {
  font-size: 12px;
  color: var(--text-muted);
}

.session-profile {
  font-size: 12px;
  color: var(--accent);
}

.session-messages {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 13px;
}

.message-role {
  font-weight: 600;
  min-width: 60px;
  color: var(--text-secondary);
}

.role-user .message-role {
  color: var(--accent);
}

.role-assistant .message-role {
  color: var(--text-primary);
}

.role-tool .message-role {
  color: var(--text-muted);
}

.message-content {
  flex: 1;
  color: var(--text-primary);
  line-height: 1.5;
}

.message-tool {
  font-size: 11px;
  color: var(--text-muted);
}

.empty-state {
  text-align: center;
  padding: 40px;
}

.hint {
  color: var(--text-muted);
  font-size: 14px;
}

.loading, .error {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}

.error {
  color: var(--error);
}

.error-msg {
  color: var(--error);
  font-size: 14px;
  margin: 12px 0;
}

.success-msg {
  color: #22c55e;
  font-size: 14px;
  margin: 12px 0;
}

.btn-cancel {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  border-color: var(--text-primary);
  color: var(--text-primary);
}

.btn-save {
  padding: 8px 16px;
  background: var(--accent);
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save:hover {
  opacity: 0.9;
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mono {
  font-family: 'JetBrains Mono', monospace;
}

.card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 6px;
}
</style>
