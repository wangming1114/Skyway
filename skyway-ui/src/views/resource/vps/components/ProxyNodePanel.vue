<template>
  <div class="proxy-node-panel">
    <div class="toolbar">
      <el-button type="primary" size="small" icon="Plus" @click="handleAdd" v-hasPermi="['resource:vps:add']">新增节点</el-button>
      <el-button type="danger" plain size="small" icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete" v-hasPermi="['resource:vps:remove']">批量删除</el-button>
      <el-select v-model="queryParams.nodeType" placeholder="全部类型" clearable size="small" style="width: 180px; margin-left: 10px" @change="getList">
        <el-option v-for="t in nodeTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-button icon="Refresh" size="small" circle style="margin-left: 8px" @click="getList" />
    </div>

    <el-table v-loading="loading" :data="nodeList" border size="small" style="margin-top: 10px" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="节点名称" prop="nodeName" min-width="120" show-overflow-tooltip />
      <el-table-column label="节点类型" prop="nodeType" width="160">
        <template #default="{ row }">
          <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="归属客户" width="120" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          {{ customerOptions.find(c => c.id === row.customerId)?.username ?? row.customerId ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="地址" prop="address" min-width="100" show-overflow-tooltip />
      <el-table-column label="端口" prop="port" width="80" align="center" />
      <el-table-column label="有效期" width="160" align="center">
        <template #default="{ row }">
          <span v-if="!row.expireTime" class="expire-forever">永久</span>
          <span v-else :class="{ 'expire-expired': isExpired(row.expireTime) }">{{ parseTime(row.expireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="流量" min-width="260" align="center">
        <template #default="{ row }">
          <div v-if="trafficMap[row.id]" class="traffic-cell">
            <div class="traffic-cell-total">{{ nodeTrafficTotalText(row) }}</div>
            <div class="traffic-cell-speed">{{ nodeRealtimeSpeedText(row) }}</div>
          </div>
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
      <el-table-column label="备注" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="hasEditPermi" class="remark-cell-editable" @click.stop="openRemarkEdit(row)">
            <span class="remark-cell-text">{{ row.remark || '点击添加备注' }}</span>
            <el-icon class="remark-cell-icon"><Edit /></el-icon>
          </span>
          <span v-else>{{ row.remark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="openNodeEdit(row)" v-hasPermi="['resource:vps:edit']">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleCopyUrl(row)">复制链接</el-button>
          <el-button link type="danger" size="small" icon="Delete" :loading="deletingNodeId === row.id || (batchDeleteLoading && selectedIds.includes(row.id))" @click="handleDelete(row)" v-hasPermi="['resource:vps:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新增节点" v-model="addDialogVisible" width="460px" append-to-body>
      <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="90px">
        <el-form-item label="归属客户" prop="customerId">
          <el-select v-model="addForm.customerId" placeholder="请选择客户" style="width: 100%" filterable>
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.username" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="协议类型" prop="nodeType">
          <el-select v-model="addForm.nodeType" placeholder="请选择协议类型" style="width: 100%" class="add-form-node-type">
            <el-option
              v-for="t in allNodeTypesForAdd"
              :key="t.value"
              :label="t.label"
              :value="t.value"
              :disabled="!t.enabled"
            >
              <span>{{ t.label }}</span>
              <span v-if="!t.enabled" class="add-form-type-tip">即将支持</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="addForm.port" :min="1" :max="65535" controls-position="right" style="width: 100%" placeholder="如 5000" />
        </el-form-item>
        <el-form-item label="有效期">
          <div style="display: flex; align-items: center; gap: 10px; width: 100%">
            <el-date-picker
              v-model="addForm.expireTime"
              type="datetime"
              placeholder="选择过期时间"
              :disabled="addFormPermanent"
              style="flex: 1"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <el-checkbox v-model="addFormPermanent" @change="v => v && (addForm.expireTime = null)">永久有效</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="选填，便于区分节点" maxlength="200" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" :disabled="!wsConnected" @click="submitAddForm">
          {{ wsConnected ? '确定（将在服务器执行）' : '等待 SSH 连接...' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="addLogDrawerVisible"
      :title="execLogTitle"
      direction="rtl"
      size="520"
      :close-on-click-modal="!addLogRunning"
    >
      <div class="add-log-body">
        <div v-if="addLogRunning" class="add-log-status">执行中…</div>
        <div v-else-if="addLogExitCode != null" :class="['add-log-status', addLogExitCode === 0 ? 'success' : 'fail']">
          {{ addLogExitCode === 0 ? '执行成功，节点已保存' : '执行失败，请查看下方日志' }}
        </div>
        <pre ref="addLogRef" class="add-log-pre">{{ addLog }}</pre>
      </div>
    </el-drawer>

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
          <div v-if="detailTraffic" class="traffic-cell traffic-cell--detail">
            <div class="traffic-cell-total">{{ nodeTrafficTotalText(detailData, detailTraffic) }}</div>
            <div class="traffic-cell-speed">{{ nodeRealtimeSpeedText(detailData) }}</div>
          </div>
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

    <el-dialog title="修改备注" v-model="remarkEditVisible" width="420px" append-to-body destroy-on-close @closed="remarkEditRow = null">
      <el-input v-model="remarkEditValue" type="textarea" :rows="3" placeholder="选填" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="remarkEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="remarkSaving" @click="submitRemarkEdit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="编辑节点" v-model="editNodeVisible" width="520px" append-to-body destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="有效期">
          <div style="display: flex; align-items: center; gap: 10px; width: 100%">
            <el-date-picker
              v-model="editNodeForm.expireTime"
              type="datetime"
              placeholder="选择过期时间"
              :disabled="editNodePermanent"
              style="flex: 1"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <el-checkbox v-model="editNodePermanent" @change="v => v && (editNodeForm.expireTime = null)">永久有效</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="订阅链接">
          <el-input v-model="editNodeForm.url" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="支持手动编辑" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editNodeVisible = false">取消</el-button>
        <el-button type="primary" :loading="editNodeSaving" @click="submitNodeEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import useUserStore from '@/store/modules/user'
import { listProxyNode, updateProxyNode, delProxyNode, getProxyNodeTraffic, getRecommendPort, getInstanceSpeed } from '@/api/resource/vps'
import { listCustomer } from '@/api/member/customer'
import { DocumentCopy, Loading, Edit, Delete } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const { res_proxy_node_status } = proxy.useDict('res_proxy_node_status')
const userStore = useUserStore()
const hasEditPermi = computed(() => (userStore.permissions || []).some(p => p === '*:*:*' || p === 'resource:vps:edit'))

const props = defineProps({
  instanceId: { type: Number, required: true },
  defaultAddress: { type: String, default: '' },
  wsConnected: { type: Boolean, default: false },
  sendWs: { type: Function, default: () => {} }
})
const emit = defineEmits(['register-handler'])

const nodeTypeOptions = [
  { value: 'VLESS-REALITY', label: 'VLESS-REALITY' },
  { value: 'VMess-TCP', label: 'VMess-TCP' }
]

const allNodeTypesForAdd = [
  { value: 'VLESS-REALITY', label: 'VLESS-REALITY', enabled: true },
  { value: 'VMess-TCP', label: 'VMess-TCP', enabled: true },
  { value: 'VLESS-HTTP2-REALITY', label: 'VLESS-HTTP2-REALITY', enabled: false },
  { value: 'VLESS-H2-TLS', label: 'VLESS-H2-TLS', enabled: false },
  { value: 'VLESS-WS-TLS', label: 'VLESS-WS-TLS', enabled: false },
  { value: 'VLESS-HTTPUpgrade-TLS', label: 'VLESS-HTTPUpgrade-TLS', enabled: false },
  { value: 'VMess-WS', label: 'VMess-WS', enabled: false },
  { value: 'VMess-HTTP', label: 'VMess-HTTP', enabled: false },
  { value: 'VMess-QUIC', label: 'VMess-QUIC', enabled: false },
  { value: 'VMess-WS-TLS', label: 'VMess-WS-TLS', enabled: false },
  { value: 'VMess-H2-TLS', label: 'VMess-H2-TLS', enabled: false },
  { value: 'VMess-HTTPUpgrade-TLS', label: 'VMess-HTTPUpgrade-TLS', enabled: false },
  { value: 'Trojan', label: 'Trojan', enabled: false },
  { value: 'Trojan-H2-TLS', label: 'Trojan-H2-TLS', enabled: false },
  { value: 'Trojan-WS-TLS', label: 'Trojan-WS-TLS', enabled: false },
  { value: 'Trojan-HTTPUpgrade-TLS', label: 'Trojan-HTTPUpgrade-TLS', enabled: false },
  { value: 'TUIC', label: 'TUIC', enabled: false },
  { value: 'Hysteria2', label: 'Hysteria2', enabled: false },
  { value: 'Shadowsocks', label: 'Shadowsocks', enabled: false },
  { value: 'Socks', label: 'Socks', enabled: false },
]

const loading = ref(false)
const nodeList = ref([])
const total = ref(0)
const speedSnapshot = ref(null)
const speedRefreshing = ref(false)
let speedTimer = null
const SPEED_REFRESH_MS = 8000
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  instanceId: props.instanceId,
  nodeType: undefined
})

function getList() {
  loading.value = true
  queryParams.instanceId = props.instanceId
  listProxyNode(queryParams).then(res => {
    nodeList.value = res.rows
    total.value = res.total
    loading.value = false
    fetchTrafficForList(nodeList.value)
    refreshInstanceSpeed()
    restartSpeedPolling()
  }).catch(() => { loading.value = false })
}

function restartSpeedPolling() {
  clearSpeedPolling()
  speedTimer = setInterval(() => {
    refreshInstanceSpeed()
  }, SPEED_REFRESH_MS)
}

function clearSpeedPolling() {
  if (speedTimer != null) {
    clearInterval(speedTimer)
    speedTimer = null
  }
}

function refreshInstanceSpeed() {
  if (speedRefreshing.value || !props.instanceId) return
  speedRefreshing.value = true
  getInstanceSpeed(props.instanceId).then(res => {
    speedSnapshot.value = res.data || null
  }).catch(() => {
    speedSnapshot.value = { error: true, ports: {} }
  }).finally(() => {
    speedRefreshing.value = false
  })
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

const EXEC_TIMEOUT_MS = 90000
const addDialogVisible = ref(false)
const addFormRef = ref(null)
const customerOptions = ref([])
const addForm = reactive({ customerId: undefined, nodeType: 'VLESS-REALITY', port: undefined, expireTime: null, remark: '' })
const addFormPermanent = ref(true)
const addFormRules = {
  customerId: [{ required: true, message: '请选择归属客户', trigger: 'change' }],
  nodeType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }]
}

const remarkEditVisible = ref(false)
const remarkEditRow = ref(null)
const remarkEditValue = ref('')
const remarkSaving = ref(false)
const editNodeVisible = ref(false)
const editNodeSaving = ref(false)
const editNodeRow = ref(null)
const editNodePermanent = ref(false)
const editNodeForm = reactive({ expireTime: null, url: '' })

const addLogDrawerVisible = ref(false)
const execLogTitle = ref('添加节点 - 执行日志')
const addLog = ref('')
const addLogRef = ref(null)
const addLogRunning = ref(false)
const addLogExitCode = ref(null)
let addNodeReqId = 1
let deleteNodeReqId = null
let execTimeoutId = null

function clearExecTimeout() {
  if (execTimeoutId != null) {
    clearTimeout(execTimeoutId)
    execTimeoutId = null
  }
}

function endLogStateAsFailed(message) {
  addLogRunning.value = false
  addLogExitCode.value = -1
  if (message && addLog.value !== undefined) addLog.value += '\n' + message
}

function handleAdd() {
  addForm.customerId = undefined
  addForm.nodeType = 'VLESS-REALITY'
  addForm.port = undefined
  addForm.expireTime = null
  addForm.remark = ''
  addFormPermanent.value = true
  addDialogVisible.value = true
  getRecommendPort(props.instanceId).then(res => {
    if (res.data != null) addForm.port = res.data
  }).catch(() => {})
  listCustomer({ pageNum: 1, pageSize: 500 }).then(res => {
    customerOptions.value = res.rows || []
  }).catch(() => { customerOptions.value = [] })
  nextTick(() => addFormRef.value?.clearValidate())
}

function submitAddForm() {
  if (!props.wsConnected) {
    proxy.$modal.msgWarning('请等待 SSH 连接完成')
    return
  }
  addFormRef.value.validate(valid => {
    if (!valid) return
    addDialogVisible.value = false
    addLog.value = ''
    addLogRunning.value = true
    addLogExitCode.value = null
    addNodeReqId = Date.now()
    execLogTitle.value = '添加节点 - 执行日志'
    addLogDrawerVisible.value = true
    clearExecTimeout()
    const sent = props.sendWs({
      type: 'add_proxy_node',
      customerId: addForm.customerId,
      nodeType: addForm.nodeType,
      port: addForm.port,
      expireTime: addFormPermanent.value ? null : addForm.expireTime,
      remark: addForm.remark ? String(addForm.remark).trim() : undefined,
      reqId: addNodeReqId
    })
    if (sent === false) {
      endLogStateAsFailed('消息发送失败，连接可能已断开。')
      proxy.$modal.msgWarning('发送失败，请检查连接后重试')
      return
    }
    execTimeoutId = setTimeout(() => {
      execTimeoutId = null
      if (addLogRunning.value) {
        endLogStateAsFailed('执行超时（90 秒），请检查连接与服务器状态。')
        proxy.$modal.msgWarning('执行超时，请查看日志')
      }
    }, EXEC_TIMEOUT_MS)
  })
}

function handleWsMessage(msg) {
  const isAdd = msg.reqId === addNodeReqId
  const isDelete = msg.reqId === deleteNodeReqId
  if (!isAdd && !isDelete) return
  if (msg.type === 'exec_output') {
    const data = msg.data != null ? String(msg.data) : ''
    addLog.value += data
    nextTick(() => {
      const el = addLogRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  } else if (msg.type === 'exec_error') {
    addLog.value += (msg.message || '') + '\n'
  } else if (msg.type === 'exec_end') {
    clearExecTimeout()
    addLogRunning.value = false
    if (isDelete) {
      proxy.$modal.closeLoading()
      deletingNodeId.value = null
    }
    const code = msg.code != null ? msg.code : -1
    addLogExitCode.value = code
    if (code === 0) {
      if (isDelete) {
        proxy.$modal.msgSuccess('节点已删除')
        getList()
      }
      // 添加成功时不在这里提示，等 node_created 再提示一次即可，避免重复
    }
  } else if (msg.type === 'node_created') {
    clearExecTimeout()
    addLogRunning.value = false
    addLogExitCode.value = 0
    proxy.$modal.msgSuccess('节点已添加并保存')
    getList()
  }
}

watch(() => props.wsConnected, (connected) => {
  if (!connected && addLogRunning.value) {
    clearExecTimeout()
    proxy.$modal.closeLoading()
    deletingNodeId.value = null
    endLogStateAsFailed('连接已断开，本次操作已中止，状态未知。')
    proxy.$modal.msgWarning('连接已断开，请等待自动重连或刷新页面')
  }
})

function openNodeEdit(row) {
  editNodeRow.value = row
  editNodeForm.expireTime = row.expireTime || null
  editNodeForm.url = row.url || ''
  editNodePermanent.value = !row.expireTime
  editNodeVisible.value = true
}

function buildNodeNameByExpire(row, expireTime) {
  const now = expireTime ? new Date(expireTime) : null
  const tag = now
    ? `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
    : 'permanent'
  return `${row.nodeType}-${row.port}-${row.customerId ?? 0}-${tag}`
}

function submitNodeEdit() {
  if (!editNodeRow.value?.id) return
  const payload = {
    id: editNodeRow.value.id,
    expireTime: editNodePermanent.value ? null : editNodeForm.expireTime,
    url: (editNodeForm.url || '').trim()
  }
  editNodeSaving.value = true
  updateProxyNode(payload).then(() => {
    const row = editNodeRow.value
    row.expireTime = payload.expireTime
    row.url = payload.url
    row.nodeName = buildNodeNameByExpire(row, payload.expireTime)
    if (detailData.value?.id === row.id) {
      detailData.value.expireTime = row.expireTime
      detailData.value.url = row.url
      detailData.value.nodeName = row.nodeName
    }
    proxy.$modal.msgSuccess('节点已更新')
    editNodeVisible.value = false
    getList()
  }).catch(() => {}).finally(() => {
    editNodeSaving.value = false
  })
}

function openRemarkEdit(row) {
  remarkEditRow.value = row
  remarkEditValue.value = row.remark || ''
  remarkEditVisible.value = true
}

function submitRemarkEdit() {
  if (remarkEditRow.value == null) return
  const id = remarkEditRow.value.id
  const remark = (remarkEditValue.value || '').trim()
  remarkSaving.value = true
  updateProxyNode({ id, remark }).then(() => {
    remarkEditRow.value.remark = remark
    proxy.$modal.msgSuccess('备注已更新')
    remarkEditVisible.value = false
  }).catch(() => {}).finally(() => {
    remarkSaving.value = false
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

function handleSelectionChange(selection) {
  selectedIds.value = (selection || []).map(r => r.id).filter(id => id != null)
}

function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  proxy.$modal.confirm(`确认要删除选中的 ${selectedIds.value.length} 个节点吗？将同时在服务器上删除配置，执行流程与单个删除一致。`).then(() => {
    batchDeleteLoading.value = true
    proxy.$modal.loading('正在删除节点...')
    return delProxyNode(selectedIds.value.join(','))
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    selectedIds.value = []
    getList()
  }).catch(() => {}).finally(() => {
    proxy.$modal.closeLoading()
    batchDeleteLoading.value = false
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认要删除节点"${row.nodeName}"吗？删除将在服务器上执行后再移除数据库记录。`).then(() => {
    if (!props.wsConnected) {
      proxy.$modal.msgWarning('请等待 SSH 连接完成')
      return
    }
    proxy.$modal.loading('正在删除节点...')
    deletingNodeId.value = row.id
    addLog.value = ''
    addLogRunning.value = true
    addLogExitCode.value = null
    deleteNodeReqId = Date.now()
    execLogTitle.value = '删除节点 - 执行日志'
    addLogDrawerVisible.value = true
    clearExecTimeout()
    const sent = props.sendWs({
      type: 'remove_proxy_node',
      nodeId: row.id,
      reqId: deleteNodeReqId
    })
    if (sent === false) {
      proxy.$modal.closeLoading()
      deletingNodeId.value = null
      endLogStateAsFailed('消息发送失败，连接可能已断开。')
      proxy.$modal.msgWarning('发送失败，请检查连接后重试')
      return
    }
    execTimeoutId = setTimeout(() => {
      execTimeoutId = null
      if (addLogRunning.value) {
        proxy.$modal.closeLoading()
        deletingNodeId.value = null
        endLogStateAsFailed('执行超时（90 秒），请检查连接与服务器状态。')
        proxy.$modal.msgWarning('执行超时，请查看日志')
      }
    }, EXEC_TIMEOUT_MS)
  }).catch(() => {})
}

const selectedIds = ref([])
const batchDeleteLoading = ref(false)
const statusLoadingId = ref(null)
const deletingNodeId = ref(null)
const trafficMap = ref({})

const detailVisible = ref(false)
const detailData = ref(null)
const detailTraffic = ref(null)
const detailConfig = computed(() => {
  if (!detailData.value?.configJson) return null
  try { return JSON.parse(detailData.value.configJson) } catch { return null }
})

function handleDetail(row) {
  detailData.value = { ...row }
  detailTraffic.value = null
  detailVisible.value = true
  getProxyNodeTraffic(row.id).then(r => { detailTraffic.value = r.data }).catch(() => {})
}
function formatTraffic(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.max(0, Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}
function formatSpeedFromMb(value) {
  const mb = Number(value)
  if (!Number.isFinite(mb) || mb < 0) return '0 B/s'
  const bytes = mb * 1024 * 1024
  if (bytes <= 0) return '0 B/s'
  const k = 1024
  const sizes = ['B/s', 'KB/s', 'MB/s', 'GB/s', 'TB/s']
  const i = Math.max(0, Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}
function nodeRealtimeSpeedText(row) {
  if (!row || speedSnapshot.value?.error) return '实时：-'
  const port = row.port != null ? String(row.port) : ''
  const speed = port && speedSnapshot.value?.ports ? speedSnapshot.value.ports[port] : null
  if (!speed) return '实时：-'
  return '实时：↑ ' + formatSpeedFromMb(speed.upMbps) + ' / ↓ ' + formatSpeedFromMb(speed.downMbps)
}
function nodeTrafficTotalText(row, traffic) {
  const stat = traffic || trafficMap.value[row.id]
  return '累计：' + (stat ? '↑ ' + formatTraffic(stat.totalTx) + ' / ↓ ' + formatTraffic(stat.totalRx) : '-')
}
function formatBytes(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
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

function getNodeTypeTagColor(nodeType) {
  if (!nodeType) return ''
  if (nodeType.startsWith('VLESS')) return ''
  if (nodeType.startsWith('VMess')) return 'success'
  if (nodeType.startsWith('Trojan')) return 'warning'
  if (nodeType === 'Hysteria2' || nodeType === 'TUIC') return 'danger'
  return 'info'
}

function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}

onMounted(() => {
  emit('register-handler', handleWsMessage)
  getList()
  listCustomer({ pageNum: 1, pageSize: 500 }).then(res => {
    customerOptions.value = res.rows || []
  }).catch(() => { customerOptions.value = [] })
})

onBeforeUnmount(() => {
  clearExecTimeout()
  clearSpeedPolling()
})

watch(() => props.instanceId, () => {
  speedSnapshot.value = null
  clearSpeedPolling()
  getList()
})
</script>

<style scoped lang="scss">
.proxy-node-panel {
  padding: 0 4px;
}
.toolbar {
  display: flex;
  align-items: center;
}
.expire-forever {
  color: var(--el-color-success);
  font-weight: 500;
}
.expire-expired {
  color: var(--el-color-danger);
  font-weight: 500;
}
.share-url-section {
  margin-top: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.share-url-label {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
}
.share-url-box {
  display: flex;
  flex-direction: column;
}
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.status-loading {
  font-size: 14px;
  margin-right: 2px;
}
.text-placeholder {
  color: var(--el-text-color-placeholder);
}
.traffic-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1.25;
  white-space: nowrap;
}
.traffic-cell--detail {
  align-items: flex-start;
}
.traffic-cell-total {
  color: var(--el-text-color-primary);
}
.traffic-cell-speed {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.add-log-body {
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.add-log-status {
  font-size: 14px;
  margin-bottom: 10px;
  font-weight: 500;
}
.add-log-status.success {
  color: var(--el-color-success);
}
.add-log-status.fail {
  color: var(--el-color-danger);
}
.add-log-pre {
  flex: 1;
  margin: 0;
  padding: 10px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-size: 12px;
  line-height: 1.5;
  overflow: auto;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
.add-form-node-type {
  width: 100%;
}
.add-form-type-tip {
  float: right;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
.remark-cell-editable {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.remark-cell-text {
  color: var(--el-text-color-primary);
}
.remark-cell-icon {
  font-size: 14px;
  color: var(--el-text-color-placeholder);
  opacity: 0;
  flex-shrink: 0;
}
.remark-cell-editable:hover .remark-cell-text {
  color: var(--el-color-primary);
}
.remark-cell-editable:hover .remark-cell-icon {
  opacity: 1;
  color: var(--el-color-primary);
}
</style>
