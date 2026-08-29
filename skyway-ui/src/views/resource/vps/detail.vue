<template>
  <div class="app-container">
    <el-card v-loading="loading" class="box-card">
      <template #header>
        <div class="detail-card-header">
          <span>VPS 详情</span>
          <span class="detail-card-actions">
          <el-button type="primary" link icon="Connection" @click="openConnect" v-hasPermi="['resource:vps:list']">连接服务器</el-button>
          <el-button type="primary" link icon="View" @click="accessLogVisible = true" v-hasPermi="['resource:vps:list', 'resource:vps:query']">访问日志</el-button>
          <el-dropdown trigger="click" @command="handleDetailCommand">
            <el-button type="primary" link icon="DArrowRight">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="back" icon="Back">返回</el-dropdown-item>
                <el-dropdown-item command="delete" icon="Delete" divided v-hasPermi="['resource:vps:remove']">删除</el-dropdown-item>
                <el-dropdown-item command="forceDelete" icon="DeleteFilled" v-hasPermi="['resource:vps:remove']">强制删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          </span>
        </div>
      </template>
      <div v-if="detail" class="detail-section">
        <el-descriptions title="基础信息" :column="isMobile ? 1 : 2" border>
          <el-descriptions-item label="VPS名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <dict-tag :options="res_instance_status" :value="detail.status" />
          </el-descriptions-item>
          <el-descriptions-item label="网络类型">
            <dict-tag :options="res_instance_network_type" :value="detail.networkType" />
          </el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="节点">{{ detail.nodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SSH端口">{{ detail.sshPort != null ? detail.sshPort : '-' }}</el-descriptions-item>
          <el-descriptions-item label="SSH账号">{{ detail.sshUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SSH密码" :span="2">
            <span v-if="detail.sshPassword">{{ showPassword ? detail.sshPassword : '******' }}</span>
            <span v-else>-</span>
            <el-button v-if="detail.sshPassword" link type="primary" size="small" @click="showPassword = !showPassword" style="margin-left: 8px">
              {{ showPassword ? '隐藏' : '显示' }}
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="CPU">{{ detail.cpu || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内存">{{ detail.memory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="磁盘">{{ detail.disk || '-' }}</el-descriptions-item>
          <el-descriptions-item label="流量限制">{{ detail.trafficLimit != null && detail.trafficLimit > 0 ? formatTraffic(detail.trafficLimit) : '不限' }}</el-descriptions-item>
          <el-descriptions-item label="续费金额">{{ detail.renewalAmount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="到期时间">
            <span v-if="!detail.expireTime">-</span>
            <span v-else :class="{ 'expire-expired': isExpired(detail.expireTime) }">{{ parseTime(detail.expireTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-divider />
      <el-tabs v-model="activeTab">
        <el-tab-pane label="节点管理" name="proxyNode">
          <ProxyNodePanel
            v-if="detail"
            :instance-id="detail.id"
            :default-address="detail.ip"
            :ws-connected="wsConnected"
            :send-ws="sendWs"
            @register-handler="setProxyNodeHandler"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <AccessLogDialog
      v-model="accessLogVisible"
      scope="vps"
      :instance-id="detail?.id"
      :title="`${detail?.name || detail?.ip || 'VPS'} - 访问日志`"
    />

  </div>
</template>

<script setup name="VpsDetail">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { delInstance, forceDelInstance, getInstance } from '@/api/resource/vps'
import ProxyNodePanel from './components/ProxyNodePanel.vue'
import AccessLogDialog from './components/AccessLogDialog.vue'
import { getToken } from '@/utils/auth'
import useAppStore from '@/store/modules/app'

const { proxy } = getCurrentInstance()
const { res_instance_status, res_instance_network_type } = proxy.useDict('res_instance_status', 'res_instance_network_type')
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const isMobile = computed(() => appStore.device === 'mobile')

const loading = ref(true)
const detail = ref(null)
const activeTab = ref('proxyNode')
const showPassword = ref(false)
const accessLogVisible = ref(false)

const wsRef = ref(null)
const wsConnected = ref(false)
const proxyNodeMessageHandler = ref(null)
function setProxyNodeHandler(fn) {
  proxyNodeMessageHandler.value = fn
}

function getWsUrl(instanceId) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = location.host
  const path = base + '/ws/ssh'
  const token = getToken() || ''
  return `${protocol}//${host}${path}?instanceId=${instanceId}&token=${encodeURIComponent(token)}`
}

function connectWs() {
  const id = detail.value?.id
  if (!id) return
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) return
  const url = getWsUrl(id)
  const socket = new WebSocket(url)
  wsRef.value = socket
  wsConnected.value = false
  socket.onopen = () => {
    wsConnected.value = true
  }
  socket.onmessage = (ev) => {
    if (typeof ev.data !== 'string') return
    try {
      const obj = JSON.parse(ev.data)
      const t = obj && obj.type
      if (t === 'exec_output' || t === 'exec_end' || t === 'exec_error' || t === 'node_created') {
        if (proxyNodeMessageHandler.value) proxyNodeMessageHandler.value(obj)
      }
    } catch (_) {}
  }
  socket.onerror = () => {
    if (!wsConnected.value) wsConnected.value = false
  }
  socket.onclose = () => {
    wsRef.value = null
    wsConnected.value = false
  }
}

function sendWs(obj) {
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) {
    wsRef.value.send(JSON.stringify(obj))
  }
}

function openConnect() {
  if (detail.value?.id) {
    router.push({
      name: 'VpsTerminal',
      params: { id: detail.value.id },
      query: { name: detail.value.name || '', ip: detail.value.ip || '' }
    })
  }
}

function handleDetailCommand(command) {
  if (command === 'back') goBack()
  else if (command === 'delete') handleDelete()
  else if (command === 'forceDelete') handleForceDelete()
}

function handleDelete() {
  if (!detail.value?.id) return
  proxy.$modal.confirm(`是否确认删除 VPS "${detail.value.name || detail.value.ip || detail.value.id}"？`).then(() => {
    return delInstance(detail.value.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    goBack()
  }).catch(() => {})
}

function handleForceDelete() {
  if (!detail.value?.id) return
  proxy.$modal.confirm(`确认要强制删除 VPS "${detail.value.name || detail.value.ip || detail.value.id}" 吗？此操作只删除本地 VPS、节点和流量记录，不连接服务器，也不会清理服务器上的残留配置。`).then(() => {
    return forceDelInstance(detail.value.id)
  }).then(() => {
    proxy.$modal.msgSuccess('强制删除成功')
    goBack()
  }).catch(() => {})
}

function loadDetail() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  getInstance(id).then(res => {
    detail.value = res.data
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

watch(detail, (v) => {
  if (v && v.id) connectWs()
}, { immediate: true })

onBeforeUnmount(() => {
  if (wsRef.value) {
    wsRef.value.close()
    wsRef.value = null
  }
  wsConnected.value = false
})

function formatTraffic(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.max(0, Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}
function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}

function goBack() {
  router.push({ path: '/resource/vps', query: route.query })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.detail-section {
  margin-bottom: 16px;
}
.detail-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.detail-card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.expire-expired {
  color: var(--el-color-danger);
  font-weight: 500;
}
@media (max-width: 992px) {
  .box-card :deep(.el-card__header) { padding: 12px; }
  .detail-card-header { align-items: flex-start; flex-direction: column; }
  .detail-card-actions { justify-content: flex-start; }
  :deep(.el-descriptions__cell) { padding: 8px 10px !important; }
  :deep(.el-descriptions__label) { width: 92px; }
}
</style>
