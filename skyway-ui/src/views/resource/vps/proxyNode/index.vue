<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon class="mb8" style="margin-bottom: 12px">
      也可在「会员管理 → 客户详情」中为指定客户新增或管理关联节点。
    </el-alert>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="关键字" prop="nodeName">
        <el-input v-model="queryParams.nodeName" placeholder="节点名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="实例" prop="instanceId">
        <el-select v-model="queryParams.instanceId" placeholder="全部实例" clearable filterable style="width: 200px" @change="handleQuery">
          <el-option v-for="i in instanceOptions" :key="i.id" :label="i.name + (i.ip ? ' (' + i.ip + ')' : '')" :value="i.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select v-model="queryParams.customerId" placeholder="全部客户" clearable filterable style="width: 160px" @change="handleQuery">
          <el-option v-for="c in customerOptions" :key="c.id" :label="c.username" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" prop="nodeType">
        <el-select v-model="queryParams.nodeType" placeholder="全部类型" clearable style="width: 160px" @change="handleQuery">
          <el-option v-for="t in nodeTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 100px" @change="handleQuery">
          <el-option v-for="d in res_proxy_node_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="nodeList" border size="small">
      <el-table-column label="节点名称" prop="nodeName" min-width="120" show-overflow-tooltip />
      <el-table-column label="节点类型" prop="nodeType" width="140">
        <template #default="{ row }">
          <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地址" prop="address" min-width="100" show-overflow-tooltip />
      <el-table-column label="端口" prop="port" width="72" align="center" />
      <el-table-column label="所属实例" width="100" align="center">
        <template #default="{ row }">
          <el-link type="primary" @click="goVpsDetail(row.instanceId)" :underline="false">#{{ row.instanceId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="归属客户" width="100" align="center">
        <template #default="{ row }">
          <el-link v-if="row.customerId" type="primary" @click="goCustomerDetail(row.customerId)" :underline="false">#{{ row.customerId }}</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="120" align="center">
        <template #default="{ row }">
          <span v-if="!row.expireTime" class="expire-forever">永久</span>
          <span v-else :class="{ 'expire-expired': isExpired(row.expireTime) }">{{ parseTime(row.expireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="流量" width="200" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="trafficMap[row.id]">↑ {{ formatTraffic(trafficMap[row.id].totalTx) }} / ↓ {{ formatTraffic(trafficMap[row.id].totalRx) }}</span>
          <span v-else class="text-placeholder">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <span class="status-cell">
            <el-icon v-if="statusLoadingId === row.id" class="is-loading status-loading"><Loading /></el-icon>
            <el-switch
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              :disabled="statusLoadingId === row.id"
              @change="handleStatusChange(row)"
              v-hasPermi="['resource:vps:edit']"
            />
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="handleCopyUrl(row)">复制链接</el-button>
          <el-button link type="danger" size="small" icon="Delete" :loading="deleteLoadingId === row.id" @click="handleDelete(row)" v-hasPermi="['resource:vps:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="节点详情" v-model="detailVisible" width="620px" append-to-body destroy-on-close>
      <el-descriptions v-if="detailData" :column="2" border size="small">
        <el-descriptions-item label="节点名称">{{ detailData.nodeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="节点类型">
          <el-tag size="small" :type="getNodeTypeTagColor(detailData.nodeType)">{{ detailData.nodeType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="地址">{{ detailData.address }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ detailData.port }}</el-descriptions-item>
        <el-descriptions-item label="有效期" :span="2">
          <span v-if="!detailData.expireTime">永久有效</span>
          <span v-else :class="{ 'expire-expired': isExpired(detailData.expireTime) }">{{ detailData.expireTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="res_proxy_node_status" :value="detailData.status" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="流量统计" :span="2">
          <span v-if="detailTraffic">↑ {{ formatTraffic(detailTraffic.totalTx) }} / ↓ {{ formatTraffic(detailTraffic.totalRx) }}</span>
          <span v-else class="text-placeholder">-</span>
        </el-descriptions-item>
        <template v-if="detailConfig">
          <el-descriptions-item label="协议">{{ detailConfig.protocol || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailConfig.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="流控">{{ detailConfig.flow || '-' }}</el-descriptions-item>
          <el-descriptions-item label="传输协议">{{ detailConfig.network || '-' }}</el-descriptions-item>
          <el-descriptions-item label="安全层">{{ detailConfig.security || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SNI">{{ detailConfig.sni || '-' }}</el-descriptions-item>
          <el-descriptions-item label="指纹">{{ detailConfig.fingerprint || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公钥" :span="2">{{ detailConfig.publicKey || '-' }}</el-descriptions-item>
        </template>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detailData && detailData.url" class="share-url-section">
        <div class="share-url-label">分享链接</div>
        <div class="share-url-box">
          <el-input :model-value="detailData.url" readonly type="textarea" :rows="3" />
          <el-button type="primary" size="small" style="margin-top: 8px" @click="copyToClipboard(detailData.url)">
            <el-icon><DocumentCopy /></el-icon> 复制链接
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button link type="danger" @click="handleDelete(detailData); detailVisible = false" v-hasPermi="['resource:vps:remove']">删除</el-button>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProxyNodeList">
import { listProxyNode, updateProxyNode, delProxyNode, listInstance, getProxyNodeTraffic } from '@/api/resource/vps'
import { listCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'
import { DocumentCopy, Loading } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const { res_proxy_node_status } = proxy.useDict('res_proxy_node_status')
const router = useRouter()

const nodeTypeOptions = [{ value: 'VLESS-REALITY', label: 'VLESS-REALITY' }]

const showSearch = ref(true)
const loading = ref(false)
const nodeList = ref([])
const total = ref(0)
const instanceOptions = ref([])
const customerOptions = ref([])
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  nodeName: undefined,
  instanceId: undefined,
  customerId: undefined,
  nodeType: undefined,
  status: undefined
})

const statusLoadingId = ref(null)
const deleteLoadingId = ref(null)
const trafficMap = ref({})
const detailVisible = ref(false)
const detailData = ref(null)
const detailTraffic = ref(null)
const detailConfig = computed(() => {
  if (!detailData.value?.configJson) return null
  try { return JSON.parse(detailData.value.configJson) } catch { return null }
})

const queryRef = ref(null)

function getList() {
  loading.value = true
  listProxyNode(queryParams.value).then(res => {
    nodeList.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
    fetchTrafficForList(nodeList.value)
  }).catch(() => { loading.value = false })
}

function fetchTrafficForList(rows) {
  if (!rows || rows.length === 0) {
    trafficMap.value = {}
    return
  }
  trafficMap.value = {}
  rows.forEach(row => {
    if (!row.id) return
    getProxyNodeTraffic(row.id).then(r => {
      if (r.data) {
        trafficMap.value = { ...trafficMap.value, [row.id]: { totalRx: r.data.totalRx, totalTx: r.data.totalTx } }
      }
    }).catch(() => {})
  })
}

/** 流量展示：按大小自动选 B/KB/MB/GB，避免小流量在 GB 下显示 0.00 */
function formatTraffic(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.max(0, Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

function formatBytes(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

function loadOptions() {
  listInstance({ pageNum: 1, pageSize: 500 }).then(res => { instanceOptions.value = res.rows || [] }).catch(() => {})
  listCustomer({ pageNum: 1, pageSize: 500 }).then(res => { customerOptions.value = res.rows || [] }).catch(() => {})
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  handleQuery()
}

function goVpsDetail(instanceId) {
  router.push({ path: '/resource/vps-detail/index/' + instanceId })
}

function goCustomerDetail(customerId) {
  router.push({ path: '/member/customer-detail/index/' + customerId })
}

function handleDetail(row) {
  detailData.value = { ...row }
  detailTraffic.value = null
  detailVisible.value = true
  getProxyNodeTraffic(row.id).then(r => {
    detailTraffic.value = r.data
  }).catch(() => {})
}

function handleCopyUrl(row) {
  if (!row.url) {
    proxy.$modal.msgWarning('该节点暂无分享链接')
    return
  }
  copyToClipboard(row.url)
}

function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess('已复制到剪贴板')
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    proxy.$modal.msgSuccess('已复制到剪贴板')
  })
}

function handleStatusChange(row) {
  const text = row.status === '0' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}节点"${row.nodeName}"吗？`).then(() => {
    statusLoadingId.value = row.id
    return updateProxyNode({ id: row.id, status: row.status })
  }).then(() => {
    proxy.$modal.msgSuccess(`${text}成功`)
  }).catch(() => {
    row.status = row.status === '0' ? '1' : '0'
  }).finally(() => {
    statusLoadingId.value = null
  })
}

function handleDelete(row) {
  if (!row?.id) return
  proxy.$modal.confirm(`确认要删除节点"${row.nodeName}"吗？将同时在服务器上删除配置。`).then(() => {
    deleteLoadingId.value = row.id
    return delProxyNode(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    detailVisible.value = false
    getList()
  }).catch(() => {}).finally(() => {
    deleteLoadingId.value = null
  })
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

onMounted(() => {
  loadOptions()
  getList()
})
</script>

<style scoped>
.expire-forever { color: var(--el-color-success); font-weight: 500; }
.expire-expired { color: var(--el-color-danger); font-weight: 500; }
.share-url-section { margin-top: 16px; padding: 12px; background: var(--el-fill-color-light); border-radius: 6px; }
.share-url-label { font-size: 13px; font-weight: 500; margin-bottom: 8px; }
.share-url-box { display: flex; flex-direction: column; }
.status-cell { display: inline-flex; align-items: center; gap: 6px; }
.status-loading { font-size: 14px; margin-right: 2px; }
.text-placeholder { color: var(--el-text-color-placeholder); }
</style>
