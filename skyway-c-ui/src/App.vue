<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}
#app { min-height: 100vh; }

.c-toast-container {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}
.c-toast {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  opacity: 0;
  transform: translateY(-8px);
  transition: opacity 0.2s, transform 0.2s;
}
.c-toast.show {
  opacity: 1;
  transform: translateY(0);
}
.c-toast-success { background: #10b981; color: #fff; }
.c-toast-error { background: #ef4444; color: #fff; }
.c-toast-warning { background: #f59e0b; color: #fff; }
</style>
