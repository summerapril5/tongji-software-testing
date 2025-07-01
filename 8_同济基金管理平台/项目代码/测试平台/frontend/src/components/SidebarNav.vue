<template>
  <div class="sidebar">
    <div class="sidebar-title">Easy Testing</div>
    <div class="sidebar-menu">
      <div class="sidebar-label">课程练习</div>
      <router-link
        v-for="item in exerciseMenus"
        :key="item.id"
        :to="item.path"
        class="sidebar-link"
        :class="{ active: $route.path === item.path || ($route.path.startsWith('/'+item.id) && item.id !== '') }"
      >
        {{ item.title }}
      </router-link>
      <div class="sidebar-label">期末项目</div>
      <router-link
        v-for="item in projectMenus"
        :key="item.id"
        :to="item.path"
        class="sidebar-link"
        :class="{ active: $route.path === item.path }"
      >
        {{ item.title }}
      </router-link>
    </div>
  </div>
</template>

<script setup>
const exerciseMenus = [
  { id: 'triangle-judge', title: '判断三角形类型', path: '/triangle-judge' },
  { id: 'calendar', title: '万年历问题', path: '/calendar' },
  { id: 'computer-sale', title: '电脑销售系统', path: '/computer-sale' },
  { id: 'telecom-fee', title: '电信收费系统', path: '/telecom-fee' },
  { id: 'sales-system', title: '销售系统', path: '/sales-system' },
  { id: 'c-diagram', title: '构建C语言程序图', path: '/c-diagram' }
]
const projectMenus = [
  { id: 'unit-test', title: '单元测试', path: '/project/unit-test' },
  { id: 'integration-test', title: '集成测试', path: '/project/integration-test' },
  { id: 'system-test', title: '系统测试', path: '/project/system-test' }
]
</script>

<style scoped>
.sidebar {
  width: 166.75px;
  min-width: 166.75px;
  max-width: 166.75px;
  background: #fff;
  height: 100vh;
  border-right: 1.5px solid #ecf0f6;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.sidebar-title {
  font-size: 28px;
  font-weight: bold;
  color: #2980ef;
  padding: 32px 0 26px 24px;
  letter-spacing: 1.8px;
}
.sidebar-menu {
  width: 100%;
  display: flex;
  flex-direction: column;
}
.sidebar-label {
  font-size: 15px;
  color: #1976d2;
  font-weight: 700;
  margin: 20px 0 10px 28px;
  letter-spacing: 1.2px;
}
.sidebar-link {
  padding: 9px 0 9px 28px;
  font-size: 15px;
  color: #222;
  border-radius: 12px 0 0 12px;
  margin-bottom: 2px;
  margin-right: 10px;
  transition: background 0.18s, color 0.16s;
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
}

/* 高亮：当前页面项，无论 hover 始终蓝色 */
.sidebar-link.active,
.sidebar-link.router-link-active {
  background: linear-gradient(90deg, #53b9ff 0%, #2980ef 100%);
  color: #fff !important;
  font-weight: 600;
}

/* hover 仅影响未激活项 */
.sidebar-link:not(.active):hover,
.sidebar-link:not(.router-link-active):hover {
  background: #f3f6fb;
  color: #1976d2;
}
</style>
