<template>
  <div class="app-container dashboard">
    <section class="dashboard-section">
      <el-row :gutter="15" class="dashboard-row">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--primary" @click="goTo('/resource/vps')" v-hasPermi="['resource:vps:list']">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.totalVps }}</span>
              <span class="kpi-label">VPS 实例</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--success" @click="goTo('/resource/vps')" v-hasPermi="['resource:vps:list']">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.runningVps }}</span>
              <span class="kpi-label">运行中</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--primary" @click="goTo('/resource/vps')" v-hasPermi="['resource:vps:list']">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.totalNodes }}</span>
              <span class="kpi-label">代理节点</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--primary" @click="goTo('/member/customer')" v-hasPermi="['member:customer:list']">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.totalCustomers }}</span>
              <span class="kpi-label">客户数</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="15" class="dashboard-row">
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--sub">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.stoppedVps }}</span>
              <span class="kpi-label">VPS 已停止</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--sub kpi-card--warn">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.abnormalVps }}</span>
              <span class="kpi-label">VPS 异常</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--sub">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.normalNodes }}</span>
              <span class="kpi-label">节点正常</span>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6" :lg="6">
          <el-card shadow="hover" class="kpi-card kpi-card--sub">
            <div class="kpi-inner">
              <span class="kpi-value">{{ summary.disabledNodes }}</span>
              <span class="kpi-label">节点停用</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="dashboard-section">
      <el-row :gutter="15" class="dashboard-row chart-row">
        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <el-card shadow="hover" class="chart-card chart-card-fixed block-card">
            <template #header>
              <div class="block-card-header">
                <span class="block-card-title">近 7 日流量趋势</span>
              </div>
            </template>
            <div class="chart-placeholder" ref="trafficChartRef">
              <div v-if="!trafficTrend.length" class="chart-empty">暂无数据，需后端概览接口支持</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12" :lg="12" class="chart-right-col">
          <div class="chart-half-row">
            <el-card shadow="hover" class="chart-card chart-card-half block-card">
              <template #header>
                <div class="block-card-header">
                  <span class="block-card-title">协议分布</span>
                </div>
              </template>
              <div class="chart-half-wrap">
                <div v-if="protocolChartEmpty" class="chart-empty-inner">暂无数据</div>
                <div v-else class="chart-half" ref="protocolChartRef"></div>
              </div>
            </el-card>
            <el-card shadow="hover" class="chart-card chart-card-half block-card">
              <template #header>
                <div class="block-card-header">
                  <span class="block-card-title">节点状态</span>
                </div>
              </template>
              <div class="chart-half-wrap">
                <div v-if="nodeStatusChartEmpty" class="chart-empty-inner">暂无数据</div>
                <div v-else class="chart-half" ref="nodeStatusChartRef"></div>
              </div>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="dashboard-section">
      <el-row :gutter="15" class="dashboard-row">
        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <el-card shadow="hover" class="table-card block-card">
            <template #header>
              <div class="block-card-header">
                <span class="block-card-title">最近节点</span>
                <el-button link type="primary" size="small" class="block-card-action" @click="goTo('/resource/vps')" v-hasPermi="['resource:vps:list']">查看全部</el-button>
              </div>
            </template>
            <div class="table-card-body">
              <el-table
                v-loading="recentLoading"
                :data="recentNodes"
                size="small"
                stripe
                max-height="280"
                v-hasPermi="['resource:vps:list']"
              >
                <el-table-column label="节点名称" prop="nodeName" min-width="100" show-overflow-tooltip />
                <el-table-column label="类型" prop="nodeType" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="流量" width="140" align="center">
                  <template #default="{ row }">
                    <span v-if="trafficMap[row.id]">↑ {{ formatTraffic(trafficMap[row.id].totalTx) }} / ↓ {{ formatTraffic(trafficMap[row.id].totalRx) }}</span>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="goVpsDetail(row.instanceId)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!recentNodes.length && !recentLoading" class="table-empty">暂无节点数据</div>
              <div v-else-if="recentNodes.length && recentNodesTotalTraffic > 0" class="table-footer">
                本页 {{ recentNodes.length }} 个节点合计流量：{{ formatTraffic(recentNodesTotalTraffic) }}
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <el-card shadow="hover" class="table-card block-card">
            <template #header>
              <div class="block-card-header">
                <span class="block-card-title">即将到期（30 天内）</span>
                <el-button link type="primary" size="small" class="block-card-action" @click="goTo('/resource/vps')" v-hasPermi="['resource:vps:list']">查看全部</el-button>
              </div>
            </template>
            <div class="table-card-body">
              <el-table
                v-loading="expiringLoading"
                :data="expiringSoon"
                size="small"
                stripe
                max-height="280"
                v-hasPermi="['resource:vps:list']"
              >
                <el-table-column label="节点名称" prop="nodeName" min-width="100" show-overflow-tooltip />
                <el-table-column label="到期日" width="110" align="center">
                  <template #default="{ row }">
                    <span :class="{ 'expire-expired': isExpired(row.expireTime) }">{{ parseTime(row.expireTime, '{y}-{m}-{d}') }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="归属客户" min-width="100" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ customerLabel(row.customerId) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="goVpsDetail(row.instanceId)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!expiringSoon.length && !expiringLoading" class="table-empty">暂无即将到期节点</div>
              <div v-else-if="expiringCountInSample > 0" class="table-footer">
                前 200 条中 30 天内到期共 {{ expiringCountInSample }} 个，下表展示前 10 个
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup name="Index">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import * as echarts from 'echarts'
import { getDashboardSummary, listProxyNode, getProxyNodeTraffic } from '@/api/resource/vps'
import { listCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'

const router = useRouter()
const { proxy } = getCurrentInstance()

const summary = ref({
  totalVps: 0,
  runningVps: 0,
  stoppedVps: 0,
  abnormalVps: 0,
  totalNodes: 0,
  normalNodes: 0,
  disabledNodes: 0,
  totalCustomers: 0
})

const trafficTrend = ref([])
const trafficChartRef = ref(null)
const protocolChartRef = ref(null)
const nodeStatusChartRef = ref(null)
let protocolChart = null
let nodeStatusChart = null

const recentNodes = ref([])
const recentLoading = ref(false)
const trafficMap = ref({})
const customerOptions = ref([])

const expiringSoon = ref([])
const expiringLoading = ref(false)
const expiringCountInSample = ref(0)
const protocolHasData = ref(false)

const protocolChartEmpty = computed(() => !protocolHasData.value)
const nodeStatusChartEmpty = computed(() => summary.value.totalNodes === 0)

const recentNodesTotalTraffic = computed(() => {
  let rx = 0
  let tx = 0
  recentNodes.value.forEach(row => {
    const t = trafficMap.value[row.id]
    if (t) {
      rx += Number(t.totalRx) || 0
      tx += Number(t.totalTx) || 0
    }
  })
  return rx + tx
})

function formatTraffic(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.max(0, Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

function getNodeTypeTagColor(nodeType) {
  if (!nodeType) return ''
  if (nodeType.startsWith('VLESS')) return ''
  if (nodeType.startsWith('VMess')) return 'success'
  if (nodeType.startsWith('Trojan')) return 'warning'
  return 'info'
}

function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}

function customerLabel(customerId) {
  if (!customerId) return '-'
  const c = customerOptions.value.find(x => x.id === customerId)
  return c ? c.username : '#' + customerId
}

function goTo(path) {
  router.push(path)
}

function goVpsDetail(instanceId) {
  if (!instanceId) return
  router.push({ path: '/resource/vps-detail/index/' + instanceId })
}

function loadSummary() {
  return getDashboardSummary().then(r => {
    if (r.data) {
      summary.value.totalVps = r.data.totalVps ?? 0
      summary.value.runningVps = r.data.runningVps ?? 0
      summary.value.stoppedVps = r.data.stoppedVps ?? 0
      summary.value.abnormalVps = r.data.abnormalVps ?? 0
      summary.value.totalNodes = r.data.totalNodes ?? 0
      summary.value.normalNodes = r.data.normalNodes ?? 0
      summary.value.disabledNodes = r.data.disabledNodes ?? 0
      summary.value.totalCustomers = r.data.totalCustomers ?? 0
    }
  }).catch(() => {})
}

function loadRecentNodes() {
  recentLoading.value = true
  listProxyNode({ pageNum: 1, pageSize: 10 }).then(res => {
    recentNodes.value = res.rows || []
    recentLoading.value = false
    trafficMap.value = {}
    recentNodes.value.forEach(row => {
      if (!row.id) return
      getProxyNodeTraffic(row.id).then(r => {
        if (r.data) {
          trafficMap.value = { ...trafficMap.value, [row.id]: { totalRx: r.data.totalRx, totalTx: r.data.totalTx } }
        }
      }).catch(() => {})
    })
  }).catch(() => { recentLoading.value = false })
}

function loadExpiringSoon() {
  expiringLoading.value = true
  const now = new Date()
  const end = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  listProxyNode({ pageNum: 1, pageSize: 200 }).then(res => {
    const rows = res.rows || []
    const filtered = rows
      .filter(n => n.expireTime && new Date(n.expireTime) >= now && new Date(n.expireTime) <= end)
      .sort((a, b) => new Date(a.expireTime) - new Date(b.expireTime))
    expiringCountInSample.value = filtered.length
    expiringSoon.value = filtered.slice(0, 10)
    expiringLoading.value = false
  }).catch(() => { expiringLoading.value = false })
}

function loadProtocolDistribution() {
  listProxyNode({ pageNum: 1, pageSize: 500 }).then(res => {
    const rows = res.rows || []
    const map = {}
    rows.forEach(n => {
      const t = n.nodeType || '未知'
      map[t] = (map[t] || 0) + 1
    })
    const data = Object.entries(map).map(([name, value]) => ({ name, value }))
    protocolHasData.value = data.length > 0
    nextTick(() => renderProtocolChart(data))
  }).catch(() => {})
}

function renderProtocolChart(data) {
  if (!protocolChartRef.value) return
  if (!protocolChart) protocolChart = echarts.init(protocolChartRef.value, 'macarons')
  protocolChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{ name: '协议', type: 'pie', radius: ['40%', '70%'], data, emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' } } }]
  })
  nextTick(() => protocolChart?.resize())
}

function renderNodeStatusChart() {
  if (!nodeStatusChartRef.value) return
  if (!nodeStatusChart) nodeStatusChart = echarts.init(nodeStatusChartRef.value, 'macarons')
  const total = summary.value.totalNodes
  const normal = summary.value.normalNodes
  const disabled = summary.value.disabledNodes
  nodeStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      name: '节点状态',
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: normal, name: '正常', itemStyle: { color: '#67c23a' } },
        { value: disabled, name: '停用', itemStyle: { color: '#909399' } }
      ].filter(d => d.value > 0),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' } }
    }]
  })
  nextTick(() => nodeStatusChart?.resize())
}

function loadCustomerOptions() {
  listCustomer({ pageNum: 1, pageSize: 500 }).then(res => { customerOptions.value = res.rows || [] }).catch(() => {})
}

function initCharts() {
  loadProtocolDistribution()
  renderNodeStatusChart()
}

function handleResize() {
  protocolChart?.resize()
  nodeStatusChart?.resize()
}

onMounted(() => {
  loadSummary().then(() => nextTick(() => renderNodeStatusChart()))
  loadRecentNodes()
  loadExpiringSoon()
  loadCustomerOptions()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  protocolChart?.dispose()
  nodeStatusChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 15px;
  background: #eff2f5;
  min-height: 100%;
}
.dashboard-section {
  margin-bottom: 15px;
  &:last-child { margin-bottom: 0; }
}
.dashboard-row {
  margin-bottom: 15px;
  align-items: stretch;
}
.dashboard-row:last-child { margin-bottom: 0; }

.dashboard :deep(.el-card) {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02), 0 4px 8px rgba(0, 0, 0, 0.02);
  border-radius: 2px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06), 0 8px 16px rgba(0, 0, 0, 0.04);
    transform: translateY(-2px);
  }
}
.dashboard :deep(.el-card__body) { padding: 24px; box-sizing: border-box; }
.dashboard :deep(.el-card__header) {
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  font-weight: 600;
}

.block-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.block-card-title { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); }
.block-card-action { font-size: 13px; font-weight: 500; }

.kpi-card {
  cursor: pointer;
  height: 100%;
  min-height: 100px;
  :deep(.el-card__body) { padding: 20px; }
  .kpi-inner {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    min-height: 56px;
    gap: 8px;
  }
  .kpi-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    letter-spacing: -0.02em;
    line-height: 1.2;
  }
  .kpi-label { font-size: 13px; color: var(--el-text-color-secondary); font-weight: 500; }
  &.kpi-card--primary .kpi-value { color: var(--el-text-color-primary); }
  &.kpi-card--success .kpi-value { color: var(--el-color-success); }
  &.kpi-card--sub {
    min-height: 88px;
    .kpi-value { font-size: 22px; font-weight: 600; }
    .kpi-label { font-size: 12px; }
  }
  &.kpi-card--warn .kpi-value { color: var(--el-color-warning); }
}

.chart-row .el-col { display: flex; align-items: stretch; }
.chart-right-col { min-width: 0; }
.chart-half-row {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  width: 100%;
  min-width: 0;
  .chart-card-half {
    flex: 1 1 280px;
    min-width: 0;
    max-width: 100%;
    min-height: 320px;
  }
}
.chart-card {
  width: 100%;
  margin-bottom: 0;
  &.chart-card-fixed {
    min-height: 320px;
    display: flex;
    flex-direction: column;
    :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; min-height: 0; padding: 24px; }
  }
  &.chart-card-half :deep(.el-card__body) {
    padding: 24px;
    height: 260px;
    overflow: hidden;
  }
}
.chart-half-wrap {
  width: 100%;
  height: 260px;
  min-height: 260px;
  position: relative;
  background: #f8f9fb;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(0, 0, 0, 0.06);
}
.chart-empty-inner { color: var(--el-text-color-placeholder); font-size: 13px; }
.chart-half {
  position: absolute;
  left: 0; top: 0;
  width: 100%; height: 100%;
  box-sizing: border-box;
}
.chart-placeholder {
  height: 260px;
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fb;
  border-radius: 2px;
  border: 1px dashed rgba(0, 0, 0, 0.06);
}
.chart-empty { color: var(--el-text-color-placeholder); font-size: 14px; }

.table-card {
  height: 100%;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; min-height: 0; padding: 24px; }
  .table-card-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }
  :deep(.el-table) {
    flex: 1;
    --el-table-border-color: rgba(0, 0, 0, 0.06);
    --el-table-header-bg-color: #fafbfc;
  }
  :deep(.el-table th.el-table__cell) { font-weight: 600; font-size: 12px; color: var(--el-text-color-secondary); }
  :deep(.el-table td.el-table__cell) { font-size: 13px; }
  :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) { background: #fafbfc; }
}
.table-empty {
  text-align: center;
  color: var(--el-text-color-placeholder);
  padding: 32px 24px;
  font-size: 13px;
  background: #f8f9fb;
  border-radius: 2px;
  margin-top: 8px;
}
.table-footer {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f8f9fb;
  border-radius: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.text-muted { color: var(--el-text-color-placeholder); }
.expire-expired { color: var(--el-color-danger); font-weight: 500; }
</style>
