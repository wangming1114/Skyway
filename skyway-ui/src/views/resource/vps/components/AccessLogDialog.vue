<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="1080px"
    append-to-body
    destroy-on-close
    class="access-log-dialog"
    @update:model-value="value => emit('update:modelValue', value)"
  >
    <div class="access-log-toolbar">
      <div class="access-log-status">
        <span class="status-dot" :class="`status-dot--${status}`" />
        <span>{{ statusText }}</span>
        <span v-if="errorMessage" class="error-message">{{ errorMessage }}</span>
      </div>
      <div class="access-log-actions">
        <el-button size="small" :disabled="status === 'connecting'" @click="toggleAutoScroll">
          {{ autoScroll ? '暂停滚动' : '恢复滚动' }}
        </el-button>
        <el-button size="small" @click="clearEntries">清空</el-button>
        <el-button size="small" type="primary" plain :loading="status === 'connecting'" @click="reconnect">重连</el-button>
      </div>
    </div>

    <el-table
      ref="tableRef"
      :data="entries"
      height="520"
      stripe
      border
      size="small"
      empty-text="暂无匹配的访问记录"
      row-key="_key"
    >
      <el-table-column label="时间" prop="timestamp" width="190" show-overflow-tooltip />
      <el-table-column label="节点" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="node-cell-main">{{ customerDisplay(row) || row.nodeName || row.inboundTag || '-' }}</div>
          <div v-if="row.customerName && row.nodeName" class="node-cell-sub">{{ row.nodeName }}</div>
          <div v-if="row.inboundTag && row.inboundTag !== row.nodeName" class="node-cell-sub">{{ row.inboundTag }}</div>
        </template>
      </el-table-column>
      <el-table-column label="协议" prop="protocol" width="100" show-overflow-tooltip />
      <el-table-column label="目标网站 / IP" prop="destinationHost" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="destination-host">{{ row.destinationHost || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="目标端口" prop="destinationPort" width="100" align="center">
        <template #default="{ row }">{{ row.destinationPort || '-' }}</template>
      </el-table-column>
    </el-table>

    <div class="access-log-summary">
      当前保留 {{ entries.length }} / {{ maxEntries }} 条
      <span v-if="!autoScroll"> · 已暂停自动滚动，日志仍在接收</span>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { getToken } from '@/utils/auth'
import { ACCESS_LOG_MAX_ENTRIES, appendAccessLogEntries, buildAccessLogWsUrl } from '@/utils/accessLog'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  scope: { type: String, required: true },
  instanceId: { type: [Number, String], default: null },
  nodeId: { type: [Number, String], default: null },
  title: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const maxEntries = ACCESS_LOG_MAX_ENTRIES
const entries = ref([])
const status = ref('closed')
const statusMessage = ref('未连接')
const errorMessage = ref('')
const autoScroll = ref(true)
const tableRef = ref(null)
let socket = null
let sequence = 0

const dialogTitle = computed(() => props.title || (props.scope === 'node' ? '代理节点访问日志' : 'VPS 访问日志'))
const statusText = computed(() => {
  if (status.value === 'connecting') return statusMessage.value || '正在连接'
  if (status.value === 'live') return statusMessage.value || '实时监控中'
  if (status.value === 'error') return '监控异常'
  return '已断开'
})

function withKeys(items) {
  return (items || []).map(item => ({ ...item, _key: `${Date.now()}-${++sequence}` }))
}

function appendEntries(items) {
  entries.value = appendAccessLogEntries(entries.value, withKeys(items), maxEntries)
  if (autoScroll.value) scrollToBottom()
}

function customerDisplay(row) {
  if (!row?.customerName) return ''
  return row.customerId ? `${row.customerName} (#${row.customerId})` : row.customerName
}

function scrollToBottom() {
  nextTick(() => tableRef.value?.setScrollTop?.(Number.MAX_SAFE_INTEGER))
}

function connect({ clear = true } = {}) {
  disconnect()
  if (!props.modelValue) return
  if (clear) entries.value = []
  errorMessage.value = ''
  status.value = 'connecting'
  statusMessage.value = '正在建立连接'
  let url
  try {
    url = buildAccessLogWsUrl({
      scope: props.scope,
      instanceId: props.instanceId,
      nodeId: props.nodeId,
      token: getToken() || '',
      locationLike: window.location,
      baseApi: import.meta.env.VITE_APP_BASE_API || ''
    })
  } catch (error) {
    status.value = 'error'
    errorMessage.value = error.message || '监控参数无效'
    return
  }

  const current = new WebSocket(url)
  socket = current
  current.onmessage = event => {
    if (socket !== current || typeof event.data !== 'string') return
    try {
      const payload = JSON.parse(event.data)
      if (payload.type === 'access_log_status') {
        status.value = payload.status || 'live'
        statusMessage.value = payload.message || ''
      } else if (payload.type === 'access_log_history') {
        entries.value = withKeys((payload.entries || []).slice(-maxEntries))
        if (autoScroll.value) scrollToBottom()
      } else if (payload.type === 'access_log_entry' && payload.entry) {
        appendEntries([payload.entry])
      } else if (payload.type === 'access_log_error') {
        status.value = 'error'
        errorMessage.value = payload.message || '访问日志监控失败'
      }
    } catch (_) {
      status.value = 'error'
      errorMessage.value = '收到无法解析的监控数据'
    }
  }
  current.onerror = () => {
    if (socket !== current) return
    status.value = 'error'
    if (!errorMessage.value) errorMessage.value = 'WebSocket 连接失败'
  }
  current.onclose = () => {
    if (socket !== current) return
    socket = null
    if (status.value !== 'error') {
      status.value = 'closed'
      statusMessage.value = '连接已关闭'
    }
  }
}

function disconnect() {
  const current = socket
  socket = null
  if (current) {
    current.onopen = null
    current.onmessage = null
    current.onerror = null
    current.onclose = null
    try { current.close() } catch (_) {}
  }
  if (status.value !== 'error') status.value = 'closed'
}

function reconnect() {
  connect({ clear: true })
}

function toggleAutoScroll() {
  autoScroll.value = !autoScroll.value
  if (autoScroll.value) scrollToBottom()
}

function clearEntries() {
  entries.value = []
}

watch(() => props.modelValue, visible => {
  if (visible) {
    autoScroll.value = true
    connect({ clear: true })
  } else {
    disconnect()
  }
})

onBeforeUnmount(disconnect)
</script>

<style scoped lang="scss">
.access-log-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.access-log-status,
.access-log-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
}
.status-dot--connecting { background: var(--el-color-warning); animation: access-log-pulse 1.2s infinite; }
.status-dot--live { background: var(--el-color-success); animation: access-log-pulse 1.5s infinite; }
.status-dot--error { background: var(--el-color-danger); }
.error-message { color: var(--el-color-danger); }
.node-cell-main,
.destination-host { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
.node-cell-sub { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 2px; }
.access-log-summary { margin-top: 10px; color: var(--el-text-color-secondary); font-size: 12px; }
@keyframes access-log-pulse { 50% { opacity: 0.35; } }
</style>
