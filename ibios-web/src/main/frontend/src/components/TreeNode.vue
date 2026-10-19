<template>
  <div class="tree-node">
    <div
      v-if="node.type === 'directory'"
      class="tree-dir"
      @click="expanded = !expanded"
    >
      <span class="tree-icon">{{ expanded ? '📂' : '' }}</span>
      <span class="tree-label">{{ node.name }}</span>
    </div>

    <div v-if="node.type === 'file'" class="tree-file" :class="{ active: selectedPath === node.path }" @click="$emit('select', node.path)">
      <span class="tree-icon">📄</span>
      <span class="tree-label mono">{{ node.name }}</span>
    </div>

    <div v-if="node.type === 'directory' && expanded && node.children" class="tree-children">
      <tree-node
        v-for="child in node.children"
        :key="child.path"
        :node="child"
        :selected-path="selectedPath"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  selectedPath: { type: String, default: null }
})

defineEmits(['select'])

const expanded = ref(true)
</script>

<style scoped>
.tree-node {
  margin-bottom: 1px;
}

.tree-dir {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  transition: background 0.15s;
}

.tree-dir:hover {
  background: var(--bg-hover);
}

.tree-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 20px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  transition: background 0.15s;
}

.tree-file:hover {
  background: var(--bg-hover);
}

.tree-file.active {
  background: rgba(249, 115, 22, 0.15);
  color: var(--accent);
}

.tree-children {
  padding-left: 16px;
}

.tree-icon {
  font-size: 14px;
  flex-shrink: 0;
  width: 18px;
  text-align: center;
}

.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}

.mono {
  font-family: 'JetBrains Mono', monospace;
}
</style>
