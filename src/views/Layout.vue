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

          <!-- 已存档修改复核（仅经理） -->
          <el-menu-item index="/archive/modify-review" v-if="role === 'MANAGER'">
            已存档修改复核
          </el-menu-item>
        </el-submenu>

        <!-- 薪酬管理 -->
        <el-submenu index="salary" v-if="role === 'SPECIALIST' || role === 'MANAGER'">
          <template #title>
            <i class="el-icon-s-finance"></i>
            <span>薪酬</span>
          </template>

          <!-- 薪酬标准 对 专员/经理 可见 -->
          <el-menu-item index="/salary/standard">薪酬标准</el-menu-item>

          <!-- 专员：薪酬发放单（登记） -->
          <el-menu-item index="/salary/payrun" v-if="role === 'SPECIALIST'">薪酬发放单</el-menu-item>

          <!-- 经理：薪酬发放复核（使用 pushreview 路径） -->
          <el-menu-item index="/salary/pushreview" v-if="role === 'MANAGER'">薪酬发放复核</el-menu-item>

          <!-- 薪酬发放查询（所有登录用户可见，按需显示） -->
          <el-menu-item index="/salary/query">薪酬发放查询</el-menu-item>
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
          <!-- 管理员关于薪酬标准的复核（保持 /salary/review） -->
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
          <span class="user">当前用户：{{ username }} <span v-if="role">（角色：{{ role }})</span></span>
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
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 响应式用户名与角色（角色统一大写以避免大小写问题）
const username = ref((localStorage.getItem('username') || '未登录').toString())
const role = ref((localStorage.getItem('role') || '').toString().trim().toUpperCase())

// 监听 storage 变化，保持多标签/切换时 UI 同步
function onStorageEvent(e) {
  if (!e) return
  if (e.key === 'username') {
    username.value = (e.newValue || '未登录').toString()
  }
  if (e.key === 'role') {
    role.value = (e.newValue || '').toString().trim().toUpperCase()
  }
}
window.addEventListener && window.addEventListener('storage', onStorageEvent)

// active menu: map route paths to menu index keys
const activeMenu = computed(() => {
  const p = route.path || '/'
  // 档案
  if (p.startsWith('/archive/detail') || p.startsWith('/archive/edit') || p.startsWith('/archive/modify-review')) return '/archive/query'
  // 薪酬标准
  if (p.startsWith('/salary/standard')) return '/salary/standard'
  // 薪酬发放登记（专员）
  if (p.startsWith('/salary/payrun')) return '/salary/payrun'
  // 薪酬标准复核（管理员）
  if (p.startsWith('/salary/review')) return '/salary/review'
  // 薪酬发放复核（经理）
  if (p.startsWith('/salary/pushreview')) return '/salary/pushreview'
  // 薪酬查询
  if (p.startsWith('/salary/query')) return '/salary/query'
  // 管理相关
  if (p.startsWith('/sys/salary')) return '/sys/salary'
  if (p.startsWith('/sys/org')) return '/sys/org'
  if (p.startsWith('/sys/position')) return '/sys/position'
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
  // 更新 reactive 变量，确保 UI 即时更新
  username.value = '未登录'
  role.value = ''
  router.push({ name: 'Login' })
}

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

onBeforeUnmount(() => {
  window.removeEventListener && window.removeEventListener('storage', onStorageEvent)
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