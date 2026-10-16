import { createRouter, createWebHistory } from 'vue-router'
import OverviewView from '../views/OverviewView.vue'
import AgentsView from '../views/AgentsView.vue'
import ProvidersView from '../views/ProvidersView.vue'
import ToolsView from '../views/ToolsView.vue'
import SandboxView from '../views/SandboxView.vue'
import MemoryView from '../views/MemoryView.vue'
import InfoView from '../views/InfoView.vue'
import SessionsView from '../views/SessionsView.vue'
import SchedulesView from '../views/SchedulesView.vue'
import SessionDetailView from '../views/SessionDetailView.vue'

const routes = [
  { path: '/', component: OverviewView },
  { path: '/agents', component: AgentsView },
  { path: '/providers', component: ProvidersView },
  { path: '/tools', component: ToolsView },
  { path: '/sandbox', component: SandboxView },
  { path: '/memory', component: MemoryView },
  { path: '/schedules', component: SchedulesView },
  { path: '/info', component: InfoView },
  { path: '/sessions', component: SessionsView },
  { path: '/sessions/:id', component: SessionDetailView }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
})

export default router
