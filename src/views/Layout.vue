<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="aside">
      <div class="logo">HR System</div>
      <el-menu
          router
          :default-active="$route.path"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
      >
        <!-- 侧边栏菜单项 -->
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon> 首页
        </el-menu-item>

        <!-- 管理员可见 -->
        <el-sub-menu index="sys" v-if="role === 'ADMIN'">
          <template #title>
            <el-icon><Setting /></el-icon> 系统管理
          </template>
          <el-menu-item index="/sys/org">机构管理</el-menu-item>
          <el-menu-item index="/sys/position">职位管理</el-menu-item>
        </el-sub-menu>

        <!-- 专员可见 (对应您的 SPECIALIST 账号) -->
        <el-menu-item index="/archive/register" v-if="role === 'SPECIALIST' || role === 'ADMIN'">
          <el-icon><DocumentAdd /></el-icon> 档案登记
        </el-menu-item>

        <!-- 经理可见 -->
        <el-menu-item index="/archive/review" v-if="role === 'MANAGER'">
          <el-icon><Checked /></el-icon> 档案复核
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <span>当前用户: {{ username }}</span>
          <el-button link type="danger" @click="handleLogout" style="margin-left: 15px;">退出</el-button>
        </div>
      </el-header>

      <el-main>
        <!-- 【核心关键点】必须有这个标签，子路由才会显示！！！ -->
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Setting, DocumentAdd, Checked } from '@element-plus/icons-vue'

const router = useRouter()
const role = localStorage.getItem('role')
const username = localStorage.getItem('username')

const handleLogout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; display: flex; }
.aside { background-color: #304156; color: white; display: flex; flex-direction: column; }
.logo { height: 60px; line-height: 60px; text-align: center; font-size: 20px; font-weight: bold; background-color: #2b3649; }
.el-menu { border-right: none; }
.header { background-color: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: flex-end; }
.el-main { background-color: #f0f2f5; padding: 20px; }
</style>