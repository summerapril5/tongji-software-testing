import { createRouter, createWebHistory } from 'vue-router'
import HomeworkPanel from './components/HomeworkPanel.vue'
import DetailTable from './components/DetailTable.vue'
import UnitTestPage from './components/UnitTestPage.vue'
import IntegrationTestPage from './components/IntegrationTestPage.vue'
import SystemTestPage from './components/SystemTestPage.vue'
import CDiagramPanel from './components/CDiagramPanel.vue' // 构建C语言程序图
import homeworkList from './data/homework-meta'

const exerciseIds = [
  'triangle-judge',
  'calendar',
  'computer-sale',
  'telecom-fee',
  'sales-system'
]

// 路由列表
const routes = [
  // 课程练习：共用 HomeworkPanel
  ...exerciseIds.map(id => ({
    path: '/' + id,
    name: id,
    component: HomeworkPanel
  })),
  // 构建C语言程序图单独页面
  { path: '/c-diagram', name: 'c-diagram', component: CDiagramPanel },

  // 期末项目：单独页面
  { path: '/project/unit-test', name: 'unit-test', component: UnitTestPage },
  { path: '/project/integration-test', name: 'integration-test', component: IntegrationTestPage },
  { path: '/project/system-test', name: 'system-test', component: SystemTestPage },

  // 详情页（如需保留）
  { path: '/detail', name: 'detail', component: DetailTable },

  // 默认重定向到第一个练习
  { path: '/', redirect: '/' + exerciseIds[0] }


]

export default createRouter({
  history: createWebHistory(),
  routes
})
