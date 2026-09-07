<template>
  <router-view></router-view>
</template>

<style>
/* 全局重置：消除 body 默认 margin，让全屏容器真正全屏 */
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: 100vh;
  box-sizing: border-box;
}
*, *::before, *::after {
  box-sizing: inherit;
}
</style>