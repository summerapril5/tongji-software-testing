<template>
  <el-container class="layout">
    <SidebarNav />
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>
<script setup>
import SidebarNav from './components/SidebarNav.vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './style.css'
</script>
<style>
.layout { min-height: 100vh; background: #f4f7fb;}
.main-content { padding: 32px 32px 0 32px; }
</style>
