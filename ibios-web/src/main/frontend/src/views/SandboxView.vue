<template>
  <div>
    <h1>Sandbox 白名单</h1>
    <div class="card">
      <table>
        <thead>
          <tr>
            <th>类别</th>
            <th>规则</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.category + rule.pattern">
            <td>{{ rule.category }}</td>
            <td class="mono">{{ rule.pattern }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="rules.length === 0" class="empty">
        暂无数据 · 待接入沙箱白名单端点
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const rules = ref([])
</script>
