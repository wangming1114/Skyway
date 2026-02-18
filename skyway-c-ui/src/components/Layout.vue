<template>
  <div class="layout">
    <header class="header">
      <router-link to="/" class="logo">
        <span class="logo-icon" aria-hidden="true">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="20" cy="8" r="3.5" fill="currentColor"/>
            <circle cx="8" cy="24" r="3.5" fill="currentColor"/>
            <circle cx="32" cy="24" r="3.5" fill="currentColor"/>
            <circle cx="20" cy="34" r="3.5" fill="currentColor"/>
            <path d="M20 11.5v5M20 29v4M20 16.5l-9 4.5M20 16.5l9 4.5M8 24l11.5-5.5M32 24l-11.5-5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="logo-name">Skyway</span>
      </router-link>
      <nav class="main-nav">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/product" class="nav-link">产品与服务</router-link>
      </nav>
      <div class="header-right">
        <div class="user-trigger" ref="triggerRef" @click="open = !open">
          <div class="avatar" :style="userInfo.avatar ? { backgroundImage: `url(${userInfo.avatar})` } : {}">
            <span v-if="!userInfo.avatar" class="avatar-text">{{ avatarText }}</span>
          </div>
          <span class="username">{{ userInfo.username || '加载中...' }}</span>
          <svg class="arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
        </div>
        <div v-if="open" class="dropdown-panel" ref="panelRef">
          <a class="dropdown-item" href="javascript:;" @click.prevent="goProfile"><span class="item-icon">👤</span>个人中心</a>
          <a class="dropdown-item" href="javascript:;" @click.prevent="logout"><span class="item-icon">⎋</span>退出登录</a>
        </div>
      </div>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getToken, removeToken } from '@/utils/auth'
import { getUserInfo } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userInfo = ref({})
const open = ref(false)
const triggerRef = ref(null)
const panelRef = ref(null)

function loadUserInfo() {
  if (!getToken()) return
  getUserInfo()
    .then((res) => {
      userInfo.value = (res && res.data) ? res.data : {}
    })
    .catch(() => {})
}

const avatarText = computed(() => {
  const u = userInfo.value.username
  if (!u) return '?'
  return u.charAt(0).toUpperCase()
})

function goProfile() {
  open.value = false
  router.push('/profile')
}

function logout() {
  open.value = false
  removeToken()
  router.replace('/login')
}

function onDocClick(e) {
  if (open.value && triggerRef.value && panelRef.value && !triggerRef.value.contains(e.target) && !panelRef.value.contains(e.target)) {
    open.value = false
  }
}

onMounted(() => {
  loadUserInfo()
  document.addEventListener('click', onDocClick)
})
onUnmounted(() => document.removeEventListener('click', onDocClick))
watch(() => route.path, () => { loadUserInfo() })
</script>

<style scoped>
.layout { min-height: 100vh; display: flex; flex-direction: column; background: #f8fafc; }
.header {
  padding: 0 24px;
  height: 56px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #2563eb;
  text-decoration: none;
}
.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #2563eb;
}
.logo-icon svg { width: 100%; height: 100%; }
.logo-name { letter-spacing: 0.02em; }
.main-nav { display: flex; align-items: center; gap: 4px; margin-left: 32px; }
.nav-link {
  padding: 8px 12px;
  font-size: 14px;
  color: #475569;
  text-decoration: none;
  border-radius: 6px;
}
.nav-link:hover { background: #f1f5f9; color: #0f172a; }
.nav-link.router-link-active { color: #2563eb; font-weight: 500; }
.header-right { position: relative; margin-left: auto; }
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
}
.user-trigger:hover { background: #f1f5f9; }
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #2563eb;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text { font-size: 13px; font-weight: 600; color: #fff; }
.username { font-size: 14px; color: #334155; }
.arrow { width: 14px; height: 14px; color: #94a3b8; flex-shrink: 0; }
.dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  min-width: 140px;
  padding: 6px 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border: 1px solid #e2e8f0;
  z-index: 100;
}
.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 14px;
  color: #334155;
  text-decoration: none;
}
.dropdown-item:hover { background: #f8fafc; color: #2563eb; }
.item-icon { font-size: 14px; }
.main { flex: 1; }
</style>
