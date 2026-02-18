<template>
  <div class="app-container">
    <el-card v-loading="infoLoading" class="box-card">
      <template #header>
        <span>客户详情</span>
        <span style="float: right">
          <el-button type="primary" link icon="Back" @click="goBack">返回</el-button>
        </span>
      </template>
      <el-descriptions :column="2" border v-if="customer.id">
        <el-descriptions-item label="编号">{{ customer.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ customer.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ customer.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ customer.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="微信号">{{ customer.wechat || '-' }}</el-descriptions-item>
        <el-descriptions-item label="QQ号">{{ customer.qq || '-' }}</el-descriptions-item>
        <el-descriptions-item label="头像">
          <el-avatar v-if="customer.avatar" :src="customer.avatar" :size="40" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="customer.status === '0' ? 'success' : 'danger'">{{ customer.status === '0' ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ parseTime(customer.registerTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近登录时间">{{ parseTime(customer.lastLoginAt) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近登录IP">{{ customer.lastLoginIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ customer.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <el-tabs v-model="activeTab">
        <el-tab-pane label="关联节点" name="bindings">
          <div class="toolbar">
            <el-button type="primary" size="small" icon="Plus" @click="handleAddNode" v-hasPermi="['resource:vps:add']">新增节点</el-button>
            <el-button type="danger" plain size="small" icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDeleteNode" v-hasPermi="['resource:vps:remove']">批量删除</el-button>
            <el-button icon="Refresh" size="small" circle style="margin-left: 8px" @click="loadBindings" />
          </div>
          <el-table v-loading="bindingsLoading" :data="bindings" border size="small" style="margin-top: 10px" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column label="节点名称" prop="nodeName" min-width="120" show-overflow-tooltip />
            <el-table-column label="节点类型" prop="nodeType" width="160">
              <template #default="{ row }">
                <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="所属VPS" width="140" align="center" show-overflow-tooltip>
              <template #default="{ row }">
                {{ instanceOptions.find(i => i.id === row.instanceId)?.name ?? row.instanceId ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column label="地址" prop="address" min-width="100" show-overflow-tooltip />
            <el-table-column label="端口" prop="port" width="80" align="center" />
            <el-table-column label="有效期" width="140" align="center">
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
            <el-table-column label="备注" min-width="100" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="hasEditPermi" class="remark-cell-editable" @click.stop="openRemarkEdit(row)">
                  <span class="remark-cell-text">{{ row.remark || '点击添加备注' }}</span>
                  <el-icon class="remark-cell-icon"><Edit /></el-icon>
                </span>
                <span v-else>{{ row.remark || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleNodeDetail(row)">详情</el-button>
                <el-button link type="primary" size="small" @click="handleCopyUrl(row)">复制链接</el-button>
                <el-button link type="danger" size="small" icon="Delete" :loading="deleteLoadingId === row.id || (batchDeleteLoading && selectedIds.includes(row.id))" @click="handleDeleteNode(row)" v-hasPermi="['resource:vps:remove']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!bindingsLoading && bindings.length === 0" description="暂无关联节点" />

          <el-dialog title="新增节点" v-model="addNodeVisible" width="460px" append-to-body>
            <el-form ref="addNodeFormRef" :model="addNodeForm" :rules="addNodeFormRules" label-width="90px">
              <el-form-item label="服务器" prop="instanceId">
                <el-select v-model="addNodeForm.instanceId" placeholder="请选择服务器" style="width: 100%" filterable @change="onAddNodeInstanceChange">
                  <el-option v-for="i in instanceOptions" :key="i.id" :label="i.name + ' (' + (i.ip || '') + ')'" :value="i.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="协议类型">
                <el-input model-value="VLESS-REALITY" disabled />
              </el-form-item>
              <el-form-item label="端口" prop="port">
                <el-input-number v-model="addNodeForm.port" :min="1" :max="65535" controls-position="right" style="width: 100%" placeholder="如 5000" />
              </el-form-item>
              <el-form-item label="有效期">
                <div style="display: flex; align-items: center; gap: 10px; width: 100%">
                  <el-date-picker
                    v-model="addNodeForm.expireTime"
                    type="datetime"
                    placeholder="选择过期时间"
                    :disabled="addNodePermanent"
                    style="flex: 1"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                  <el-checkbox v-model="addNodePermanent" @change="v => v && (addNodeForm.expireTime = null)">永久有效</el-checkbox>
                </div>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="addNodeForm.remark" type="textarea" :rows="2" placeholder="选填" maxlength="500" show-word-limit clearable />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="addNodeVisible = false">取 消</el-button>
              <el-button type="primary" :loading="addNodeSubmitting" @click="submitAddNode">确定（将在服务器执行）</el-button>
            </template>
          </el-dialog>

          <el-dialog title="节点详情" v-model="nodeDetailVisible" width="620px" append-to-body destroy-on-close>
            <el-descriptions v-if="currentNode" :column="2" border size="small">
              <el-descriptions-item label="节点名称">{{ currentNode.nodeName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="节点类型">
                <el-tag size="small" :type="getNodeTypeTagColor(currentNode.nodeType)">{{ currentNode.nodeType }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="地址">{{ currentNode.address }}</el-descriptions-item>
              <el-descriptions-item label="端口">{{ currentNode.port }}</el-descriptions-item>
              <el-descriptions-item label="有效期" :span="2">
                <span v-if="!currentNode.expireTime">永久有效</span>
                <span v-else :class="{ 'expire-expired': isExpired(currentNode.expireTime) }">{{ currentNode.expireTime }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <dict-tag :options="res_proxy_node_status" :value="currentNode.status" />
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentNode.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="流量统计" :span="2">
                <span v-if="nodeDetailTraffic">↑ {{ formatTraffic(nodeDetailTraffic.totalTx) }} / ↓ {{ formatTraffic(nodeDetailTraffic.totalRx) }}</span>
                <span v-else class="text-placeholder">-</span>
              </el-descriptions-item>
              <template v-if="nodeDetailConfig">
                <el-descriptions-item label="协议">{{ nodeDetailConfig.protocol || '-' }}</el-descriptions-item>
                <el-descriptions-item label="用户ID">{{ nodeDetailConfig.id || '-' }}</el-descriptions-item>
                <el-descriptions-item label="流控">{{ nodeDetailConfig.flow || '-' }}</el-descriptions-item>
                <el-descriptions-item label="传输协议">{{ nodeDetailConfig.network || '-' }}</el-descriptions-item>
                <el-descriptions-item label="安全层">{{ nodeDetailConfig.security || '-' }}</el-descriptions-item>
                <el-descriptions-item label="SNI">{{ nodeDetailConfig.sni || '-' }}</el-descriptions-item>
                <el-descriptions-item label="指纹">{{ nodeDetailConfig.fingerprint || '-' }}</el-descriptions-item>
                <el-descriptions-item label="公钥" :span="2">{{ nodeDetailConfig.publicKey || '-' }}</el-descriptions-item>
              </template>
              <el-descriptions-item label="备注" :span="2">{{ currentNode.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="currentNode && currentNode.url" class="share-url-section">
              <div class="share-url-label">分享链接</div>
              <div class="share-url-box">
                <el-input :model-value="currentNode.url" readonly type="textarea" :rows="3" />
                <el-button type="primary" size="small" style="margin-top: 8px" @click="copyToClipboard(currentNode.url)">
                  <el-icon><DocumentCopy /></el-icon> 复制链接
                </el-button>
              </div>
            </div>
            <template #footer>
              <el-button link type="danger" @click="handleDeleteNode(currentNode); nodeDetailVisible = false" v-hasPermi="['resource:vps:remove']">删除</el-button>
              <el-button @click="nodeDetailVisible = false">关 闭</el-button>
            </template>
          </el-dialog>

          <el-dialog title="修改备注" v-model="remarkEditVisible" width="420px" append-to-body destroy-on-close @closed="remarkEditRow = null">
            <el-input v-model="remarkEditValue" type="textarea" :rows="3" placeholder="选填" maxlength="500" show-word-limit />
            <template #footer>
              <el-button @click="remarkEditVisible = false">取消</el-button>
              <el-button type="primary" :loading="remarkSaving" @click="submitRemarkEdit">确定</el-button>
            </template>
          </el-dialog>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="MemberCustomerDetail">
import useUserStore from '@/store/modules/user'
import { getCustomer, getCustomerBindings } from '@/api/member/customer'
import { listInstance, addProxyNodeOnInstance, updateProxyNode, delProxyNode, getProxyNodeTraffic, getRecommendPort } from '@/api/resource/vps'
import { parseTime } from '@/utils/skyway'
import { DocumentCopy, Loading, Edit, Delete } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const { res_proxy_node_status } = proxy.useDict('res_proxy_node_status')
const userStore = useUserStore()
const hasEditPermi = computed(() => (userStore.permissions || []).some(p => p === '*:*:*' || p === 'resource:vps:edit'))

const customerId = computed(() => Number(route.params.customerId))
const customer = ref({})
const bindings = ref([])
const infoLoading = ref(true)
const bindingsLoading = ref(false)
const activeTab = ref('bindings')

const addNodeVisible = ref(false)
const addNodeSubmitting = ref(false)
const addNodePermanent = ref(true)
const instanceOptions = ref([])
const addNodeForm = reactive({ instanceId: undefined, port: undefined, expireTime: null, remark: '' })
const addNodeFormRef = ref(null)
const addNodeFormRules = {
  instanceId: [{ required: true, message: '请选择服务器', trigger: 'change' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }]
}

const nodeDetailVisible = ref(false)
const currentNode = ref(null)
const nodeDetailTraffic = ref(null)
const nodeDetailConfig = computed(() => {
  if (!currentNode.value?.configJson) return null
  try { return JSON.parse(currentNode.value.configJson) } catch { return null }
})

const selectedIds = ref([])
const batchDeleteLoading = ref(false)
const statusLoadingId = ref(null)
const deleteLoadingId = ref(null)
const trafficMap = ref({})

const remarkEditVisible = ref(false)
const remarkEditRow = ref(null)
const remarkEditValue = ref('')
const remarkSaving = ref(false)

const listQuery = ref({})

function loadBindings() {
  bindingsLoading.value = true
  getCustomerBindings(customerId.value).then(res => {
    bindings.value = res.data || []
    bindingsLoading.value = false
    fetchTrafficForList(bindings.value)
  }).catch(() => { bindingsLoading.value = false })
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

function handleAddNode() {
  addNodeForm.instanceId = undefined
  addNodeForm.port = undefined
  addNodeForm.expireTime = null
  addNodeForm.remark = ''
  addNodePermanent.value = true
  addNodeVisible.value = true
  listInstance({ pageNum: 1, pageSize: 500 }).then(res => {
    instanceOptions.value = res.rows || []
  }).catch(() => { instanceOptions.value = [] })
  nextTick(() => addNodeFormRef.value?.clearValidate())
}

function onAddNodeInstanceChange(instanceId) {
  if (instanceId != null) {
    getRecommendPort(instanceId).then(res => {
      if (res.data != null) addNodeForm.port = res.data
    }).catch(() => {})
  } else {
    addNodeForm.port = undefined
  }
}

function submitAddNode() {
  addNodeFormRef.value.validate(valid => {
    if (!valid) return
    addNodeSubmitting.value = true
    addProxyNodeOnInstance(addNodeForm.instanceId, {
      customerId: customerId.value,
      port: addNodeForm.port,
      expireTime: addNodePermanent.value ? null : addNodeForm.expireTime,
      remark: (addNodeForm.remark || '').trim() || undefined
    }).then(() => {
      proxy.$modal.msgSuccess('节点已添加')
      addNodeVisible.value = false
      loadBindings()
    }).catch(e => {
      proxy.$modal.msgError(e.msg || e.message || '添加失败')
    }).finally(() => { addNodeSubmitting.value = false })
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
    if (currentNode.value?.id === id) currentNode.value.remark = remark
    proxy.$modal.msgSuccess('备注已更新')
    remarkEditVisible.value = false
  }).catch(() => {}).finally(() => {
    remarkSaving.value = false
  })
}

function handleNodeDetail(row) {
  currentNode.value = { ...row }
  nodeDetailTraffic.value = null
  nodeDetailVisible.value = true
  getProxyNodeTraffic(row.id).then(r => { nodeDetailTraffic.value = r.data }).catch(() => {})
}
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

function handleSelectionChange(selection) {
  selectedIds.value = (selection || []).map(r => r.id).filter(id => id != null)
}

function handleBatchDeleteNode() {
  if (selectedIds.value.length === 0) return
  proxy.$modal.confirm(`确认要删除选中的 ${selectedIds.value.length} 个节点吗？将同时在服务器上删除配置，执行流程与单个删除一致。`).then(() => {
    batchDeleteLoading.value = true
    proxy.$modal.loading('正在删除节点...')
    return delProxyNode(selectedIds.value.join(','))
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    nodeDetailVisible.value = false
    selectedIds.value = []
    loadBindings()
  }).catch(() => {}).finally(() => {
    proxy.$modal.closeLoading()
    batchDeleteLoading.value = false
  })
}

function handleDeleteNode(row) {
  if (!row?.id) return
  proxy.$modal.confirm(`确认要删除节点"${row.nodeName}"吗？将同时在服务器上删除配置。`).then(() => {
    deleteLoadingId.value = row.id
    proxy.$modal.loading('正在删除节点...')
    return delProxyNode(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    nodeDetailVisible.value = false
    loadBindings()
  }).catch(() => {}).finally(() => {
    proxy.$modal.closeLoading()
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

function goBack() {
  router.push({ path: '/member/customer', query: listQuery.value })
}

onMounted(() => {
  listQuery.value = { pageNum: route.query.pageNum, pageSize: route.query.pageSize, keyword: route.query.keyword, status: route.query.status }
  getCustomer(customerId.value).then(res => {
    customer.value = res.data
    infoLoading.value = false
  }).catch(() => { infoLoading.value = false })
  loadBindings()
  listInstance({ pageNum: 1, pageSize: 500 }).then(res => {
    instanceOptions.value = res.rows || []
  }).catch(() => { instanceOptions.value = [] })
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; }
.expire-forever { color: var(--el-color-success); font-weight: 500; }
.expire-expired { color: var(--el-color-danger); font-weight: 500; }
.share-url-section { padding: 12px; background: var(--el-fill-color-light); border-radius: 6px; }
.share-url-label { font-size: 13px; font-weight: 500; margin-bottom: 8px; }
.share-url-box { display: flex; flex-direction: column; }
.status-cell { display: inline-flex; align-items: center; gap: 6px; }
.status-loading { font-size: 14px; margin-right: 2px; }
.text-placeholder { color: var(--el-text-color-placeholder); }
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
