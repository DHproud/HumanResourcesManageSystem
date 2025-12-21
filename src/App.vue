<template>
  <!-- 这里是整个应用的入口，根据 URL 显示 Login 或 Layout -->
  <router-view />
</template>

<style>
/* 全局重置样式，防止出现白色边框 */
body, html, #app {
  margin: 0;
  padding: 0;
  height: 100%;
}
</style>