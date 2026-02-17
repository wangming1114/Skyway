<template>
  <div class="server-monitor-panel">
    <div v-if="ip" class="panel-ip-row">
      <span class="panel-ip-label">IP {{ ip }}</span>
      <el-button type="primary" link size="small" @click="copyIp">复制</el-button>
    </div>
    <div v-if="error" class="panel-error">{{ errorDisplay }}</div>
    <div class="panel-content">
      <div class="block block-run-load">
        <div class="metric-row"><span class="metric-label-inline">运行</span><span class="metric-value">{{ uptimeDisplay }}</span></div>
        <div class="metric-row"><span class="metric-label-inline">负载</span><span class="metric-value">{{ loadDisplay }}</span></div>
      </div>
      <div class="block block-resource">
        <div class="resource-item">
          <div class="resource-item-head">
            <span class="metric-label">CPU {{ cpuPercent }}%</span>
          </div>
          <el-progress :percentage="cpuPercent" :stroke-width="6" :show-text="false" />
        </div>
        <div class="resource-item">
          <div class="resource-item-head">
            <span class="metric-label">内存 {{ memoryPercent }}%</span>
            <span class="metric-detail-inline">{{ formatMb(memoryUsedMb) }} / {{ formatMb(memoryTotalMb) }}</span>
          </div>
          <el-progress :percentage="memoryPercent" :stroke-width="6" :show-text="false" />
        </div>
        <div class="resource-item">
          <div class="resource-item-head">
            <span class="metric-label">交换 {{ swapPercent }}%</span>
            <span class="metric-detail-inline">{{ formatMb(swapUsedMb) }} / {{ formatMb(swapTotalMb) }}</span>
          </div>
          <el-progress :percentage="swapPercent" :stroke-width="6" :show-text="false" />
        </div>
      </div>
      <div class="block block-process">
        <div class="metric-card process-card">
          <div class="process-table-wrap">
            <table class="process-table">
              <thead>
                <tr>
                  <th class="sortable" @click="toggleProcessSort('memory')">
                    内存 {{ processSortBy === 'memory' ? (processSortOrder === 'desc' ? '↓' : '↑') : '' }}
                  </th>
                  <th class="sortable" @click="toggleProcessSort('cpu')">
                    CPU {{ processSortBy === 'cpu' ? (processSortOrder === 'desc' ? '↓' : '↑') : '' }}
                  </th>
                  <th>命令</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, i) in sortedProcesses" :key="i">
                  <td>{{ p.rssMb }}M</td>
                  <td>{{ p.cpuPct }}</td>
                  <td class="process-comm">{{ p.comm }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="block block-network">
        <div class="metric-card network-card">
          <div class="network-row">
            <span class="network-down">下载 ↓ {{ networkDownRate }}</span>
            <span class="network-up">上传 ↑ {{ networkUpRate }}</span>
            <el-select v-model="selectedInterface" size="small" class="network-select">
              <el-option
                v-for="iface in interfaces"
                :key="iface.name"
                :label="iface.name"
                :value="iface.name"
              />
            </el-select>
          </div>
          <div ref="networkChartRef" class="network-chart"></div>
        </div>
      </div>
      <div class="block block-disk">
        <div class="metric-card disk-card">
          <table class="disk-table">
            <thead>
              <tr>
                <th>路径</th>
                <th>可用/大小</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in disks" :key="d.mount">
                <td class="disk-path">{{ d.mount }}</td>
                <td>{{ d.avail }} / {{ d.size }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onBeforeUnmount, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const props = defineProps({
  ip: { type: String, default: '' },
  sendSysinfo: { type: Function, default: null },
  sysinfoData: { type: Object, default: null },
  visible: { type: Boolean, default: false }
})

const error = ref('')
const errorDisplay = computed(() => {
  const msg = error.value
  if (!msg) return ''
  if (/BINARY_PARTIAL_WRITING|invalid state|InvalidState/i.test(msg)) return '连接繁忙，请稍候重试'
  if (msg.length > 60) return msg.slice(0, 57) + '...'
  return msg
})
let timer = null
const processSortBy = ref('cpu')
const processSortOrder = ref('desc')
const selectedInterface = ref('eth0')
const networkChartRef = ref(null)
let networkChartInstance = null
const lastNetSample = ref(null)
const netChartData = ref({ times: [], rx: [], tx: [] })
const MAX_CHART_POINTS = 30

function toggleProcessSort(field) {
  if (processSortBy.value === field) {
    processSortOrder.value = processSortOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    processSortBy.value = field
    processSortOrder.value = 'desc'
  }
}

const uptimeDisplay = computed(() => {
  const d = props.sysinfoData
  if (!d || d.error) return '-'
  if (d.uptimeDays != null) {
    const parts = []
    if (d.uptimeDays > 0) parts.push(`${d.uptimeDays}天`)
    if (d.uptimeHours > 0 || d.uptimeDays > 0) parts.push(`${d.uptimeHours ?? 0}小时`)
    parts.push(`${d.uptimeMinutes ?? 0}分钟`)
    return parts.length ? parts.join('') : '-'
  }
  return (d.uptime && d.uptime.trim()) ? d.uptime.trim() : '-'
})

const loadDisplay = computed(() => {
  const d = props.sysinfoData
  if (!d || d.error || d.load1 == null) return '-'
  const l1 = d.load1 != null ? Number(d.load1).toFixed(2) : '-'
  const l5 = d.load5 != null ? Number(d.load5).toFixed(2) : '-'
  const l15 = d.load15 != null ? Number(d.load15).toFixed(2) : '-'
  return `${l1}, ${l5}, ${l15}`
})

const cpuPercent = computed(() => {
  const v = props.sysinfoData?.cpuPercent
  return v != null ? Math.min(100, Math.max(0, Math.round(v))) : 0
})

const memoryTotalMb = computed(() => props.sysinfoData?.memoryTotalMb ?? 0)
const memoryUsedMb = computed(() => props.sysinfoData?.memoryUsedMb ?? 0)
const memoryPercent = computed(() => {
  const v = props.sysinfoData?.memoryPercent
  return v != null ? Math.min(100, Math.max(0, v)) : 0
})

const swapTotalMb = computed(() => props.sysinfoData?.swapTotalMb ?? 0)
const swapUsedMb = computed(() => props.sysinfoData?.swapUsedMb ?? 0)
const swapPercent = computed(() => {
  const v = props.sysinfoData?.swapPercent
  return v != null ? Math.min(100, Math.max(0, v)) : 0
})

const processes = computed(() => {
  const list = props.sysinfoData?.processes
  return Array.isArray(list) ? list : []
})

const sortedProcesses = computed(() => {
  const list = [...processes.value]
  const order = processSortOrder.value === 'desc' ? -1 : 1
  if (processSortBy.value === 'memory') {
    list.sort((a, b) => order * ((b.rssMb ?? 0) - (a.rssMb ?? 0)))
  } else if (processSortBy.value === 'cpu') {
    list.sort((a, b) => order * ((b.cpuPct ?? 0) - (a.cpuPct ?? 0)))
  }
  return list.slice(0, 10)
})

const interfaces = computed(() => {
  const list = props.sysinfoData?.interfaces
  return Array.isArray(list) ? list : []
})

const currentRxRate = ref(0)
const currentTxRate = ref(0)

function formatRate(bytesPerSec) {
  if (bytesPerSec == null || bytesPerSec <= 0) return '0 B/s'
  if (bytesPerSec >= 1e9) return (bytesPerSec / 1e9).toFixed(2) + ' G/s'
  if (bytesPerSec >= 1e6) return (bytesPerSec / 1e6).toFixed(2) + ' M/s'
  if (bytesPerSec >= 1e3) return (bytesPerSec / 1e3).toFixed(1) + ' K/s'
  return Math.round(bytesPerSec) + ' B/s'
}

const networkUpRate = computed(() => formatRate(currentTxRate.value))
const networkDownRate = computed(() => formatRate(currentRxRate.value))

const disks = computed(() => {
  const list = props.sysinfoData?.disks
  if (!Array.isArray(list)) return []
  const skip = /^\/(run|dev|sys|proc|boot|snap|var\/lib\/docker)|tmpfs|overlay/
  return list.filter((d) => d && d.mount && !skip.test(d.mount))
})

watch(
  () => props.sysinfoData?.interfaces,
  (list) => {
    if (Array.isArray(list) && list.length && !list.some((i) => i.name === selectedInterface.value)) {
      selectedInterface.value = list[0].name
    }
  },
  { immediate: true }
)

watch(() => props.sysinfoData, (d) => {
  if (d && d.error) error.value = d.error
  else if (d && !d.error) error.value = ''
}, { immediate: true })

watch(
  () => [props.sysinfoData?.interfaces, selectedInterface.value],
  ([list]) => {
    if (!Array.isArray(list) || !list.length) return
    const now = Date.now()
    const byIface = {}
    list.forEach((i) => {
      byIface[i.name] = { rxBytes: i.rxBytes ?? 0, txBytes: i.txBytes ?? 0 }
    })
    const cur = list.find((i) => i.name === selectedInterface.value) || list[0]
    if (!cur) return
    const prev = lastNetSample.value
    if (prev && prev.byIface[cur.name] != null) {
      const dt = (now - prev.time) / 1000
      if (dt > 0) {
        const rxRate = Math.max(0, (cur.rxBytes - prev.byIface[cur.name].rxBytes) / dt)
        const txRate = Math.max(0, (cur.txBytes - prev.byIface[cur.name].txBytes) / dt)
        currentRxRate.value = rxRate
        currentTxRate.value = txRate
        const data = netChartData.value
        const times = [...data.times, now]
        const rx = [...data.rx, Math.round(rxRate)]
        const tx = [...data.tx, Math.round(txRate)]
        if (times.length > MAX_CHART_POINTS) {
          times.shift()
          rx.shift()
          tx.shift()
        }
        netChartData.value = { times, rx, tx }
        updateNetworkChart()
      }
    } else {
      currentRxRate.value = 0
      currentTxRate.value = 0
    }
    lastNetSample.value = { time: now, byIface }
  },
  { immediate: true }
)

watch(selectedInterface, () => {
  netChartData.value = { times: [], rx: [], tx: [] }
  updateNetworkChart()
})

function initNetworkChart() {
  if (!networkChartRef.value) return
  networkChartInstance = echarts.init(networkChartRef.value)
  updateNetworkChart()
}

function updateNetworkChart() {
  if (!networkChartInstance || !networkChartRef.value) return
  const data = netChartData.value
  const timeLabels = data.times.map((t) => {
    const d = new Date(t)
    return d.getMinutes() + ':' + String(d.getSeconds()).padStart(2, '0')
  })
  const success = getComputedStyle(document.documentElement).getPropertyValue('--el-color-success').trim() || '#67c23a'
  const danger = getComputedStyle(document.documentElement).getPropertyValue('--el-color-danger').trim() || '#f56c6c'
  networkChartInstance.resize()
  networkChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (items) => {
        const rx = items.find((i) => i.seriesName === '下载')
        const tx = items.find((i) => i.seriesName === '上传')
        const rxVal = rx?.value != null ? formatRate(rx.value) : '0 B/s'
        const txVal = tx?.value != null ? formatRate(tx.value) : '0 B/s'
        return (items[0]?.axisValue ?? '') + '<br/>↓ 下载 ' + rxVal + '<br/>↑ 上传 ' + txVal
      }
    },
    grid: { left: 32, right: 8, top: 4, bottom: 16 },
    xAxis: { type: 'category', data: timeLabels, boundaryGap: false, axisLabel: { fontSize: 9 } },
    yAxis: {
      type: 'value',
      name: 'B/s',
      nameTextStyle: { fontSize: 9 },
      axisLabel: { fontSize: 9, formatter: (v) => v >= 1024 ? (v / 1024).toFixed(0) + 'K' : v },
      splitLine: { lineStyle: { opacity: 0.25 } }
    },
    series: [
      { name: '下载', type: 'line', smooth: true, data: data.rx, itemStyle: { color: success }, areaStyle: { color: success, opacity: 0.2 } },
      { name: '上传', type: 'line', smooth: true, data: data.tx, itemStyle: { color: danger }, areaStyle: { color: danger, opacity: 0.2 } }
    ]
  }, true)
}

onMounted(() => {
  setTimeout(initNetworkChart, 100)
})

watch(networkChartRef, (el) => {
  if (el) initNetworkChart()
})

function formatMb(mb) {
  if (mb == null || mb === 0) return '0'
  if (mb >= 1024) return (mb / 1024).toFixed(1) + 'G'
  return mb + 'M'
}

function copyIp() {
  if (!props.ip) return
  navigator.clipboard.writeText(props.ip).then(() => {
    ElMessage.success('已复制')
  }).catch(() => {})
}

function startPolling() {
  if (!props.sendSysinfo) return
  stopPolling()
  props.sendSysinfo()
  timer = setInterval(() => props.sendSysinfo(), 3000)
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
  if (networkChartInstance) {
    networkChartInstance.dispose()
    networkChartInstance = null
  }
})
</script>

<style scoped lang="scss">
.server-monitor-panel {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  padding: 10px 12px;
  font-size: 12px;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color);
}
.panel-ip-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: var(--el-bg-color);
  border-radius: 2px;
  font-size: 12px;
  border: 1px solid var(--el-border-color);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  .panel-ip-label {
    color: var(--el-text-color-regular);
    font-weight: 500;
  }
}
.panel-error {
  flex-shrink: 0;
  color: var(--el-color-danger);
  font-size: 12px;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: var(--el-color-danger-light-9);
  border-radius: 2px;
  border: 1px solid var(--el-color-danger-light-7);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.panel-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.block {
  flex-shrink: 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.block-run-load {
  .metric-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    min-height: 0;
    border-radius: 0;
    border: none;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: transparent;
    &:last-child {
      border-bottom: none;
    }
  }
  .metric-label-inline {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    flex-shrink: 0;
  }
  .metric-value {
    font-family: ui-monospace, Consolas, Monaco, monospace;
    font-size: 12px;
    color: var(--el-text-color-primary);
    min-width: 0;
    overflow-wrap: break-word;
  }
}
.block-resource {
  display: flex;
  flex-direction: column;
  .resource-item {
    padding: 10px 12px;
    min-height: 46px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    border-bottom: 1px solid var(--el-border-color-lighter);
    &:last-child {
      border-bottom: none;
    }
    .resource-item-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      margin-bottom: 6px;
    }
    .metric-label {
      color: var(--el-text-color-secondary);
      font-size: 11px;
      font-weight: 500;
    }
    .metric-detail-inline {
      font-size: 11px;
      color: var(--el-text-color-secondary);
      font-family: ui-monospace, Consolas, Monaco, monospace;
      flex-shrink: 0;
    }
    :deep(.el-progress-bar__outer) {
      height: 6px;
      border-radius: 2px;
    }
    :deep(.el-progress-bar__inner) {
      border-radius: 2px;
    }
  }
}
.block-process {
  .metric-card.process-card {
    border: none;
    background: transparent;
    padding: 10px 12px;
  }
}
.block-network {
  .metric-card.network-card {
    border: none;
    background: transparent;
    padding: 10px 12px;
  }
}
.block-disk {
  .metric-card.disk-card {
    border: none;
    background: transparent;
    padding: 10px 12px;
  }
}
.metric-card {
  flex-shrink: 0;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 2px;
  .metric-label {
    color: var(--el-text-color-secondary);
    margin-bottom: 3px;
    font-size: 11px;
    font-weight: 500;
  }
  .metric-detail {
    font-size: 11px;
    color: var(--el-text-color-secondary);
    margin-top: 2px;
    font-family: ui-monospace, Consolas, Monaco, monospace;
  }
  :deep(.el-progress-bar__outer) {
    border-radius: 2px;
  }
  :deep(.el-progress-bar__inner) {
    border-radius: 2px;
  }
}
.process-card {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  max-height: 140px;
  padding: 10px 12px;
  .process-table-wrap {
    overflow-x: hidden;
    overflow-y: auto;
    max-height: 140px;
    margin: 0 -2px;
    border-radius: 2px;
    scrollbar-width: none;
    -ms-overflow-style: none;
    &::-webkit-scrollbar {
      display: none;
    }
  }
  .process-table {
    width: 100%;
    font-size: 11px;
    border-collapse: collapse;
    th, td {
      padding: 5px 8px;
      text-align: left;
      border-bottom: 1px solid var(--el-border-color-lighter);
    }
    thead th {
      position: sticky;
      top: 0;
      z-index: 1;
      background: var(--el-fill-color);
      color: var(--el-text-color-secondary);
      font-weight: 600;
      font-size: 11px;
      white-space: nowrap;
      box-shadow: 0 1px 0 0 var(--el-border-color-lighter);
    }
    th.sortable {
      cursor: pointer;
      user-select: none;
      transition: color 0.15s;
    }
    th.sortable:hover {
      color: var(--el-color-primary);
    }
    tbody tr:hover {
      background: var(--el-fill-color);
    }
    .process-comm {
      max-width: 100px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-family: ui-monospace, Consolas, Monaco, monospace;
    }
  }
}
.network-card {
  flex-shrink: 0;
  .network-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
    flex-wrap: wrap;
  }
  .network-up, .network-down {
    font-family: ui-monospace, Consolas, Monaco, monospace;
    font-size: 12px;
    font-weight: 500;
  }
  .network-up { color: var(--el-color-danger); }
  .network-down { color: var(--el-color-success); }
  .network-select { flex: 1; min-width: 72px; }
  .network-chart {
    height: 76px;
    width: 100%;
    border-radius: 2px;
    background: var(--el-fill-color);
  }
}
.disk-card {
  flex-shrink: 0;
  .disk-table {
    width: 100%;
    font-size: 11px;
    border-collapse: collapse;
    th, td {
      padding: 5px 8px;
      text-align: left;
      border-bottom: 1px solid var(--el-border-color-lighter);
    }
    thead th {
      color: var(--el-text-color-secondary);
      font-weight: 600;
      font-size: 11px;
    }
    tbody tr:hover {
      background: var(--el-fill-color);
    }
    .disk-path {
      max-width: 88px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-family: ui-monospace, Consolas, Monaco, monospace;
    }
  }
}
</style>
