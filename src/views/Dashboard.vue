<template>
  <div>
    <h2>欢迎使用人力资源管理系统</h2>
    <el-tag size="large">{{ roleText }}</el-tag>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const roleText = computed(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return ''
  const user = JSON.parse(userStr)
  return user.role === 'SPECIALIST' ? '当前身份：人事专员' : '当前身份：人事经理'
})
</script>