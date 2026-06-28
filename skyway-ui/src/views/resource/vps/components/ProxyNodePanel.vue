<template>
  <div class="proxy-node-panel">
    <div class="toolbar">
      <el-button type="primary" size="small" icon="Plus" @click="handleAdd" v-hasPermi="['resource:vps:add']">新增节点</el-button>
      <el-button type="danger" plain size="small" icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete" v-hasPermi="['resource:vps:remove']">批量删除</el-button>
      <el-select v-model="queryParams.nodeType" placeholder="全部类型" clearable size="small" style="width: 180px; margin-left: 10px" @change="handleFilterChange">
        <el-option v-for="t in nodeTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="queryParams.expireStatus" placeholder="过期状态" size="small" style="width: 120px; margin-left: 8px" @change="handleFilterChange">
        <el-option label="未过期" value="unexpired" />
        <el-option label="已过期" value="expired" />
        <el-option label="全部" value="all" />
      </el-select>
      <el-button icon="Refresh" size="small" circle style="margin-left: 8px" @click="getList" />
    </div>

    <el-table v-loading="loading" :data="nodeList" border size="small" style="margin-top: 10px" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="节点名称" prop="nodeName" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link
            v-if="linkNodeNameToInstance && row.instanceId"
            type="primary"
            :underline="false"
            @click.stop="goVpsDetail(row.instanceId)"
          >
            {{ row.nodeName || '-' }}
          </el-link>
          <span v-else>{{ row.nodeName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="节点类型" prop="nodeType" width="160">
        <template #default="{ row }">
          <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!hideCustomerColumn" label="归属客户" width="120" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          {{ customerOptions.find(c => c.id === row.customerId)?.username ?? row.customerId ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column v-if="showInstanceColumn" label="所属VPS" width="150" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          {{ instanceOptions.find(i => i.id === row.instanceId)?.name ?? row.instanceId ?? '-' }}
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
      <el-table-column label="限速" min-width="210" align="center">
        <template #default="{ row }">
          <div class="rate-limit-cell">
            <div>{{ rateLimitLogicText(row) }}</div>
            <div class="rate-limit-sub">{{ rateLimitDurationText(row) }}</div>
            <div class="rate-limit-rule">{{ rateLimitRuleText(row) }}</div>
          </div>
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
      <el-table-column label="操作" width="210" align="center">
        <template #default="{ row }">
          <div class="node-op-actions">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleCopyUrl(row)">复制链接</el-button>
            <el-dropdown class="node-op-dropdown" trigger="click" @command="(cmd) => handleNodeCommand(cmd, row)" v-hasPermi="['resource:vps:edit', 'resource:vps:remove']">
              <el-button link type="primary" size="small" icon="DArrowRight">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" icon="Edit" v-hasPermi="['resource:vps:edit']">编辑</el-dropdown-item>
                  <el-dropdown-item command="rateLimit" icon="Timer" v-hasPermi="['resource:vps:edit']">设置限速</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="Delete" divided v-hasPermi="['resource:vps:remove']">删除</el-dropdown-item>
                  <el-dropdown-item command="forceDelete" icon="DeleteFilled" v-hasPermi="['resource:vps:remove']">强制删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新增节点" v-model="addDialogVisible" width="560px" append-to-body>
      <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="90px">
        <el-form-item v-if="!fixedCustomer" label="归属客户" prop="customerId">
          <el-select v-model="addForm.customerId" placeholder="请选择客户" style="width: 100%" filterable>
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.username" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!instanceId" label="服务器" prop="instanceId">
          <el-select v-model="addForm.instanceId" placeholder="请选择服务器" style="width: 100%" filterable @change="onAddInstanceChange">
            <el-option v-for="i in instanceOptions" :key="i.id" :label="i.name + ' (' + (i.ip || '') + ')'" :value="i.id" />
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
        <template v-if="canConfigureRelay">
          <el-form-item label="启用中转">
            <el-switch v-model="addForm.enableRelay" active-text="SOCKS5" />
          </el-form-item>
          <template v-if="addForm.enableRelay">
            <el-form-item label="S5配置" prop="relayText">
              <el-input
                v-model="addForm.relayText"
                placeholder="204.1.132.93:35345:lVZjQtlJ:Qat3T6ofak"
                clearable
                @input="parseRelayTextToForm"
              />
            </el-form-item>
            <el-form-item label="服务器">
              <el-input v-model="addForm.relayHost" disabled />
            </el-form-item>
            <el-form-item label="中转端口">
              <el-input v-model="addForm.relayPort" disabled />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="addForm.relayUsername" disabled />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="addForm.relayPassword" disabled show-password />
            </el-form-item>
          </template>
        </template>
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
        <el-button type="primary" :disabled="!canSubmitAdd" :loading="httpExecSubmitting" @click="submitAddForm">
          {{ canSubmitAdd ? '确定（将在服务器执行）' : '等待 SSH 连接...' }}
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
          <template v-if="detailConfig.relay">
            <el-descriptions-item label="中转类型">{{ detailConfig.relay.type || '-' }}</el-descriptions-item>
            <el-descriptions-item label="中转地址">{{ detailConfig.relay.server || '-' }}</el-descriptions-item>
            <el-descriptions-item label="中转端口">{{ detailConfig.relay.serverPort || '-' }}</el-descriptions-item>
            <el-descriptions-item label="中转用户">{{ detailConfig.relay.username || '-' }}</el-descriptions-item>
          </template>
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
        <el-dropdown trigger="click" @command="(cmd) => handleDetailNodeCommand(cmd)" v-hasPermi="['resource:vps:edit', 'resource:vps:remove']">
          <el-button>更多</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit" icon="Edit" v-hasPermi="['resource:vps:edit']">编辑</el-dropdown-item>
              <el-dropdown-item command="rateLimit" icon="Timer" v-hasPermi="['resource:vps:edit']">设置限速</el-dropdown-item>
              <el-dropdown-item command="delete" icon="Delete" divided v-hasPermi="['resource:vps:remove']">删除</el-dropdown-item>
              <el-dropdown-item command="forceDelete" icon="DeleteFilled" v-hasPermi="['resource:vps:remove']">强制删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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

    <el-dialog :title="rateLimitDialogTitle" v-model="rateLimitVisible" width="480px" append-to-body destroy-on-close>
      <el-form ref="rateLimitFormRef" :model="rateLimitForm" :rules="rateLimitRules" label-width="96px" class="rate-limit-form">
        <el-form-item label="节点端口">
          <el-input :model-value="rateLimitRow?.port || '-'" disabled />
        </el-form-item>
        <el-form-item label="下载限速" prop="downloadMbps">
          <div class="rate-limit-input-row">
            <el-input-number v-model="rateLimitForm.downloadMbps" :min="1" :max="100000" controls-position="right" class="rate-limit-input-number" />
            <span class="rate-limit-unit">Mbps</span>
          </div>
        </el-form-item>
        <el-form-item label="上传限速" prop="uploadMbps">
          <div class="rate-limit-input-row">
            <el-input-number v-model="rateLimitForm.uploadMbps" :min="1" :max="100000" controls-position="right" class="rate-limit-input-number" />
            <span class="rate-limit-unit">Mbps</span>
          </div>
        </el-form-item>
        <el-form-item label="限速时长">
          <div class="rate-limit-duration-row">
            <el-date-picker
              v-model="rateLimitForm.expireTime"
              type="datetime"
              placeholder="选择到期时间"
              :disabled="rateLimitPermanent"
              style="flex: 1"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <el-checkbox v-model="rateLimitPermanent" @change="v => v && (rateLimitForm.expireTime = null)">永久</el-checkbox>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          v-if="currentRateLimit"
          type="danger"
          plain
          :loading="rateLimitSaving"
          @click="submitRemoveRateLimit"
        >移除限速</el-button>
        <el-button @click="rateLimitVisible = false">取消</el-button>
        <el-button type="primary" :loading="rateLimitSaving" @click="submitRateLimit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import useUserStore from '@/store/modules/user'
import {
  listProxyNode,
  updateProxyNode,
  delProxyNode,
  forceDelProxyNode,
  getProxyNodeTraffic,
  getRecommendPort,
  getInstanceSpeed,
  getInstanceSpeedSnapshot,
  listInstance,
  checkInstanceSsh,
  addProxyNodeOnInstance,
  listProxyNodeRateLimit,
  setProxyNodeRateLimit,
  removeProxyNodeRateLimit
} from '@/api/resource/vps'
import { listCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'
import { DocumentCopy, Loading, Edit, Delete } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const { res_proxy_node_status } = proxy.useDict('res_proxy_node_status')
const userStore = useUserStore()
const hasEditPermi = computed(() => (userStore.permissions || []).some(p => p === '*:*:*' || p === 'resource:vps:edit'))

const props = defineProps({
  instanceId: { type: Number, default: null },
  customerId: { type: Number, default: null },
  fixedCustomer: { type: Boolean, default: false },
  hideCustomerColumn: { type: Boolean, default: false },
  showInstanceColumn: { type: Boolean, default: false },
  linkNodeNameToInstance: { type: Boolean, default: false },
  useHttpExec: { type: Boolean, default: false },
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
  instanceId: props.instanceId || undefined,
  customerId: props.customerId || undefined,
  nodeType: undefined,
  expireStatus: 'unexpired'
})
const effectiveInstanceId = computed(() => props.instanceId || addForm.instanceId || null)
const canUseWsExec = computed(() => !!props.instanceId && props.wsConnected && !props.useHttpExec)
const canSubmitAdd = computed(() => {
  if (props.useHttpExec || !props.instanceId) return true
  return props.wsConnected
})

function getList() {
  loading.value = true
  queryParams.instanceId = props.instanceId || undefined
  queryParams.customerId = props.customerId || undefined
  const params = {
    ...queryParams,
    expireStatus: queryParams.expireStatus === 'all' ? undefined : queryParams.expireStatus
  }
  listProxyNode(params).then(res => {
    nodeList.value = res.rows
    total.value = res.total
    loading.value = false
    syncRateLimitsFromRows(nodeList.value)
    fetchTrafficForList(nodeList.value)
    fetchRateLimits()
    refreshInstanceSpeed()
    restartSpeedPolling()
  }).catch(() => { loading.value = false })
}

function handleFilterChange() {
  queryParams.pageNum = 1
  getList()
}

function syncRateLimitsFromRows(rows) {
  const map = { ...rateLimitMap.value }
  ;(rows || []).forEach(row => {
    if (!row || row.id == null) return
    delete map[row.id]
    if (row.rateLimit) {
      map[row.id] = row.rateLimit
    }
  })
  rateLimitMap.value = buildRateLimitMap(Object.values(map))
}

function fetchRateLimits() {
  if (!props.instanceId) {
    applyRateLimitMapToRows(rateLimitMap.value)
    return
  }
  listProxyNodeRateLimit({ instanceId: props.instanceId }).then(res => {
    const map = buildRateLimitMap([
      ...Object.values(rateLimitMap.value || {}),
      ...(res.data || [])
    ])
    rateLimitMap.value = map
    applyRateLimitMapToRows(map)
  }).catch(() => {
    rateLimitMap.value = { ...rateLimitMap.value }
  })
}

function buildRateLimitMap(rows) {
  const map = {}
  ;(rows || []).forEach(item => {
    if (!item || item.proxyNodeId == null) return
    const current = map[item.proxyNodeId]
    if (!current || compareRateLimit(item, current) > 0) {
      map[item.proxyNodeId] = item
    }
  })
  return map
}

function compareRateLimit(a, b) {
  const timeA = new Date(a.updateTime || a.createTime || 0).getTime() || 0
  const timeB = new Date(b.updateTime || b.createTime || 0).getTime() || 0
  if (timeA !== timeB) return timeA - timeB
  return Number(a.id || 0) - Number(b.id || 0)
}

function mergeRateLimit(limit, fallback) {
  const normalized = {
    ...(fallback || {}),
    ...(limit || {})
  }
  if (normalized.proxyNodeId == null) return
  normalized.updateTime = normalized.updateTime || parseTime(new Date(), '{y}-{m}-{d} {h}:{i}:{s}')
  rateLimitMap.value = {
    ...rateLimitMap.value,
    [normalized.proxyNodeId]: normalized
  }
  const row = nodeList.value.find(item => item.id === normalized.proxyNodeId)
  if (row) {
    row.rateLimit = normalized
  }
}

function applyRateLimitMapToRows(map) {
  ;(nodeList.value || []).forEach(row => {
    if (!row || row.id == null) return
    const limit = map[row.id]
    if (limit || row.rateLimit) {
      row.rateLimit = limit || null
    }
  })
}

function removeRateLimitFromMap(proxyNodeId) {
  if (proxyNodeId == null) return
  const map = { ...rateLimitMap.value }
  delete map[proxyNodeId]
  rateLimitMap.value = map
  const row = nodeList.value.find(item => item.id === proxyNodeId)
  if (row) {
    row.rateLimit = null
  }
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
  if (speedRefreshing.value) return
  speedRefreshing.value = true
  const request = props.instanceId ? getInstanceSpeed(props.instanceId) : getInstanceSpeedSnapshot()
  request.then(res => {
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
const instanceOptions = ref([])
const addForm = reactive({
  customerId: undefined,
  instanceId: undefined,
  nodeType: 'VLESS-REALITY',
  port: undefined,
  enableRelay: false,
  relayText: '',
  relayHost: '',
  relayPort: '',
  relayUsername: '',
  relayPassword: '',
  expireTime: null,
  remark: ''
})
const addFormPermanent = ref(true)
const canConfigureRelay = computed(() => addForm.nodeType === 'VLESS-REALITY')
const addFormRules = {
  customerId: [{ required: true, message: '请选择归属客户', trigger: 'change' }],
  instanceId: [{ required: true, message: '请选择服务器', trigger: 'change' }],
  nodeType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  relayText: [{
    validator: (rule, value, callback) => {
      if (!addForm.enableRelay || !canConfigureRelay.value) {
        callback()
        return
      }
      const parsed = parseSocks5RelayText(value)
      if (!parsed.ok) {
        callback(new Error(parsed.message))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }]
}
const httpExecSubmitting = ref(false)

const remarkEditVisible = ref(false)
const remarkEditRow = ref(null)
const remarkEditValue = ref('')
const remarkSaving = ref(false)
const editNodeVisible = ref(false)
const editNodeSaving = ref(false)
const editNodeRow = ref(null)
const editNodePermanent = ref(false)
const editNodeForm = reactive({ expireTime: null, url: '' })
const rateLimitVisible = ref(false)
const rateLimitSaving = ref(false)
const rateLimitFormRef = ref(null)
const rateLimitRow = ref(null)
const rateLimitPermanent = ref(true)
const rateLimitForm = reactive({ downloadMbps: 50, uploadMbps: 20, expireTime: null })
const rateLimitRules = {
  downloadMbps: [{ required: true, message: '请输入下载限速', trigger: 'blur' }],
  uploadMbps: [{ required: true, message: '请输入上传限速', trigger: 'blur' }]
}
const currentRateLimit = computed(() => {
  const rowId = rateLimitRow.value?.id
  return rowId != null ? rateLimitMap.value[rowId] : null
})
const rateLimitDialogTitle = computed(() => currentRateLimit.value ? '修改端口限速' : '设置端口限速')

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

function resetRelayForm() {
  addForm.enableRelay = false
  addForm.relayText = ''
  addForm.relayHost = ''
  addForm.relayPort = ''
  addForm.relayUsername = ''
  addForm.relayPassword = ''
}

function parseSocks5RelayText(value) {
  const text = (value || '').trim()
  if (!text) return { ok: false, message: '请输入 SOCKS5 中转配置' }
  const parts = text.split(':')
  if (parts.length !== 4 || parts.some(part => !part.trim())) {
    return { ok: false, message: '格式应为 host:port:username:password' }
  }
  const port = Number(parts[1].trim())
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    return { ok: false, message: 'SOCKS5 端口范围为 1-65535' }
  }
  return {
    ok: true,
    host: parts[0].trim(),
    port: String(port),
    username: parts[2].trim(),
    password: parts[3].trim()
  }
}

function parseRelayTextToForm() {
  const parsed = parseSocks5RelayText(addForm.relayText)
  addForm.relayHost = parsed.ok ? parsed.host : ''
  addForm.relayPort = parsed.ok ? parsed.port : ''
  addForm.relayUsername = parsed.ok ? parsed.username : ''
  addForm.relayPassword = parsed.ok ? parsed.password : ''
}

function addRelayPayload(payload) {
  if (!addForm.enableRelay || !canConfigureRelay.value) return payload
  return {
    ...payload,
    relayText: (addForm.relayText || '').trim()
  }
}

function handleAdd() {
  addForm.customerId = props.fixedCustomer ? props.customerId : undefined
  addForm.instanceId = props.instanceId || undefined
  addForm.nodeType = 'VLESS-REALITY'
  addForm.port = undefined
  resetRelayForm()
  addForm.expireTime = null
  addForm.remark = ''
  addFormPermanent.value = true
  addDialogVisible.value = true
  const portInstanceId = effectiveInstanceId.value
  if (portInstanceId) getRecommendPort(portInstanceId).then(res => {
    if (res.data != null) addForm.port = res.data
  }).catch(() => {})
  ensureCustomerOptions()
  ensureInstanceOptions()
  nextTick(() => addFormRef.value?.clearValidate())
}

function onAddInstanceChange(instanceId) {
  if (instanceId != null) {
    getRecommendPort(instanceId).then(res => {
      if (res.data != null) addForm.port = res.data
    }).catch(() => {})
  } else {
    addForm.port = undefined
  }
}

function ensureCustomerOptions() {
  if (props.fixedCustomer && props.customerId) return
  listCustomer({ pageNum: 1, pageSize: 500 }).then(res => {
    customerOptions.value = res.rows || []
  }).catch(() => { customerOptions.value = [] })
}

function ensureInstanceOptions() {
  if (props.instanceId) return
  listInstance({ pageNum: 1, pageSize: 500 }).then(res => {
    instanceOptions.value = res.rows || []
  }).catch(() => { instanceOptions.value = [] })
}

function submitAddForm() {
  if (!props.useHttpExec && props.instanceId && !props.wsConnected) {
    proxy.$modal.msgWarning('请等待 SSH 连接完成')
    return
  }
  addFormRef.value.validate(valid => {
    if (!valid) return
    if (props.useHttpExec || !props.instanceId) {
      submitAddFormByHttp()
      return
    }
    addDialogVisible.value = false
    addLog.value = ''
    addLogRunning.value = true
    addLogExitCode.value = null
    addNodeReqId = Date.now()
    execLogTitle.value = '添加节点 - 执行日志'
    addLogDrawerVisible.value = true
    clearExecTimeout()
    const sent = props.sendWs(addRelayPayload({
      type: 'add_proxy_node',
      customerId: addForm.customerId,
      nodeType: addForm.nodeType,
      port: addForm.port,
      expireTime: addFormPermanent.value ? null : addForm.expireTime,
      remark: addForm.remark ? String(addForm.remark).trim() : undefined,
      reqId: addNodeReqId
    }))
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

function submitAddFormByHttp() {
  const instanceId = effectiveInstanceId.value
  if (!instanceId) {
    proxy.$modal.msgWarning('请选择服务器')
    return
  }
  addDialogVisible.value = false
  addLog.value = '正在连接 SSH...\n'
  addLogRunning.value = true
  addLogExitCode.value = null
  execLogTitle.value = '添加节点 - 执行日志'
  addLogDrawerVisible.value = true
  httpExecSubmitting.value = true
  const payload = addRelayPayload({
    customerId: addForm.customerId,
    nodeType: addForm.nodeType,
    port: addForm.port,
    expireTime: addFormPermanent.value ? null : addForm.expireTime,
    remark: addForm.remark ? String(addForm.remark).trim() : undefined
  })
  const finishErr = (msg) => {
    addLog.value += msg + '\n'
    addLogRunning.value = false
    addLogExitCode.value = -1
    proxy.$modal.msgError(msg)
    nextTick(() => { const el = addLogRef.value; if (el) el.scrollTop = el.scrollHeight })
  }
  checkInstanceSsh(instanceId).then(() => {
    addLog.value += 'SSH 连接成功，正在执行添加节点...\n'
    nextTick(() => { const el = addLogRef.value; if (el) el.scrollTop = el.scrollHeight })
    return addProxyNodeOnInstance(instanceId, payload)
  }).then(() => {
    addLog.value += '节点已保存到数据库。\n'
    addLogRunning.value = false
    addLogExitCode.value = 0
    proxy.$modal.msgSuccess('节点已添加')
    getList()
    nextTick(() => { const el = addLogRef.value; if (el) el.scrollTop = el.scrollHeight })
  }).catch(e => {
    finishErr(e.msg || e.message || '添加失败')
  }).finally(() => {
    httpExecSubmitting.value = false
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

watch(() => addForm.nodeType, (nodeType) => {
  if (nodeType !== 'VLESS-REALITY') {
    resetRelayForm()
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
  return `${row.nodeType}-${row.address || 'unknown'}-${row.port}-${row.customerId ?? 0}-${tag}`
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

function handleNodeCommand(command, row) {
  if (command === 'edit') openNodeEdit(row)
  else if (command === 'rateLimit') openRateLimitDialog(row)
  else if (command === 'delete') handleDelete(row)
  else if (command === 'forceDelete') handleForceDelete(row)
}

function handleDetailNodeCommand(command) {
  if (!detailData.value) return
  if (command === 'delete' || command === 'forceDelete') {
    detailVisible.value = false
  }
  handleNodeCommand(command, detailData.value)
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认要删除节点"${row.nodeName}"吗？删除将在服务器上执行后再移除数据库记录。`).then(() => {
    if (props.useHttpExec || !props.instanceId) {
      proxy.$modal.loading('正在删除节点...')
      return delProxyNode(row.id).then(() => {
        proxy.$modal.msgSuccess('节点已删除')
        getList()
      }).finally(() => {
        proxy.$modal.closeLoading()
      })
    }
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

function handleForceDelete(row) {
  proxy.$modal.confirm(`确认要强制删除节点"${row.nodeName}"吗？此操作只删除本地节点和流量记录，不连接服务器，也不会清理服务器上的残留配置。`).then(() => {
    return forceDelProxyNode(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('强制删除成功')
    getList()
  }).catch(() => {})
}

const selectedIds = ref([])
const batchDeleteLoading = ref(false)
const statusLoadingId = ref(null)
const deletingNodeId = ref(null)
const trafficMap = ref({})
const rateLimitMap = ref({})

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

function goVpsDetail(instanceId) {
  if (!instanceId) return
  router.push({ path: '/resource/vps-detail/index/' + instanceId })
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
  const snapshot = resolveSpeedSnapshot(row)
  const speed = port && snapshot?.ports ? snapshot.ports[port] : null
  if (!speed) return '实时：-'
  return '实时：↑ ' + formatSpeedFromMb(speed.upMbps) + ' / ↓ ' + formatSpeedFromMb(speed.downMbps)
}
function resolveSpeedSnapshot(row) {
  if (!speedSnapshot.value) return null
  if (props.instanceId) return speedSnapshot.value
  const instanceId = row?.instanceId
  if (instanceId == null) return null
  return speedSnapshot.value[String(instanceId)] || speedSnapshot.value[instanceId] || null
}
function nodeTrafficTotalText(row, traffic) {
  const stat = traffic || trafficMap.value[row.id]
  return '累计：' + (stat ? '↑ ' + formatTraffic(stat.totalTx) + ' / ↓ ' + formatTraffic(stat.totalRx) : '-')
}
function getRateLimit(row) {
  if (!row || row.id == null) return null
  return buildRateLimitMap([row.rateLimit, rateLimitMap.value[row.id]])[row.id] || null
}
function isRateLimitExpired(limit) {
  return limit?.expireTime ? new Date(limit.expireTime) <= new Date() : false
}
function rateLimitLogicText(row) {
  const limit = getRateLimit(row)
  if (!limit) return '未限速'
  if (limit.status === 'failed') return '应用失败'
  if (isRateLimitExpired(limit)) return '已过期'
  return '端口独立限速'
}
function rateLimitDurationText(row) {
  const limit = getRateLimit(row)
  if (!limit) return '-'
  if (!limit.expireTime) return '永久'
  return '至 ' + parseTime(limit.expireTime, '{y}-{m}-{d} {h}:{i}')
}
function rateLimitRuleText(row) {
  const limit = getRateLimit(row)
  if (!limit) return '-'
  return '↓ ' + limit.downloadMbps + ' Mbps / ↑ ' + limit.uploadMbps + ' Mbps'
}
function openRateLimitDialog(row) {
  rateLimitRow.value = row
  const existing = getRateLimit(row)
  rateLimitForm.downloadMbps = existing?.downloadMbps || 50
  rateLimitForm.uploadMbps = existing?.uploadMbps || 20
  rateLimitForm.expireTime = existing?.expireTime || null
  rateLimitPermanent.value = !rateLimitForm.expireTime
  rateLimitVisible.value = true
  nextTick(() => rateLimitFormRef.value?.clearValidate())
}
function submitRateLimit() {
  if (!rateLimitRow.value?.id) return
  rateLimitFormRef.value.validate(valid => {
    if (!valid) return
    const payload = {
      downloadMbps: rateLimitForm.downloadMbps,
      uploadMbps: rateLimitForm.uploadMbps,
      expireTime: rateLimitPermanent.value ? null : rateLimitForm.expireTime
    }
    if (!payload.downloadMbps || payload.downloadMbps <= 0 || !payload.uploadMbps || payload.uploadMbps <= 0) {
      proxy.$modal.msgWarning('限速带宽必须大于 0')
      return
    }
    if (!rateLimitPermanent.value && !payload.expireTime) {
      proxy.$modal.msgWarning('请选择限速到期时间')
      return
    }
    rateLimitSaving.value = true
    setProxyNodeRateLimit(rateLimitRow.value.id, payload).then(res => {
      mergeRateLimit(res.data, {
        ...(getRateLimit(rateLimitRow.value) || {}),
        proxyNodeId: rateLimitRow.value.id,
        instanceId: rateLimitRow.value.instanceId || props.instanceId,
        port: rateLimitRow.value.port,
        status: 'active',
        downloadMbps: payload.downloadMbps,
        uploadMbps: payload.uploadMbps,
        expireTime: payload.expireTime
      })
      proxy.$modal.msgSuccess('限速已应用')
      rateLimitVisible.value = false
      fetchRateLimits()
    }).catch(() => {}).finally(() => {
      rateLimitSaving.value = false
    })
  })
}
function submitRemoveRateLimit() {
  if (!rateLimitRow.value?.id) return
  proxy.$modal.confirm(`确认要移除端口 ${rateLimitRow.value.port} 的限速吗？`).then(() => {
    rateLimitSaving.value = true
    return removeProxyNodeRateLimit(rateLimitRow.value.id)
  }).then(() => {
    removeRateLimitFromMap(rateLimitRow.value.id)
    proxy.$modal.msgSuccess('限速已移除')
    rateLimitVisible.value = false
    fetchRateLimits()
  }).catch(() => {}).finally(() => {
    rateLimitSaving.value = false
  })
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
  ensureCustomerOptions()
  ensureInstanceOptions()
})

onBeforeUnmount(() => {
  clearExecTimeout()
  clearSpeedPolling()
})

watch(() => props.instanceId, () => {
  speedSnapshot.value = null
  rateLimitMap.value = {}
  clearSpeedPolling()
  getList()
})

watch(() => props.customerId, () => {
  queryParams.pageNum = 1
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
.node-op-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  line-height: 1;
  white-space: nowrap;
}
.node-op-actions :deep(.el-button) {
  margin-left: 0;
  vertical-align: middle;
}
.node-op-dropdown {
  display: inline-flex;
  align-items: center;
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
.rate-limit-cell {
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}
.rate-limit-sub {
  color: var(--el-text-color-secondary);
}
.rate-limit-rule {
  color: var(--el-color-primary);
}
.rate-limit-form :deep(.el-form-item__label) {
  white-space: nowrap;
}
.rate-limit-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 54px;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.rate-limit-input-number {
  width: 100%;
}
.rate-limit-unit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.rate-limit-duration-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
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
