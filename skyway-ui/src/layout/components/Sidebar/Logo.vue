<template>
  <div class="sidebar-logo-container" :class="{ 'collapse': collapse }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <span class="sidebar-logo-icon" aria-hidden="true">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="20" cy="8" r="3.5" fill="currentColor"/>
            <circle cx="8" cy="24" r="3.5" fill="currentColor"/>
            <circle cx="32" cy="24" r="3.5" fill="currentColor"/>
            <circle cx="20" cy="34" r="3.5" fill="currentColor"/>
            <path d="M20 11.5v5M20 29v4M20 16.5l-9 4.5M20 16.5l9 4.5M8 24l11.5-5.5M32 24l-11.5-5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </span>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <span class="sidebar-logo-icon" aria-hidden="true">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="20" cy="8" r="3.5" fill="currentColor"/>
            <circle cx="8" cy="24" r="3.5" fill="currentColor"/>
            <circle cx="32" cy="24" r="3.5" fill="currentColor"/>
            <circle cx="20" cy="34" r="3.5" fill="currentColor"/>
            <path d="M20 11.5v5M20 29v4M20 16.5l-9 4.5M20 16.5l9 4.5M8 24l11.5-5.5M32 24l-11.5-5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </span>
        <h1 class="sidebar-title">{{ title }}</h1>
      </router-link>
    </transition>
  </div>
</template>

<script setup>
import useSettingsStore from '@/store/modules/settings'
import variables from '@/assets/styles/variables.module.scss'

defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})

const title = 'NetCloud'
const settingsStore = useSettingsStore()
const sideTheme = computed(() => settingsStore.sideTheme)

// 获取Logo背景色（默认亮色，与侧边栏主题一致）
const getLogoBackground = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-bg)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightBg
  }
  return sideTheme.value === 'theme-dark' ? variables.menuBg : variables.menuLightBg
})

// 获取Logo文字颜色
const getLogoTextColor = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-text)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightText
  }
  return sideTheme.value === 'theme-dark' ? '#fff' : variables.menuLightText
})
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  height: 56px;
  background: v-bind(getLogoBackground);
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    min-width: 0;
    display: flex !important;
    flex-direction: row !important;
    flex-wrap: nowrap;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 0 12px;
    box-sizing: border-box;
    text-align: center;
    overflow: hidden;

    & .sidebar-logo-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      width: 34px;
      height: 34px;
      color: rgba(255, 255, 255, 0.95);
    }
    & .sidebar-logo-icon svg {
      width: 100%;
      height: 100%;
    }

    & .sidebar-title {
      margin: 0;
      padding: 0;
      color: rgba(255, 255, 255, 0.95);
      font-weight: 600;
      font-size: 16px;
      line-height: 1;
      font-family: system-ui, -apple-system, sans-serif;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      min-width: 0;
      flex-shrink: 1;
    }
  }

  &.collapse {
    .sidebar-logo-link {
      padding: 0;
    }
    .sidebar-title {
      display: none;
    }
  }
}
</style>