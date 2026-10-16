<template>
  <div class="app">
    <Sidebar />
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
</script>

<style scoped>
.app {
  display: flex;
  min-height: 100vh;
}

.content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}
</style>
