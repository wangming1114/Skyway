<template>
  <div class="vps-hover-card" :class="isDark ? 'vps-hover-card--dark' : 'vps-hover-card--light'">
    <!-- Header -->
    <div class="vps-hover-card__header">
      <div class="vps-hover-card__header-left">
        <div class="vps-hover-card__icon-wrap">
          <img :src="serverIcon" class="vps-hover-card__icon" alt="" />
        </div>
        <div class="vps-hover-card__title-wrap">
          <span class="vps-hover-card__name truncate">{{ displayName }}</span>
          <span v-if="row.id != null" class="vps-hover-card__id">#{{ row.id }}</span>
          <span v-if="row.categoryName" class="vps-hover-card__category truncate">{{ row.categoryName }}</span>
        </div>
      </div>
      <div class="vps-hover-card__header-right">
        <span class="vps-hover-card__status" :class="statusClass">
          <i class="vps-hover-card__status-dot" />
          {{ statusText }}
        </span>
      </div>
    </div>

    <!-- Body -->
    <div class="vps-hover-card__body">
      <div class="vps-hover-card__section">
        <div class="vps-hover-card__section-title">连接信息</div>
        <div class="vps-hover-card__grid">
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">公网 IP</span>
            <div class="vps-hover-card__value vps-hover-card__value--mono truncate">{{ row.ip || '-' }}</div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">SSH 端口</span>
            <div class="vps-hover-card__value vps-hover-card__value--mono">{{ row.sshPort != null ? row.sshPort : '-' }}</div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">SSH 账号</span>
            <div class="vps-hover-card__value vps-hover-card__value--mono truncate">{{ row.sshUsername || '-' }}</div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">SSH 密码</span>
            <div class="vps-hover-card__value vps-hover-card__value--mono vps-hover-card__pwd-wrap">
              <span class="truncate">{{ passwordVisible && row.sshPassword ? row.sshPassword : (row.sshPassword ? '******' : '-') }}</span>
              <el-icon v-if="row.sshPassword" class="vps-hover-card__pwd-toggle" @click="passwordVisible = !passwordVisible">
                <View v-if="!passwordVisible" />
                <Close v-else />
              </el-icon>
            </div>
          </div>
        </div>
      </div>
      <div class="vps-hover-card__section">
        <div class="vps-hover-card__section-title">配置与网络</div>
        <div class="vps-hover-card__grid">
          <div class="vps-hover-card__item vps-hover-card__item--full">
            <span class="vps-hover-card__label">网络类型</span>
            <div class="vps-hover-card__value">
              <template v-if="networkTags.length">
                <span v-for="(tag, i) in networkTags" :key="i" class="vps-hover-card__tag">{{ tag }}</span>
              </template>
              <span v-else>-</span>
            </div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">CPU</span>
            <div class="vps-hover-card__value truncate">{{ row.cpu || '-' }}</div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">内存</span>
            <div class="vps-hover-card__value truncate">{{ row.memory || '-' }}</div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">磁盘</span>
            <div class="vps-hover-card__value truncate">{{ row.disk || '-' }}</div>
          </div>
          <div class="vps-hover-card__item">
            <span class="vps-hover-card__label">续费金额</span>
            <div class="vps-hover-card__value truncate">{{ row.renewalAmount || '-' }}</div>
          </div>
        </div>
      </div>
      <div class="vps-hover-card__lifecycle">
        <div class="vps-hover-card__lifecycle-item">
          <span class="vps-hover-card__label">创建时间</span>
          <div class="vps-hover-card__value vps-hover-card__value--mono">{{ formatDate(row.createTime) }}</div>
        </div>
        <div class="vps-hover-card__lifecycle-item">
          <span class="vps-hover-card__label">到期时间</span>
          <div class="vps-hover-card__value vps-hover-card__value--mono" :class="{ 'vps-hover-card__value--expired': isExpired }">{{ formatDate(row.expireTime) }}</div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="vps-hover-card__footer">
      <span class="vps-hover-card__update">上次更新 {{ formatDate(row.updateTime) }}</span>
      <el-button type="primary" size="small" class="vps-hover-card__connect" @click="onConnect">
        <el-icon><Connection /></el-icon>
        一键连接
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Connection, View, Close } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/skyway'
import useSettingsStore from '@/store/modules/settings'
import serverIcon from '@/assets/images/os/server.svg'

const settingsStore = useSettingsStore()
const isDark = computed(() => settingsStore.isDark)

const props = defineProps({
  row: { type: Object, default: () => ({}) },
  statusLabel: { type: Function, default: () => '' },
  networkTypeLabel: { type: Function, default: () => '' },
  displayName: { type: String, default: '' }
})

const emit = defineEmits(['connect'])

const passwordVisible = ref(false)

const statusText = computed(() => props.statusLabel(props.row.status) || '-')
const statusClass = computed(() => {
  const s = props.row.status
  if (s === 'running') return 'vps-hover-card__status--success'
  if (s === 'stopped' || s === 'error') return 'vps-hover-card__status--danger'
  return 'vps-hover-card__status--warn'
})

const networkTags = computed(() => {
  const label = props.networkTypeLabel(props.row.networkType)
  if (!label || typeof label !== 'string') return []
  return label.split('+').map(s => s.trim()).filter(Boolean)
})

function formatDate(val) {
  if (!val) return '-'
  return parseTime(val, '{y}-{m}-{d}')
}

const isExpired = computed(() => {
  const t = props.row.expireTime
  if (!t) return false
  return new Date(t) < new Date()
})

function onConnect() {
  emit('connect', props.row)
}
</script>

<style scoped lang="scss">
.vps-hover-card {
  width: 360px;
  border-radius: 8px;
  overflow: hidden;
  font-size: 12px;
}
.vps-hover-card--dark {
  background: #1a1a1a;
  border: 1px solid #334155;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  color: #cbd5e1;
}
.vps-hover-card--light {
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.15);
  color: #334155;
}

.vps-hover-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
}
.vps-hover-card--dark .vps-hover-card__header { border-bottom: 1px solid #334155; }
.vps-hover-card--light .vps-hover-card__header { border-bottom: 1px solid #e2e8f0; }
.vps-hover-card__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.vps-hover-card__icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.vps-hover-card__icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  filter: brightness(0) invert(1);
}
.vps-hover-card__title-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  min-width: 0;
}
.vps-hover-card__name {
  font-weight: 600;
  max-width: 140px;
}
.vps-hover-card--dark .vps-hover-card__name { color: #f1f5f9; }
.vps-hover-card--light .vps-hover-card__name { color: #1e293b; }
.vps-hover-card__id {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-family: ui-monospace, monospace;
}
.vps-hover-card--dark .vps-hover-card__id { background: #334155; color: #94a3b8; }
.vps-hover-card--light .vps-hover-card__id { background: #f1f5f9; color: #64748b; }
.vps-hover-card__category {
  font-size: 11px;
  max-width: 80px;
}
.vps-hover-card--dark .vps-hover-card__category { color: #64748b; }
.vps-hover-card--light .vps-hover-card__category { color: #64748b; }
.vps-hover-card__header-right {
  flex-shrink: 0;
}
.vps-hover-card__status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
}
.vps-hover-card--dark .vps-hover-card__status { background: #334155; color: #94a3b8; }
.vps-hover-card--light .vps-hover-card__status { background: #f1f5f9; color: #64748b; }
.vps-hover-card__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.vps-hover-card__status--success .vps-hover-card__status-dot {
  animation: vps-pulse 1.5s ease-in-out infinite;
  color: #10b981;
}
.vps-hover-card__status--success { color: #10b981; }
.vps-hover-card__status--warn .vps-hover-card__status-dot { color: #f59e0b; }
.vps-hover-card__status--warn { color: #f59e0b; }
.vps-hover-card__status--danger .vps-hover-card__status-dot { color: #f43f5e; }
.vps-hover-card__status--danger { color: #f43f5e; }

@keyframes vps-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.vps-hover-card__body {
  padding: 12px 14px;
}
.vps-hover-card__section {
  margin-bottom: 12px;
}
.vps-hover-card__section:last-of-type {
  margin-bottom: 0;
}
.vps-hover-card__section-title {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.vps-hover-card__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}
.vps-hover-card__item--full {
  grid-column: 1 / -1;
}
.vps-hover-card__item {
  min-width: 0;
}
.vps-hover-card__label {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}
.vps-hover-card__value {
  min-height: 18px;
}
.vps-hover-card--dark .vps-hover-card__value { color: #cbd5e1; }
.vps-hover-card--light .vps-hover-card__value { color: #334155; }
.vps-hover-card__value--mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
.vps-hover-card__value--expired {
  color: #f43f5e;
}
.vps-hover-card__pwd-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.vps-hover-card__pwd-wrap span {
  min-width: 0;
}
.vps-hover-card__pwd-toggle {
  flex-shrink: 0;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
}
.vps-hover-card__pwd-toggle:hover {
  color: #3b82f6;
}
.vps-hover-card__tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 0 4px 4px 0;
  border: 1px solid #3b82f6;
  border-radius: 4px;
  font-size: 11px;
}
.vps-hover-card--dark .vps-hover-card__tag { color: #93c5fd; }
.vps-hover-card--light .vps-hover-card__tag { color: #2563eb; }

.vps-hover-card__lifecycle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 16px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 6px;
}
.vps-hover-card--dark .vps-hover-card__lifecycle { background: rgba(30, 41, 59, 0.5); border: 1px solid #334155; }
.vps-hover-card--light .vps-hover-card__lifecycle { background: #f8fafc; border: 1px solid #e2e8f0; }
.vps-hover-card__lifecycle-item {
  min-width: 0;
}
.vps-hover-card__lifecycle .vps-hover-card__label {
  margin-bottom: 2px;
}

.vps-hover-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
}
.vps-hover-card--dark .vps-hover-card__footer { border-top: 1px solid #334155; background: #0f172a; }
.vps-hover-card--light .vps-hover-card__footer { border-top: 1px solid #e2e8f0; background: #f8fafc; }
.vps-hover-card__update {
  font-size: 11px;
  color: #64748b;
}
.vps-hover-card__connect {
  flex-shrink: 0;
}
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
