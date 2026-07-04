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
      <el-button type="danger" plain icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete" v-hasPermi="['resource:vps:remove']">批量删除</el-button>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="nodeList" border size="small" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="节点信息" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="node-info-cell">
            <span class="node-info-name">{{ row.nodeName || '-' }}</span>
            <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="地址端口" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="address-port-cell">{{ row.address || '-' }}<span v-if="row.port">:{{ row.port }}</span></span>
        </template>
      </el-table-column>
      <el-table-column label="所属实例" min-width="140" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.instanceId" type="primary" @click="goVpsDetail(row.instanceId)" :underline="false">
            #{{ row.instanceId }} {{ instanceOptions.find(i => i.id === row.instanceId)?.name || '' }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="归属客户" min-width="140" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.customerId" type="primary" @click="goCustomerDetail(row.customerId)" :underline="false">
            #{{ row.customerId }} {{ customerOptions.find(c => c.id === row.customerId)?.username || '' }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="120" align="center">
        <template #default="{ row }">
          <span v-if="!row.expireTime" class="expire-forever">永久</span>
          <span v-else :class="{ 'expire-expired': isExpired(row.expireTime) }">{{ parseTime(row.expireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="流量" min-width="220" align="left" header-align="left">
        <template #default="{ row }">
          <div v-if="trafficMap[row.id]" class="traffic-cell">
            <div class="traffic-cell-line">
              <span class="traffic-cell-label">累计</span>
              <span>{{ nodeTrafficSummaryText(row) }}</span>
            </div>
            <div class="traffic-cell-line">
              <span class="traffic-cell-label">实时</span>
              <span>{{ nodeRealtimeSummaryText(row) }}</span>
            </div>
          </div>
          <span v-else class="text-placeholder">-</span>
        </template>
      </el-table-column>
      <el-table-column label="限速" min-width="170" align="center">
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
      <el-table-column label="操作" width="270" align="center" fixed="right">
        <template #default="{ row }">
          <div class="node-op-actions">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleShare(row)">订阅信息</el-button>
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
        <el-button v-if="detailData?.url" type="primary" plain @click="handleShare(detailData)">订阅信息</el-button>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <el-dialog title="订阅信息" v-model="shareVisible" width="760px" append-to-body destroy-on-close>
      <div v-if="shareData" class="proxy-share-page">
        <div class="proxy-share-header">
          <div>
            <div class="proxy-share-title">{{ shareData.nodeName || shareParsed?.name || '代理节点' }}</div>
            <div class="proxy-share-subtitle">{{ shareParsed ? `${shareParsed.host}:${shareParsed.port}` : 'VLESS 订阅信息' }}</div>
          </div>
          <el-tag v-if="shareData.nodeType" size="small" :type="getNodeTypeTagColor(shareData.nodeType)">{{ shareData.nodeType }}</el-tag>
        </div>

        <section class="proxy-share-section">
          <div class="proxy-share-section-head">
            <div>
              <div class="proxy-share-section-title">VLESS 原始链接</div>
              <div class="proxy-share-section-desc">适用于 v2rayN、v2rayNG 等支持 VLESS-REALITY 的客户端。</div>
            </div>
            <el-button type="primary" size="small" @click="copyToClipboard(shareVlessUrl)">
              <el-icon><DocumentCopy /></el-icon> 复制 VLESS
            </el-button>
          </div>
          <el-input :model-value="shareVlessUrl" readonly type="textarea" :rows="3" />
        </section>

        <section class="proxy-share-section proxy-share-grid">
          <div>
            <div class="proxy-share-section-title">小火箭二维码</div>
            <div class="proxy-share-section-desc">二维码内容为原始 VLESS 链接，小火箭可直接扫码导入。</div>
            <div class="proxy-share-actions">
              <el-button size="small" @click="copyToClipboard(shareVlessUrl)">复制链接</el-button>
              <el-button size="small" @click="downloadQrCode">下载二维码</el-button>
            </div>
          </div>
          <div class="proxy-share-qr">
            <img v-if="shareQrDataUrl" :src="shareQrDataUrl" alt="小火箭导入二维码" />
            <span v-else class="proxy-share-qr-loading">生成中...</span>
          </div>
        </section>

        <section class="proxy-share-section">
          <div class="proxy-share-section-head">
            <div>
              <div class="proxy-share-section-title">Clash Verge 订阅</div>
              <div class="proxy-share-section-desc">使用 ACL4SSR 基础配置，通过 api.wcc.best 转换为 Clash 订阅。</div>
            </div>
            <div class="proxy-share-actions">
              <el-button type="primary" size="small" @click="copyToClipboard(shareClashUrl)">
                <el-icon><DocumentCopy /></el-icon> 复制 Clash 订阅
              </el-button>
              <el-button size="small" @click="openClashSubscribe">打开订阅</el-button>
            </div>
          </div>
          <el-input :model-value="shareClashUrl" readonly type="textarea" :rows="4" />
        </section>
      </div>
      <template #footer>
        <el-button @click="shareVisible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <el-dialog title="修改备注" v-model="remarkEditVisible" width="420px" append-to-body destroy-on-close @closed="remarkEditRow = null">
      <el-input v-model="remarkEditValue" type="textarea" :rows="3" placeholder="选填" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="remarkEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="remarkSaving" @click="submitRemarkEdit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="编辑节点" v-model="editNodeVisible" width="560px" append-to-body destroy-on-close>
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
        <template v-if="canEditRelay">
          <el-form-item label="启用中转">
            <el-switch v-model="editNodeForm.enableRelay" active-text="SOCKS5" />
          </el-form-item>
          <template v-if="editNodeForm.enableRelay">
            <el-form-item label="S5配置">
              <el-input
                v-model="editNodeForm.relayText"
                placeholder="204.1.132.93:35345:lVZjQtlJ:Qat3T6ofak"
                clearable
                @input="parseEditRelayTextToForm"
              />
            </el-form-item>
            <el-form-item label="服务器">
              <el-input v-model="editNodeForm.relayHost" disabled />
            </el-form-item>
            <el-form-item label="中转端口">
              <el-input v-model="editNodeForm.relayPort" disabled />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="editNodeForm.relayUsername" disabled />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="editNodeForm.relayPassword" disabled show-password />
            </el-form-item>
          </template>
        </template>
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

<script setup name="ProxyNodeList">
import QRCode from 'qrcode'
import useUserStore from '@/store/modules/user'
import {
  listProxyNode,
  updateProxyNode,
  delProxyNode,
  forceDelProxyNode,
  listInstance,
  getProxyNodeTraffic,
  getInstanceSpeedSnapshot,
  listProxyNodeRateLimit,
  setProxyNodeRateLimit,
  removeProxyNodeRateLimit
} from '@/api/resource/vps'
import { listCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'
import { buildClashSubscribeUrl, parseVlessUrl, safeProxyShareFilename } from '@/utils/proxyShare'
import { DocumentCopy, Loading, Edit } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const { res_proxy_node_status } = proxy.useDict('res_proxy_node_status')
const router = useRouter()
const userStore = useUserStore()
const hasEditPermi = computed(() => (userStore.permissions || []).some(p => p === '*:*:*' || p === 'resource:vps:edit'))

const nodeTypeOptions = [
  { value: 'VLESS-REALITY', label: 'VLESS-REALITY' },
  { value: 'VMess-TCP', label: 'VMess-TCP' }
]

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

const tableRef = ref(null)
const selectedIds = ref([])
const statusLoadingId = ref(null)
const deleteLoadingId = ref(null)
const batchDeleteLoading = ref(false)
const trafficMap = ref({})
const speedSnapshot = ref(null)
const speedRefreshing = ref(false)
let speedTimer = null
const SPEED_REFRESH_MS = 8000
const rateLimitMap = ref({})
const detailVisible = ref(false)
const detailData = ref(null)
const detailTraffic = ref(null)
const detailConfig = computed(() => {
  if (!detailData.value?.configJson) return null
  try { return JSON.parse(detailData.value.configJson) } catch { return null }
})
const shareVisible = ref(false)
const shareData = ref(null)
const shareVlessUrl = computed(() => (shareData.value?.url || '').trim())
const shareParsed = computed(() => {
  if (!shareVlessUrl.value) return null
  try { return parseVlessUrl(shareVlessUrl.value) } catch { return null }
})
const shareClashUrl = computed(() => shareVlessUrl.value ? buildClashSubscribeUrl(shareVlessUrl.value) : '')
const shareQrDataUrl = ref('')
const shareBaseName = computed(() => shareData.value?.nodeName || shareParsed.value?.name || 'proxy-share')

const remarkEditVisible = ref(false)
const remarkEditRow = ref(null)
const remarkEditValue = ref('')
const remarkSaving = ref(false)
const editNodeVisible = ref(false)
const editNodeSaving = ref(false)
const editNodeRow = ref(null)
const editNodePermanent = ref(false)
const editNodeForm = reactive({
  expireTime: null,
  url: '',
  originalRelay: false,
  enableRelay: false,
  relayText: '',
  relayHost: '',
  relayPort: '',
  relayUsername: '',
  relayPassword: ''
})
const canEditRelay = computed(() => editNodeRow.value?.nodeType === 'VLESS-REALITY')
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
const queryRef = ref(null)

function getList() {
  loading.value = true
  listProxyNode(queryParams.value).then(res => {
    nodeList.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
    syncRateLimitsFromRows(nodeList.value)
    fetchTrafficForList(nodeList.value)
    fetchRateLimits()
    refreshInstanceSpeed()
    restartSpeedPolling()
  }).catch(() => { loading.value = false })
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
  const instanceIds = Array.from(new Set((nodeList.value || []).map(row => row.instanceId).filter(id => id != null)))
  if (instanceIds.length === 0) {
    applyRateLimitMapToRows(rateLimitMap.value)
    return
  }
  const requests = instanceIds.map(instanceId => listProxyNodeRateLimit({ instanceId }).then(res => res.data || []).catch(() => []))
  Promise.all(requests).then(results => {
    const map = buildRateLimitMap([
      ...Object.values(rateLimitMap.value || {}),
      ...results.flat()
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
  if (detailData.value?.id === normalized.proxyNodeId) {
    detailData.value.rateLimit = normalized
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
  if (detailData.value?.id != null) {
    detailData.value.rateLimit = map[detailData.value.id] || detailData.value.rateLimit || null
  }
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
  if (detailData.value?.id === proxyNodeId) {
    detailData.value.rateLimit = null
  }
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
  getInstanceSpeedSnapshot().then(res => {
    speedSnapshot.value = res.data || null
  }).catch(() => {
    speedSnapshot.value = { error: true }
  }).finally(() => {
    speedRefreshing.value = false
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

function resolveSpeedSnapshot(row) {
  if (!speedSnapshot.value) return null
  const instanceId = row?.instanceId
  if (instanceId == null) return null
  return speedSnapshot.value[String(instanceId)] || speedSnapshot.value[instanceId] || null
}

function nodeRealtimeSpeedText(row) {
  if (!row || speedSnapshot.value?.error) return '实时：-'
  const port = row.port != null ? String(row.port) : ''
  const snapshot = resolveSpeedSnapshot(row)
  const speed = port && snapshot?.ports ? snapshot.ports[port] : null
  if (!speed) return '实时：-'
  return '实时：↑ ' + formatSpeedFromMb(speed.upMbps) + ' / ↓ ' + formatSpeedFromMb(speed.downMbps)
}

function getNodeRealtimeSpeed(row) {
  if (!row || speedSnapshot.value?.error) return null
  const port = row.port != null ? String(row.port) : ''
  const snapshot = resolveSpeedSnapshot(row)
  return port && snapshot?.ports ? snapshot.ports[port] : null
}

function nodeTrafficTotalText(row, traffic) {
  const stat = traffic || trafficMap.value[row.id]
  return '累计：' + (stat ? '↑ ' + formatTraffic(stat.totalTx) + ' / ↓ ' + formatTraffic(stat.totalRx) : '-')
}

function nodeTrafficSummaryText(row) {
  const stat = row ? trafficMap.value[row.id] : null
  return stat ? '↑ ' + formatTraffic(stat.totalTx) + ' / ↓ ' + formatTraffic(stat.totalRx) : '-'
}

function nodeRealtimeSummaryText(row) {
  const speed = getNodeRealtimeSpeed(row)
  return speed ? '↑ ' + formatSpeedFromMb(speed.upMbps) + ' / ↓ ' + formatSpeedFromMb(speed.downMbps) : '-'
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

function handleShare(row) {
  if (!row?.url) {
    proxy.$modal.msgWarning('该节点暂无分享链接')
    return
  }
  try {
    parseVlessUrl(row.url)
  } catch (e) {
    proxy.$modal.msgWarning(e.message || '仅支持 VLESS 分享链接')
    return
  }
  shareData.value = { ...row }
  shareVisible.value = true
  refreshShareQrCode()
}

function refreshShareQrCode() {
  shareQrDataUrl.value = ''
  if (!shareVlessUrl.value) return
  QRCode.toDataURL(shareVlessUrl.value, {
    width: 220,
    margin: 1,
    errorCorrectionLevel: 'M'
  }).then(url => {
    if (shareVlessUrl.value) shareQrDataUrl.value = url
  }).catch(() => {
    proxy.$modal.msgWarning('二维码生成失败，请直接复制 VLESS 链接')
  })
}

function downloadQrCode() {
  if (!shareQrDataUrl.value) return
  const a = document.createElement('a')
  a.href = shareQrDataUrl.value
  a.download = safeProxyShareFilename(shareBaseName.value + '-qrcode', 'png')
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

function openClashSubscribe() {
  if (!shareClashUrl.value) return
  window.open(shareClashUrl.value, '_blank', 'noopener,noreferrer')
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

function resetEditRelayForm() {
  editNodeForm.originalRelay = false
  editNodeForm.enableRelay = false
  editNodeForm.relayText = ''
  editNodeForm.relayHost = ''
  editNodeForm.relayPort = ''
  editNodeForm.relayUsername = ''
  editNodeForm.relayPassword = ''
}

function parseEditRelayTextToForm() {
  const parsed = parseSocks5RelayText(editNodeForm.relayText)
  editNodeForm.relayHost = parsed.ok ? parsed.host : ''
  editNodeForm.relayPort = parsed.ok ? parsed.port : ''
  editNodeForm.relayUsername = parsed.ok ? parsed.username : ''
  editNodeForm.relayPassword = parsed.ok ? parsed.password : ''
}

function relayTextFromRow(row) {
  const relay = configFromRow(row)?.relay
  if (relay?.server && relay?.serverPort && relay?.username && relay?.password) {
    return `${relay.server}:${relay.serverPort}:${relay.username}:${relay.password}`
  }
  const remark = (row?.remark || '').trim()
  return parseSocks5RelayText(remark).ok ? remark : ''
}

function configFromRow(row) {
  if (!row?.configJson) return null
  try { return JSON.parse(row.configJson) } catch { return null }
}

function buildConfigJsonWithRelay(configJson, relayText) {
  const parsed = parseSocks5RelayText(relayText)
  if (!parsed.ok) return configJson
  let config = {}
  try { config = configJson ? JSON.parse(configJson) : {} } catch { config = {} }
  config.relay = {
    type: 'socks5',
    server: parsed.host,
    serverPort: Number(parsed.port),
    username: parsed.username,
    password: parsed.password
  }
  return JSON.stringify(config)
}

function removeRelayFromConfigJson(configJson) {
  if (!configJson) return configJson
  try {
    const config = JSON.parse(configJson)
    delete config.relay
    return JSON.stringify(config)
  } catch {
    return configJson
  }
}

function openNodeEdit(row) {
  editNodeRow.value = row
  editNodeForm.expireTime = row.expireTime || null
  editNodeForm.url = row.url || ''
  editNodePermanent.value = !row.expireTime
  resetEditRelayForm()
  if (row.nodeType === 'VLESS-REALITY') {
    const relayText = relayTextFromRow(row)
    if (relayText) {
      editNodeForm.originalRelay = true
      editNodeForm.enableRelay = true
      editNodeForm.relayText = relayText
      parseEditRelayTextToForm()
    }
  }
  editNodeVisible.value = true
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
        instanceId: rateLimitRow.value.instanceId,
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
  if (editNodeForm.enableRelay && canEditRelay.value) {
    const relayText = (editNodeForm.relayText || '').trim()
    const parsedRelay = parseSocks5RelayText(relayText)
    if (!parsedRelay.ok) {
      proxy.$modal.msgWarning(parsedRelay.message)
      return
    }
    payload.relayText = relayText
  } else if (editNodeForm.originalRelay && canEditRelay.value) {
    payload.relayEnabled = false
  }
  editNodeSaving.value = true
  updateProxyNode(payload).then(() => {
    const row = editNodeRow.value
    row.expireTime = payload.expireTime
    row.url = payload.url
    row.nodeName = buildNodeNameByExpire(row, payload.expireTime)
    if (payload.relayText) {
      row.remark = payload.relayText
      row.configJson = buildConfigJsonWithRelay(row.configJson, payload.relayText)
    } else if (payload.relayEnabled === false) {
      row.remark = ''
      row.configJson = removeRelayFromConfigJson(row.configJson)
    }
    if (detailData.value?.id === row.id) {
      detailData.value.expireTime = row.expireTime
      detailData.value.url = row.url
      detailData.value.nodeName = row.nodeName
      detailData.value.remark = row.remark
      detailData.value.configJson = row.configJson
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
    detailVisible.value = false
    selectedIds.value = []
    tableRef.value?.clearSelection?.()
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
  if (!row?.id) return
  proxy.$modal.confirm(`确认要删除节点"${row.nodeName}"吗？将同时在服务器上删除配置。`).then(() => {
    deleteLoadingId.value = row.id
    proxy.$modal.loading('正在删除节点...')
    return delProxyNode(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    detailVisible.value = false
    getList()
  }).catch(() => {}).finally(() => {
    proxy.$modal.closeLoading()
    deleteLoadingId.value = null
  })
}

function handleForceDelete(row) {
  if (!row?.id) return
  proxy.$modal.confirm(`确认要强制删除节点"${row.nodeName}"吗？此操作只删除本地节点和流量记录，不连接服务器，也不会清理服务器上的残留配置。`).then(() => {
    deleteLoadingId.value = row.id
    proxy.$modal.loading('正在强制删除节点...')
    return forceDelProxyNode(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('强制删除成功')
    detailVisible.value = false
    getList()
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

onMounted(() => {
  loadOptions()
  getList()
})

onBeforeUnmount(() => {
  clearSpeedPolling()
})
</script>

<style scoped>
.expire-forever { color: var(--el-color-success); font-weight: 500; }
.expire-expired { color: var(--el-color-danger); font-weight: 500; }
.share-url-section { margin-top: 16px; padding: 12px; background: var(--el-fill-color-light); border-radius: 6px; }
.share-url-label { font-size: 13px; font-weight: 500; margin-bottom: 8px; }
.share-url-box { display: flex; flex-direction: column; }
.node-info-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.node-info-name {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.address-port-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  color: var(--el-text-color-primary);
  word-break: break-all;
}
.proxy-share-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.proxy-share-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.proxy-share-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 24px;
  word-break: break-all;
}
.proxy-share-subtitle {
  margin-top: 2px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.proxy-share-section {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}
.proxy-share-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.proxy-share-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 22px;
}
.proxy-share-section-desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 18px;
}
.proxy-share-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 244px;
  align-items: center;
  gap: 18px;
}
.proxy-share-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.proxy-share-actions :deep(.el-button) {
  margin-left: 0;
}
.proxy-share-qr {
  width: 244px;
  min-height: 244px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
}
.proxy-share-qr img {
  width: 220px;
  height: 220px;
  display: block;
}
.status-cell { display: inline-flex; align-items: center; gap: 6px; }
.status-loading { font-size: 14px; margin-right: 2px; }
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
.text-placeholder { color: var(--el-text-color-placeholder); }
.traffic-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  line-height: 1.35;
  white-space: nowrap;
}
.traffic-cell--detail {
  align-items: flex-start;
}
.traffic-cell-line {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 4px;
  width: 100%;
  font-size: 12px;
  color: var(--el-text-color-primary);
}
.traffic-cell-label {
  color: var(--el-text-color-secondary);
}
.traffic-cell-total {
  color: var(--el-text-color-primary);
}
.traffic-cell-speed {
  color: var(--el-text-color-secondary);
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
