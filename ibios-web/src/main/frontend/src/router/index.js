import { createRouter, createWebHistory } from 'vue-router'
import OverviewView from '../views/OverviewView.vue'
import AgentsView from '../views/AgentsView.vue'
import AgentDetailView from '../views/AgentDetailView.vue'
import ProvidersView from '../views/ProvidersView.vue'
import ToolsView from '../views/ToolsView.vue'
import SandboxView from '../views/SandboxView.vue'
import NotifyChannelsView from '../views/NotifyChannelsView.vue'
import InfoView from '../views/InfoView.vue'
import SessionsView from '../views/SessionsView.vue'
import SchedulesView from '../views/SchedulesView.vue'
import SkillsView from '../views/SkillsView.vue'
import KnowledgeView from '../views/KnowledgeView.vue'
import SessionDetailView from '../views/SessionDetailView.vue'

const routes = [
  { path: '/', component: OverviewView },
  { path: '/agents', component: AgentsView },
  { path: '/agents/:name', component: AgentDetailView },
  { path: '/providers', component: ProvidersView },
  { path: '/tools', component: ToolsView },
  { path: '/sandbox', component: SandboxView },
  { path: '/notify-channels', component: NotifyChannelsView },
  { path: '/schedules', component: SchedulesView },
  { path: '/skills', component: SkillsView },
  { path: '/knowledge', component: KnowledgeView },
  { path: '/info', component: InfoView },
  { path: '/sessions', component: SessionsView },
  { path: '/sessions/:id', component: SessionDetailView }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
})

export default router
