<template>
  <el-container class="layout-container">
    <!-- 左侧导航 -->
    <el-aside width="220px" class="aside">
      <div class="logo">HR System</div>
      <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          router
          @select="onMenuSelect"
          :collapse="false"
      >
        <el-menu-item index="/dashboard">
          <i class="el-icon-menu"></i>
          <span>首页</span>
        </el-menu-item>

        <el-submenu index="archive" v-if="true">
          <template #title>
            <i class="el-icon-document"></i>
            <span>档案</span>
          </template>

          <el-menu-item index="/archive/query">
            档案查询
          </el-menu-item>

          <!-- 档案登记 - 仅人事专员 -->
          <el-menu-item index="/archive/register" v-if="role === 'SPECIALIST'">
            档案登记
          </el-menu-item>

          <!-- 原有复核页 - 仅经理 -->
          <el-menu-item index="/archive/review" v-if="role === 'MANAGER'">
            档案复核
          </el-menu-item>

          <!-- 新增：已有档案修改复核（仅经理） -->
          <el-menu-item index="/archive/modify-review" v-if="role === 'MANAGER'">
            已存档修改复核
          </el-menu-item>
        </el-submenu>

        <!-- 薪酬管理 - 薪酬专员/薪酬经理 可见薪酬标准页面 -->
        <el-submenu index="salary" v-if="role === 'SPECIALIST' || role === 'MANAGER'">
          <template #title>
            <i class="el-icon-s-finance"></i>
            <span>薪酬</span>
          </template>
          <el-menu-item index="/salary/standard">薪酬标准</el-menu-item>
        </el-submenu>

        <!-- 管理相关 - 仅管理员 -->
        <el-submenu index="sys" v-if="role === 'ADMIN'">
          <template #title>
            <i class="el-icon-s-tools"></i>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/sys/org">机构管理</el-menu-item>
          <el-menu-item index="/sys/position">职位管理</el-menu-item>
          <!-- 薪酬项目（管理员） -->
          <el-menu-item index="/sys/salary">薪酬项目</el-menu-item>
          <el-menu-item index="/salary/review">薪酬复核</el-menu-item>
        </el-submenu>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container style="flex:1; display:flex; flex-direction:column;">
      <el-header class="header">
        <div style="flex:1;">
          <el-breadcrumb separator="/" style="padding-left:12px;">
            <el-breadcrumb-item to="/">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div style="padding-right:16px; display:flex; align-items:center; gap:12px;">
          <span class="user">当前用户：{{ username }}</span>
          <el-button type="text" @click="logout" class="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="el-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const username = ref(localStorage.getItem('username') || '未登录')
const role = localStorage.getItem('role') || ''

// active menu: use the current route path
const activeMenu = computed(() => {
  const p = route.path || '/'
  if (p.startsWith('/archive/detail')) return '/archive/query'
  if (p.startsWith('/archive/edit')) return '/archive/query'
  if (p.startsWith('/salary/standard')) return '/salary/standard'
  if (p.startsWith('/sys/salary')) return '/sys/salary'
  return p
})

// derive title from route meta for breadcrumb / header
const currentTitle = computed(() => {
  return route.meta && route.meta.title ? route.meta.title : ''
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('username')
  router.push({ name: 'Login' })
}

watch(() => route.path, (p) => {
  // nothing required; activeMenu is computed from route
})

function onMenuSelect(index) {
  if (!index) return
  if (index === '/dashboard') {
    router.push({ path: '/dashboard' })
  } else {
    router.push({ path: index })
  }
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (!token && route.name !== 'Login') {
    router.push({ name: 'Login' })
  }
})
</script>

<style scoped>
.layout-container { height: 100vh; display: flex; }
.aside { background-color: #304156; color: white; display: flex; flex-direction: column; }
.logo { height: 60px; line-height: 60px; text-align: center; font-size: 20px; font-weight: bold; background-color: #2b3649; }
.el-menu { border-right: none; }
.header { background-color: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: flex-end; }
.el-main { background-color: #f0f2f5; padding: 20px; }

/* 覆盖 Element Plus 菜单样式以保持侧栏深色 */
.el-menu-vertical-demo { background: transparent !important; color: rgba(255,255,255,0.95); }
.el-menu-vertical-demo .el-menu-item, .el-menu-vertical-demo .el-submenu__title { color: rgba(255,255,255,0.95) !important; }
.el-menu-vertical-demo .el-menu-item.is-active, .el-menu-vertical-demo .el-menu-item:hover,
.el-menu-vertical-demo .el-submenu__title:hover, .el-menu-vertical-demo .el-submenu__title.is-active {
  background-color: rgba(255,255,255,0.04) !important; color: #fff !important;
}

.user { color: #333; margin-right: 8px; }
.logout { color: #f56c6c; }
</style>