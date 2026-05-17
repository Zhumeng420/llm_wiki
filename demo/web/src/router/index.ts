import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '总览', icon: 'Odometer' } },
  { path: '/sources', name: 'sources', component: () => import('@/views/Sources.vue'), meta: { title: '数据源', icon: 'Folder' } },
  { path: '/wiki', name: 'wiki', component: () => import('@/views/Wiki.vue'), meta: { title: 'Wiki 页面', icon: 'Document' } },
  { path: '/graph', name: 'graph', component: () => import('@/views/Graph.vue'), meta: { title: '知识图谱', icon: 'Share' } },
  { path: '/search', name: 'search', component: () => import('@/views/Search.vue'), meta: { title: '智能检索', icon: 'Search' } },
  { path: '/insights', name: 'insights', component: () => import('@/views/Insights.vue'), meta: { title: '空白反推', icon: 'MagicStick' } },
  { path: '/schedule', name: 'schedule', component: () => import('@/views/Schedule.vue'), meta: { title: '定时更新', icon: 'AlarmClock' } },
  { path: '/eval', name: 'eval', component: () => import('@/views/Eval.vue'), meta: { title: '评测体系', icon: 'DataAnalysis' } },
  { path: '/settings', name: 'settings', component: () => import('@/views/Settings.vue'), meta: { title: '系统设置', icon: 'Setting' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
