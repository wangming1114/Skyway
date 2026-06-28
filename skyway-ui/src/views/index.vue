<template>
  <div class="app-container dashboard">
    <section class="overview-grid">
      <el-card v-for="card in overviewCards" :key="card.key" shadow="hover" class="overview-card" :class="'overview-card--' + card.tone" @click="goTo(card.path)">
        <div class="overview-card__head">
          <span class="overview-card__title">{{ card.title }}</span>
          <el-tag size="small" :type="card.tagType" effect="plain">{{ card.tag }}</el-tag>
        </div>
        <div class="overview-card__main">{{ card.value }}</div>
        <div class="overview-card__metrics">
          <span v-for="item in card.metrics" :key="item.label">
            <b>{{ item.value }}</b>{{ item.label }}
          </span>
        </div>
      </el-card>
    </section>

    <section class="dashboard-grid dashboard-grid--top">
      <el-card shadow="hover" class="panel-card panel-card--trend">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">VPS 流量趋势</div>
              <div class="panel-subtitle">按 VPS 聚合，支持日、周、15 天、月级别统计</div>
            </div>
            <el-radio-group v-model="trafficRange" size="small" @change="handleTrafficRangeChange">
              <el-radio-button v-for="item in trafficRangeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="trend-layout">
          <div class="trend-chart">
            <div ref="trafficTrendRef" class="trend-chart__canvas"></div>
            <div v-if="trafficTrendEmpty" class="empty-state empty-state--overlay">暂无流量趋势数据</div>
          </div>
          <div class="trend-rank">
            <div class="trend-rank__title">{{ trafficRangeLabel }} VPS 流量排行</div>
            <div v-for="(row, index) in vpsPeriodRankList" :key="row.instanceId" class="trend-rank__item" @click="goVpsDetail(row.instanceId)">
              <span class="rank-index">{{ index + 1 }}</span>
              <div>
                <strong>{{ row.instanceName || ('VPS #' + row.instanceId) }}</strong>
                <small>{{ row.instanceIp || '-' }}</small>
              </div>
              <em>{{ formatTraffic(row.totalTraffic) }}</em>
            </div>
            <div v-if="!vpsPeriodRankList.length" class="empty-list empty-list--compact">暂无排行数据</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="panel-card period-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">VPS 周期用量</div>
              <div class="panel-subtitle">日、周、15 天、月级别快捷摘要</div>
            </div>
            <el-button link type="primary" @click="goTo('/resource/vps')">查看 VPS</el-button>
          </div>
        </template>
        <div class="period-grid">
          <button
            v-for="item in periodSummaryCards"
            :key="item.days"
            class="period-summary"
            :class="{ 'period-summary--active': trafficRange === item.days }"
            @click="setTrafficRange(item.days)"
          >
            <span>{{ item.label }}</span>
            <strong>{{ formatTraffic(item.totalTraffic) }}</strong>
            <small>{{ item.activeVps }} 台 VPS 有流量</small>
          </button>
        </div>
        <div class="period-detail">
          <div class="period-detail__main">
            <span>{{ trafficRangeLabel }}总流量</span>
            <strong>{{ formatTraffic(currentPeriodSummary.totalTraffic) }}</strong>
          </div>
          <div class="period-detail__list">
            <div>
              <span>峰值 VPS</span>
              <strong>{{ currentPeriodSummary.peakVpsName }}</strong>
            </div>
            <div>
              <span>峰值流量</span>
              <strong>{{ formatTraffic(currentPeriodSummary.peakTraffic) }}</strong>
            </div>
            <div>
              <span>日均流量</span>
              <strong>{{ formatTraffic(currentPeriodSummary.dailyAverage) }}</strong>
            </div>
            <div>
              <span>活跃 VPS</span>
              <strong>{{ currentPeriodSummary.activeVps }}</strong>
            </div>
          </div>
        </div>
      </el-card>
    </section>

    <section class="dashboard-grid dashboard-grid--rank">
      <el-card shadow="hover" class="panel-card rank-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">VPS 流量排行</div>
              <div class="panel-subtitle">按实例累计流量排序，辅助定位资源压力</div>
            </div>
            <el-button link type="primary" @click="goTo('/resource/vps')">全部 VPS</el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="vpsRankList" size="small" stripe height="360">
          <el-table-column label="#" width="48" align="center">
            <template #default="{ $index }">
              <span class="rank-index">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="VPS" min-width="150" align="left" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="name-cell">
                <el-link type="primary" :underline="false" @click="goVpsDetail(row.id)">{{ row.name || ('VPS #' + row.id) }}</el-link>
                <small>{{ row.ip || '-' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="节点" prop="nodeCount" width="70" align="center" />
          <el-table-column label="累计流量" width="130" align="right">
            <template #default="{ row }">{{ formatTraffic(row.totalTrafficBytes) }}</template>
          </el-table-column>
          <el-table-column label="当前速率" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ instanceSpeedText(row) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="vpsStatusType(row.status)">{{ vpsStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="hover" class="panel-card rank-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">用户流量排行</div>
              <div class="panel-subtitle">按用户名下节点累计流量排序</div>
            </div>
            <el-button link type="primary" @click="goTo('/member/customer')">全部用户</el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="customerRankList" size="small" stripe height="360">
          <el-table-column label="#" width="48" align="center">
            <template #default="{ $index }">
              <span class="rank-index">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="140" align="left" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="name-cell">
                <el-link type="primary" :underline="false" @click="goCustomerDetail(row.id)">{{ row.username || ('用户 #' + row.id) }}</el-link>
                <small>{{ row.status === '1' ? '已停用' : '正常' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="节点" prop="nodeCount" width="70" align="center" />
          <el-table-column label="累计流量" width="130" align="right">
            <template #default="{ row }">{{ formatTraffic(row.totalTraffic) }}</template>
          </el-table-column>
          <el-table-column label="到期节点" prop="expiringCount" width="90" align="center" />
          <el-table-column label="限速节点" prop="limitedCount" width="90" align="center" />
        </el-table>
      </el-card>
    </section>

    <section class="dashboard-grid dashboard-grid--detail">
      <el-card shadow="hover" class="panel-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">近 30 天客户流量排行</div>
              <div class="panel-subtitle">按客户名下节点流量倒序展示</div>
            </div>
            <el-button link type="primary" @click="goTo('/member/customer')">全部用户</el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="monthlyCustomerRankList" size="small" stripe height="300">
          <el-table-column label="#" width="48" align="center">
            <template #default="{ $index }">
              <span class="rank-index">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="140" align="left" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="name-cell">
                <el-link type="primary" :underline="false" @click="goCustomerDetail(row.customerId)">{{ row.username || ('用户 #' + row.customerId) }}</el-link>
                <small>{{ row.nodeCount || 0 }} 个节点</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="下载" width="120" align="right">
            <template #default="{ row }">{{ formatTraffic(row.totalRx) }}</template>
          </el-table-column>
          <el-table-column label="上传" width="120" align="right">
            <template #default="{ row }">{{ formatTraffic(row.totalTx) }}</template>
          </el-table-column>
          <el-table-column label="合计" width="130" align="right">
            <template #default="{ row }"><strong>{{ formatTraffic(row.totalTraffic) }}</strong></template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="hover" class="panel-card detail-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-title">近期到期节点</div>
              <div class="panel-subtitle">按到期时间升序展示，优先处理临期资源</div>
            </div>
            <el-button link type="primary" @click="goTo('/resource/vps')">查看全部</el-button>
          </div>
        </template>
        <el-table v-loading="loading" :data="expiringNodeList" size="small" stripe height="300">
          <el-table-column label="节点" prop="nodeName" min-width="140" show-overflow-tooltip />
          <el-table-column label="用户" min-width="100" align="left" show-overflow-tooltip>
            <template #default="{ row }">{{ customerName(row.customerId) }}</template>
          </el-table-column>
          <el-table-column label="VPS" min-width="120" align="left" show-overflow-tooltip>
            <template #default="{ row }">{{ instanceName(row.instanceId) }}</template>
          </el-table-column>
          <el-table-column label="流量" width="140" align="right">
            <template #default="{ row }">{{ formatTraffic(nodeTrafficTotal(row.id)) }}</template>
          </el-table-column>
          <el-table-column label="有效期" width="110" align="center">
            <template #default="{ row }">
              <span :class="{ 'text-danger': isExpired(row.expireTime) }">{{ expireText(row.expireTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="限速" width="82" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.rateLimit ? 'warning' : 'info'" effect="plain">{{ row.rateLimit ? '启用' : '无' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </div>
</template>

<script setup name="Index">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  getDashboardCustomerTrafficRank,
  getDashboardSummary,
  getDashboardVpsTrafficTrend,
  getInstanceSpeedSnapshot,
  getProxyNodeTraffic,
  listInstance,
  listProxyNode
} from '@/api/resource/vps'
import { listCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'

const router = useRouter()

const summary = ref({
  totalVps: 0,
  runningVps: 0,
  stoppedVps: 0,
  abnormalVps: 0,
  totalNodes: 0,
  normalNodes: 0,
  disabledNodes: 0,
  totalCustomers: 0,
  expiringNodes: 0,
  expiredNodes: 0,
  limitedNodes: 0,
  vpsTrafficTotal: 0,
  customerTrafficTotal: 0
})

const loading = ref(false)
const instances = ref([])
const nodes = ref([])
const customers = ref([])
const nodeTrafficMap = ref({})
const speedMap = ref({})
const trafficTrend = ref([])
const vpsTrafficRows = ref([])
const monthlyCustomerRankList = ref([])
const trafficPeriodRows = ref({})
const trafficRange = ref(7)
const trafficRangeOptions = [
  { label: '日', value: 1 },
  { label: '周', value: 7 },
  { label: '15天', value: 15 },
  { label: '月', value: 30 }
]

const trafficTrendRef = ref(null)
let trafficTrendChart = null

const todayTrafficTotal = computed(() => trafficTrend.value.length ? trafficTrend.value[trafficTrend.value.length - 1].total : 0)
const trafficTrendEmpty = computed(() => !trafficTrend.value.some(item => item.total > 0))
const trafficRangeLabel = computed(() => trafficRangeOptions.find(item => item.value === trafficRange.value)?.label || '')

const overviewCards = computed(() => [
  {
    key: 'health',
    title: '平台健康',
    tag: summary.value.abnormalVps > 0 ? '需关注' : '正常',
    tagType: summary.value.abnormalVps > 0 ? 'warning' : 'success',
    value: `${summary.value.runningVps}/${summary.value.totalVps}`,
    tone: summary.value.abnormalVps > 0 ? 'warn' : 'success',
    path: '/resource/vps',
    metrics: [
      { label: '运行中', value: summary.value.runningVps },
      { label: '异常', value: summary.value.abnormalVps },
      { label: '停止', value: summary.value.stoppedVps }
    ]
  },
  {
    key: 'supply',
    title: '节点供给',
    tag: summary.value.expiringNodes > 0 ? '有到期' : '稳定',
    tagType: summary.value.expiringNodes > 0 ? 'warning' : 'success',
    value: summary.value.normalNodes,
    tone: 'primary',
    path: '/resource/vps',
    metrics: [
      { label: '总节点', value: summary.value.totalNodes },
      { label: '停用', value: summary.value.disabledNodes },
      { label: '30天到期', value: summary.value.expiringNodes }
    ]
  },
  {
    key: 'traffic',
    title: '今日流量',
    tag: '趋势',
    tagType: '',
    value: formatTraffic(todayTrafficTotal.value),
    tone: 'traffic',
    path: '/resource/vps',
    metrics: [
      { label: 'VPS累计', value: formatTraffic(summary.value.vpsTrafficTotal) },
      { label: '用户累计', value: formatTraffic(summary.value.customerTrafficTotal) }
    ]
  },
  {
    key: 'customer',
    title: '用户消耗',
    tag: summary.value.limitedNodes > 0 ? '有限速' : '正常',
    tagType: summary.value.limitedNodes > 0 ? 'warning' : 'success',
    value: summary.value.totalCustomers,
    tone: 'customer',
    path: '/member/customer',
    metrics: [
      { label: '绑定节点', value: boundNodeCount.value },
      { label: '限速节点', value: summary.value.limitedNodes },
      { label: '过期节点', value: summary.value.expiredNodes }
    ]
  }
])

const boundNodeCount = computed(() => nodes.value.filter(row => row.customerId != null).length)

const periodSummaryCards = computed(() => trafficRangeOptions.map(option => buildPeriodSummary(option.value, option.label)))
const currentPeriodSummary = computed(() => buildPeriodSummary(trafficRange.value, trafficRangeLabel.value))

const vpsRankList = computed(() => {
  return [...instances.value]
    .sort((a, b) => Number(b.totalTrafficBytes || 0) - Number(a.totalTrafficBytes || 0))
    .slice(0, 10)
})

const customerRankList = computed(() => {
  const customerMap = new Map(customers.value.map(row => [row.id, { ...row, nodeCount: 0, totalTraffic: 0, expiringCount: 0, limitedCount: 0 }]))
  nodes.value.forEach(node => {
    if (node.customerId == null) return
    const row = customerMap.get(node.customerId) || { id: node.customerId, username: '用户 #' + node.customerId, nodeCount: 0, totalTraffic: 0, expiringCount: 0, limitedCount: 0 }
    row.nodeCount += 1
    row.totalTraffic += nodeTrafficTotal(node.id)
    if (isExpiringWithin(node.expireTime, 30)) row.expiringCount += 1
    if (node.rateLimit) row.limitedCount += 1
    customerMap.set(node.customerId, row)
  })
  return [...customerMap.values()]
    .filter(row => row.nodeCount > 0 || row.totalTraffic > 0)
    .sort((a, b) => Number(b.totalTraffic || 0) - Number(a.totalTraffic || 0))
    .slice(0, 10)
})

const vpsPeriodRankList = computed(() => {
  const map = buildVpsTrafficMap()
  return [...map.values()]
    .sort((a, b) => Number(b.totalTraffic || 0) - Number(a.totalTraffic || 0))
    .slice(0, 8)
})

const expiringNodeList = computed(() => {
  return nodes.value
    .filter(row => row.expireTime && !isExpired(row.expireTime))
    .sort((a, b) => new Date(a.expireTime) - new Date(b.expireTime))
    .slice(0, 12)
})

async function loadDashboard() {
  loading.value = true
  try {
    const [summaryRes, instanceRes, nodeRes, customerRes, speedRes, customerRankRes] = await Promise.all([
      getDashboardSummary().catch(() => ({ data: {} })),
      listInstance({ pageNum: 1, pageSize: 100 }).catch(() => ({ rows: [] })),
      listProxyNode({ pageNum: 1, pageSize: 200 }).catch(() => ({ rows: [] })),
      listCustomer({ pageNum: 1, pageSize: 200 }).catch(() => ({ rows: [] })),
      getInstanceSpeedSnapshot().catch(() => ({ data: {} })),
      getDashboardCustomerTrafficRank(30).catch(() => ({ data: [] }))
    ])
    Object.assign(summary.value, summaryRes.data || {})
    instances.value = instanceRes.rows || []
    nodes.value = nodeRes.rows || []
    customers.value = customerRes.rows || []
    speedMap.value = buildSpeedMap(instances.value, speedRes.data || {})
    monthlyCustomerRankList.value = customerRankRes.data || []
    await loadNodeTraffic()
    await loadTrafficPeriodSummary()
    setVpsTrafficRowsForRange()
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function buildSpeedMap(rows, snapshot) {
  const next = {}
  rows.forEach(row => {
    if (row.id == null) return
    if (row.status !== 'running') {
      next[row.id] = { skipped: true }
    } else {
      next[row.id] = snapshot[String(row.id)] || snapshot[row.id] || { skipped: true }
    }
  })
  return next
}

async function loadNodeTraffic() {
  const targets = nodes.value.slice(0, 80)
  const pairs = await Promise.all(targets.map(row =>
    getProxyNodeTraffic(row.id)
      .then(res => [row.id, res.data || {}])
      .catch(() => [row.id, {}])
  ))
  nodeTrafficMap.value = Object.fromEntries(pairs)
}

async function handleTrafficRangeChange() {
  setVpsTrafficRowsForRange()
  await nextTick()
  renderTrafficTrend()
}

async function setTrafficRange(days) {
  trafficRange.value = days
  await handleTrafficRangeChange()
}

async function loadTrafficPeriodSummary() {
  const pairs = await Promise.all(trafficRangeOptions.map(option =>
    getDashboardVpsTrafficTrend(option.value)
      .then(res => [option.value, res.data || []])
      .catch(() => [option.value, []])
  ))
  trafficPeriodRows.value = Object.fromEntries(pairs)
}

function setVpsTrafficRowsForRange() {
  vpsTrafficRows.value = trafficPeriodRows.value[trafficRange.value] || []
  buildTrafficTrend()
}

function buildTrafficTrend() {
  const days = []
  const now = new Date()
  for (let i = trafficRange.value - 1; i >= 0; i--) {
    const date = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i)
    const key = parseTime(date, '{y}-{m}-{d}')
    days.push({ key, label: parseTime(date, '{m}-{d}'), upload: 0, download: 0, total: 0 })
  }
  const dayMap = new Map(days.map(day => [day.key, day]))
  vpsTrafficRows.value.forEach(item => {
    const key = normalizeDateKey(item.statDate || item.statdate)
    const day = dayMap.get(key)
    if (!day) return
    day.download += Number(item.totalRx) || 0
    day.upload += Number(item.totalTx) || 0
    day.total = day.download + day.upload
  })
  trafficTrend.value = days
}

function renderCharts() {
  renderTrafficTrend()
}

function renderTrafficTrend() {
  if (!trafficTrendRef.value) return
  if (!trafficTrendChart) trafficTrendChart = echarts.init(trafficTrendRef.value, 'macarons')
  if (trafficTrendEmpty.value) {
    trafficTrendChart.clear()
    return
  }
  const days = trafficTrend.value
  const topVps = vpsPeriodRankList.value.slice(0, 5)
  trafficTrendChart.setOption({
    tooltip: { trigger: 'axis', formatter: trafficTooltipFormatter },
    legend: { bottom: 0, type: 'scroll', data: topVps.map(item => item.instanceName || ('VPS #' + item.instanceId)) },
    grid: { left: 42, right: 24, top: 28, bottom: 44 },
    xAxis: { type: 'category', boundaryGap: false, data: days.map(item => item.label) },
    yAxis: { type: 'value', axisLabel: { formatter: value => formatTraffic(value) } },
    series: topVps.map(item => ({
      name: item.instanceName || ('VPS #' + item.instanceId),
      type: trafficRange.value === 1 ? 'bar' : 'line',
      smooth: trafficRange.value !== 1,
      areaStyle: trafficRange.value === 1 ? undefined : {},
      data: days.map(day => item.dailyMap[day.key] || 0)
    }))
  })
}

function trafficTooltipFormatter(params) {
  return params.map(item => `${item.marker}${item.seriesName}: ${formatTraffic(item.value)}`).join('<br/>')
}

function handleResize() {
  trafficTrendChart?.resize()
}

function buildVpsTrafficMap() {
  const map = new Map()
  vpsTrafficRows.value.forEach(row => {
    mergeVpsTrafficRow(map, row)
  })
  return map
}

function buildPeriodSummary(days, label) {
  const map = new Map()
  ;(trafficPeriodRows.value[days] || []).forEach(row => {
    mergeVpsTrafficRow(map, row)
  })
  const rows = [...map.values()]
  const totalTraffic = rows.reduce((sum, row) => sum + row.totalTraffic, 0)
  const peak = rows.sort((a, b) => b.totalTraffic - a.totalTraffic)[0]
  return {
    days,
    label,
    totalTraffic,
    activeVps: rows.filter(row => row.totalTraffic > 0).length,
    peakVpsName: peak ? (peak.instanceName || ('VPS #' + peak.instanceId)) : '-',
    peakTraffic: peak?.totalTraffic || 0,
    dailyAverage: totalTraffic / Math.max(days, 1)
  }
}

function mergeVpsTrafficRow(map, row) {
    const instanceId = row.instanceId ?? row.instanceid
    if (instanceId == null) return map
    const key = normalizeDateKey(row.statDate || row.statdate)
    const item = map.get(instanceId) || {
      instanceId,
      instanceName: row.instanceName || row.instancename,
      instanceIp: row.instanceIp || row.instanceip,
      nodeCount: Number(row.nodeCount || row.nodecount || 0),
      totalRx: 0,
      totalTx: 0,
      totalTraffic: 0,
      dailyMap: {}
    }
    const rx = Number(row.totalRx || row.totalrx || 0)
    const tx = Number(row.totalTx || row.totaltx || 0)
    item.totalRx += rx
    item.totalTx += tx
    item.totalTraffic += rx + tx
    item.dailyMap[key] = (item.dailyMap[key] || 0) + rx + tx
    map.set(instanceId, item)
  return map
}

function normalizeDateKey(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

function nodeTrafficTotal(nodeId) {
  const stat = nodeTrafficMap.value[nodeId]
  return Number(stat?.totalRx || 0) + Number(stat?.totalTx || 0)
}

function customerName(customerId) {
  if (customerId == null) return '-'
  const customer = customers.value.find(row => row.id === customerId)
  return customer?.username || ('用户 #' + customerId)
}

function instanceName(instanceId) {
  if (instanceId == null) return '-'
  const instance = instances.value.find(row => row.id === instanceId)
  return instance?.name || ('VPS #' + instanceId)
}

function instanceSpeedText(row) {
  const speed = speedMap.value[row.id]
  if (row.status !== 'running' || speed?.skipped) return '未监控'
  if (!speed || speed.error) return '-'
  return `↑ ${formatSpeedFromMb(speed.totalUpMbps)} / ↓ ${formatSpeedFromMb(speed.totalDownMbps)}`
}

function vpsStatusText(status) {
  if (status === 'running') return '运行'
  if (status === 'abnormal') return '异常'
  if (status === 'stopped') return '停止'
  return status || '未知'
}

function vpsStatusType(status) {
  if (status === 'running') return 'success'
  if (status === 'abnormal') return 'danger'
  if (status === 'stopped') return 'info'
  return ''
}

function formatTraffic(bytes) {
  const value = Number(bytes) || 0
  if (value <= 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.max(0, Math.min(Math.floor(Math.log(value) / Math.log(k)), sizes.length - 1))
  return (value / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

function formatSpeedFromMb(value) {
  const mb = Number(value)
  if (!Number.isFinite(mb) || mb <= 0) return '0 B/s'
  return formatTraffic(mb * 1024 * 1024) + '/s'
}

function expireText(expireTime) {
  if (!expireTime) return '永久'
  return parseTime(expireTime, '{y}-{m}-{d}')
}

function isExpired(expireTime) {
  return !!expireTime && new Date(expireTime) < new Date()
}

function isExpiringWithin(expireTime, days) {
  if (!expireTime) return false
  const now = new Date()
  const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000)
  const target = new Date(expireTime)
  return target >= now && target <= end
}

function goTo(path) {
  if (!path) return
  router.push(path)
}

function goVpsDetail(id) {
  if (!id) return
  router.push('/resource/vps-detail/index/' + id)
}

function goCustomerDetail(id) {
  if (!id) return
  router.push('/member/customer-detail/index/' + id)
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trafficTrendChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard {
  min-height: 100%;
  padding: 16px;
  background: #f3f5f8;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.overview-card {
  cursor: pointer;
  border-radius: 4px;

  :deep(.el-card__body) {
    padding: 18px;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 14px;
  }

  &__title {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
  }

  &__main {
    min-height: 38px;
    font-size: 30px;
    line-height: 1.2;
    font-weight: 700;
    color: var(--el-text-color-primary);
    word-break: break-all;
  }

  &__metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 14px;
    margin-top: 14px;
    color: var(--el-text-color-secondary);
    font-size: 12px;

    b {
      margin-right: 4px;
      color: var(--el-text-color-primary);
      font-weight: 700;
    }
  }
}

.overview-card--success .overview-card__main { color: var(--el-color-success); }
.overview-card--warn .overview-card__main { color: var(--el-color-warning); }
.overview-card--traffic .overview-card__main { font-size: 25px; }
.overview-card--customer .overview-card__main { color: var(--el-color-primary); }

.dashboard-grid {
  display: grid;
  gap: 14px;
  margin-bottom: 14px;
}

.dashboard-grid--top {
  grid-template-columns: minmax(0, 2fr) minmax(360px, 1fr);
}

.dashboard-grid--rank,
.dashboard-grid--detail {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel-card {
  border-radius: 4px;

  :deep(.el-card__body) {
    padding: 16px;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.panel-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.trend-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: stretch;
}

.trend-chart {
  position: relative;
  min-width: 0;
  height: 400px;
}

.trend-chart__canvas {
  width: 100%;
  height: 100%;
}

.trend-rank {
  min-width: 0;
  height: 400px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafbfc;
  overflow: auto;
}

.trend-rank__title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.trend-rank__item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 48px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;

  &:last-child {
    border-bottom: 0;
  }

  div {
    min-width: 0;
    text-align: left;
  }

  strong,
  small {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    font-size: 13px;
    color: var(--el-text-color-primary);
  }

  small {
    margin-top: 3px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  em {
    font-style: normal;
    font-size: 12px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }
}

.period-card {
  height: 100%;
}

.period-card :deep(.el-card__body) {
  min-height: 414px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.period-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.period-summary {
  min-height: 92px;
  padding: 12px;
  text-align: left;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: #fff;
  cursor: pointer;

  span,
  strong,
  small {
    display: block;
  }

  span {
    font-size: 12px;
    font-weight: 700;
    color: var(--el-text-color-secondary);
  }

  strong {
    margin-top: 8px;
    font-size: 20px;
    line-height: 1.2;
    color: var(--el-text-color-primary);
    word-break: break-all;
  }

  small {
    margin-top: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.period-summary--active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.period-detail {
  flex: 1;
  min-height: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: #fafbfc;
}

.period-detail__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  span {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  strong {
    text-align: right;
    font-size: 18px;
    color: var(--el-text-color-primary);
  }
}

.period-detail__list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding-top: 12px;

  div {
    min-width: 0;
  }

  span,
  strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  strong {
    margin-top: 5px;
    font-size: 13px;
    color: var(--el-text-color-primary);
  }
}

.rank-card :deep(.el-table th.el-table__cell),
.detail-card :deep(.el-table th.el-table__cell) {
  font-size: 12px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
}

.rank-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-weight: 700;
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  text-align: left;
  align-items: flex-start;

  small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--el-text-color-secondary);
  }
}

.empty-state,
.empty-list {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.empty-state--overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.empty-list {
  min-height: 120px;
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: 4px;
}

.empty-list--compact {
  min-height: 220px;
}

.text-danger {
  color: var(--el-color-danger);
  font-weight: 600;
}

@media (max-width: 1280px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid--top,
  .dashboard-grid--rank,
  .dashboard-grid--detail {
    grid-template-columns: 1fr;
  }

  .trend-layout {
    grid-template-columns: 1fr;
  }

  .trend-rank {
    height: auto;
    max-height: 280px;
  }
}

@media (max-width: 720px) {
  .dashboard {
    padding: 10px;
  }

  .overview-grid,
  .period-grid,
  .period-detail__list {
    grid-template-columns: 1fr;
  }
}
</style>

<style lang="scss">
html.dark .app-container.dashboard {
  background: var(--el-bg-color);
}

html.dark .app-container.dashboard .period-summary,
html.dark .app-container.dashboard .period-detail,
html.dark .app-container.dashboard .trend-rank {
  background: var(--el-bg-color-overlay);
}
</style>
