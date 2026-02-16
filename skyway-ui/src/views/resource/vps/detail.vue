<template>
  <div class="app-container">
    <el-card v-loading="loading" class="box-card">
      <template #header>
        <span>VPS 详情</span>
        <span style="float: right">
          <el-button type="primary" link icon="Connection" @click="openConnect" v-hasPermi="['resource:vps:list']">连接服务器</el-button>
          <el-button type="primary" link icon="Back" @click="goBack">返回</el-button>
        </span>
      </template>
      <div v-if="detail" class="detail-section">
        <el-descriptions title="基础信息" :column="2" border>
          <el-descriptions-item label="VPS名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="VPS ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <dict-tag :options="res_instance_status" :value="detail.status" />
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

  </div>
</template>

<script setup name="VpsDetail">
import { ref, watch, onBeforeUnmount } from 'vue'
import { getInstance } from '@/api/resource/vps'
import ProxyNodePanel from './components/ProxyNodePanel.vue'
import { getToken } from '@/utils/auth'

const { proxy } = getCurrentInstance()
const { res_instance_status } = proxy.useDict('res_instance_status')
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref(null)
const activeTab = ref('proxyNode')
const showPassword = ref(false)

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
      query: { name: detail.value.name || '' }
    })
  }
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
</style>
