<template>
  <div class="server-monitor-panel">
    <div class="panel-title">系统信息</div>
    <div v-if="!sysinfoData && !error" class="panel-placeholder">等待数据...</div>
    <div v-else-if="error" class="panel-error">{{ error }}</div>
    <div v-else class="panel-content">
      <template v-if="hasStructured">
        <div class="metric-card">
          <div class="metric-label">CPU</div>
          <el-progress :percentage="cpuPercent" :stroke-width="6" :show-text="true" />
        </div>
        <div class="metric-card">
          <div class="metric-label">内存</div>
          <el-progress :percentage="memoryPercent" :stroke-width="6" :show-text="true" />
          <div class="metric-detail">{{ memoryUsedMb }} MB / {{ memoryTotalMb }} MB</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">负载 (1 / 5 / 15 min)</div>
          <div class="load-values">{{ load1 }} / {{ load5 }} / {{ load15 }}</div>
        </div>
        <div class="metric-card" v-if="disks.length">
          <div class="metric-label">磁盘</div>
          <div v-for="d in disks" :key="d.mount" class="disk-row">
            <span class="disk-mount">{{ d.mount }}</span>
            <el-progress :percentage="d.usePercent" :stroke-width="6" :show-text="true" />
          </div>
        </div>
        <div class="metric-card uptime" v-if="uptimeDisplay">
          <div class="metric-label">运行时间</div>
          <div class="uptime-formatted">{{ uptimeDisplay }}</div>
        </div>
      </template>
      <template v-else>
        <div class="info-block">
          <div class="label">运行时间 / 负载</div>
          <pre class="value pre">{{ sysinfoData.uptime || '-' }}</pre>
          <pre class="value pre small">{{ sysinfoData.loadavg || '-' }}</pre>
        </div>
        <div class="info-block">
          <div class="label">内存</div>
          <pre class="value pre small">{{ sysinfoData.memory || '-' }}</pre>
        </div>
        <div class="info-block">
          <div class="label">磁盘</div>
          <pre class="value pre small">{{ sysinfoData.disk || '-' }}</pre>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onBeforeUnmount } from 'vue'

const props = defineProps({
  sendSysinfo: { type: Function, default: null },
  sysinfoData: { type: Object, default: null },
  visible: { type: Boolean, default: false }
})

const error = ref('')
let timer = null

const hasStructured = computed(() => {
  const d = props.sysinfoData
  if (!d || d.error) return false
  return d.cpuPercent != null || d.memoryPercent != null || (d.load1 != null && d.load5 != null) || (d.disks && d.disks.length)
})

const cpuPercent = computed(() => {
  const v = props.sysinfoData?.cpuPercent
  return v != null ? Math.min(100, Math.max(0, v)) : 0
})

const memoryTotalMb = computed(() => props.sysinfoData?.memoryTotalMb ?? 0)
const memoryUsedMb = computed(() => props.sysinfoData?.memoryUsedMb ?? 0)
const memoryPercent = computed(() => {
  const v = props.sysinfoData?.memoryPercent
  return v != null ? Math.min(100, Math.max(0, v)) : 0
})

const load1 = computed(() => props.sysinfoData?.load1 ?? '-')
const load5 = computed(() => props.sysinfoData?.load5 ?? '-')
const load15 = computed(() => props.sysinfoData?.load15 ?? '-')

const uptimeDisplay = computed(() => {
  const d = props.sysinfoData
  if (!d) return ''
  if (d.uptimeDays != null) {
    const parts = []
    if (d.uptimeDays > 0) parts.push(`${d.uptimeDays} 天`)
    if (d.uptimeHours > 0 || d.uptimeDays > 0) parts.push(`${d.uptimeHours ?? 0} 小时`)
    parts.push(`${d.uptimeMinutes ?? 0} 分钟`)
    return parts.join(' ')
  }
  return d.uptime || ''
})

const disks = computed(() => {
  const list = props.sysinfoData?.disks
  if (!Array.isArray(list)) return []
  const skip = /^\/(run|dev|sys|proc|boot|snap|var\/lib\/docker)|tmpfs|overlay/
  return list.filter((d) => d && d.mount && !skip.test(d.mount))
})

watch(() => props.sysinfoData, (d) => {
  if (d && d.error) error.value = d.error
  else if (d) error.value = ''
}, { immediate: true })

function startPolling() {
  if (!props.sendSysinfo) return
  stopPolling()
  props.sendSysinfo()
  timer = setInterval(() => props.sendSysinfo(), 5000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(() => [props.visible, props.sendSysinfo], ([v, fn]) => {
  if (v && fn) startPolling()
  else stopPolling()
}, { immediate: true })

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
.server-monitor-panel {
  height: 100%;
  overflow: auto;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 13px;
}
.panel-placeholder,
.panel-error {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.panel-error {
  color: var(--el-color-danger);
}
.panel-content {
  font-size: 12px;
}
.metric-card {
  margin-bottom: 10px;
  padding: 6px 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  .metric-label {
    color: var(--el-text-color-secondary);
    margin-bottom: 4px;
    font-size: 11px;
  }
  .metric-detail {
    font-size: 10px;
    color: var(--el-text-color-secondary);
    margin-top: 2px;
  }
  .load-values {
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
  }
  .uptime-formatted {
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
    color: var(--el-text-color-primary);
  }
}
.disk-row {
  margin-top: 6px;
  .disk-mount {
    display: block;
    font-size: 10px;
    color: var(--el-text-color-secondary);
    margin-bottom: 2px;
  }
}
.info-block {
  margin-bottom: 10px;
  .label {
    color: var(--el-text-color-secondary);
    margin-bottom: 4px;
  }
  .value {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
  }
  .value.pre {
    background: var(--el-fill-color-dark);
    padding: 8px;
    border-radius: 4px;
  }
  .value.small {
    max-height: 120px;
    overflow: auto;
  }
}
</style>
