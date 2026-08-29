<template>
  <div class="app-container">
    <el-row :gutter="20">
      <splitpanes :horizontal="appStore.device === 'mobile'" class="default-theme">
        <pane size="18">
          <el-col>
            <div class="head-container">
              <span class="head-label">VPS 分类</span>
              <el-button type="primary" link icon="Plus" @click="handleAddCategory(null)" v-hasPermi="['resource:vps:add']">新增</el-button>
            </div>
            <div class="head-container">
              <el-tree
                ref="categoryTreeRef"
                :data="categoryOptions"
                :props="{ label: 'name', children: 'children' }"
                :expand-on-click-node="false"
                node-key="id"
                highlight-current
                default-expand-all
                @node-click="handleCategoryNodeClick"
              >
                <template #default="{ node, data }">
                  <span class="custom-tree-node">
                    <span>{{ node.label }}</span>
                    <el-dropdown v-if="data.type === '1'" trigger="click" @command="(cmd) => handleTreeMenuCommand(cmd, data)" @click.stop>
                      <el-button link type="primary" size="small" icon="Operation" class="tree-op-btn" />
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="addChild" icon="Plus" v-hasPermi="['resource:vps:add']">新增子级</el-dropdown-item>
                          <el-dropdown-item command="addSibling" icon="CopyDocument" v-hasPermi="['resource:vps:add']">新增同级</el-dropdown-item>
                          <el-dropdown-item command="edit" icon="Edit" v-hasPermi="['resource:vps:edit']">编辑</el-dropdown-item>
                          <el-dropdown-item command="delete" icon="Delete" divided v-hasPermi="['resource:vps:remove']">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </span>
                </template>
              </el-tree>
            </div>
          </el-col>
        </pane>
        <pane size="82">
          <el-col>
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="72px" class="query-form">
              <el-form-item label="关键字" prop="keyword">
                <el-input v-model="queryParams.keyword" placeholder="名称、编号或 IP" clearable style="width: 180px" @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 110px">
                  <el-option v-for="dict in res_instance_status" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="网络类型" prop="networkType">
                <el-select v-model="queryParams.networkType" placeholder="全部" clearable style="width: 120px">
                  <el-option v-for="dict in res_instance_network_type" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="系统类型" prop="osType">
                <el-select v-model="queryParams.osType" placeholder="全部" clearable style="width: 110px">
                  <el-option v-for="t in osTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <el-row :gutter="10" class="mb8">
              <el-col :span="1.5">
                <el-button type="primary" plain icon="Plus" @click="handleAddInstance" v-hasPermi="['resource:vps:add']">新增VPS</el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['resource:vps:export']">导出</el-button>
              </el-col>
              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
            </el-row>
            <el-alert v-if="categoryOptions.length > 0 && queryParams.categoryId == null" type="info" :closable="false" show-icon class="mb8" style="margin-bottom: 12px">
              未选择分类时显示全部 VPS；在左侧选择分类/节点可筛选列表。
            </el-alert>

            <el-table v-loading="loading" :data="instanceList" :row-class-name="() => 'vps-table-row'">
              <el-table-column label="编号" align="center" prop="id" width="72">
                <template #default="scope">
                  <span>{{ scope.row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column label="主机信息" align="left" prop="name" min-width="300" show-overflow-tooltip class-name="vps-name-column">
                <template #default="scope">
                  <div class="vps-row-wrap">
                    <div class="vps-icon-box" :title="statusLabel(scope.row.status)">
                      <img :src="serverIcon" class="vps-icon-box-icon" alt="" />
                      <i v-if="scope.row.status != null" class="vps-status-badge" :class="'vps-status-badge--' + (scope.row.status || '')" />
                    </div>
                    <div class="vps-name-cell">
                      <div class="vps-name-row">
                        <el-link type="primary" :underline="false" @click="goDetail(scope.row.id)" class="vps-name-link" :title="displayName(scope.row)">{{ displayName(scope.row) }}</el-link>
                      </div>
                      <div class="vps-name-sub">
                        <span v-if="scope.row.ip" class="vps-name-ip-wrap" :title="scope.row.ip">
                          <span class="vps-name-ip">{{ scope.row.ip }}</span>
                          <el-tooltip content="复制 IP" placement="top">
                            <el-icon class="vps-name-ip-copy" @click.stop="copyIp(scope.row.ip)"><DocumentCopy /></el-icon>
                          </el-tooltip>
                        </span>
                        <span v-if="scope.row.ip && hasOsInfo(scope.row)" class="vps-name-sep" />
                        <span class="vps-name-os">
                          <img v-if="scope.row.osType || scope.row.osVersion" :src="osIconUrl(scope.row.osType)" class="vps-os-icon" alt="" />
                          <img v-else :src="osIconMap.other" class="vps-os-icon" alt="" />
                          {{ osDisplayText(scope.row) || '未知系统' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="配置" align="left" min-width="320" show-overflow-tooltip>
                <template #default="scope">
                  <div class="vps-config-tags">
                    <span v-if="scope.row.cpu" class="vps-config-tag vps-config-tag--cpu"><el-icon><Cpu /></el-icon>{{ scope.row.cpu }}</span>
                    <span v-if="scope.row.memory" class="vps-config-tag vps-config-tag--memory"><el-icon><Coin /></el-icon>{{ scope.row.memory }}</span>
                    <span v-if="scope.row.disk" class="vps-config-tag vps-config-tag--disk"><el-icon><Folder /></el-icon>{{ scope.row.disk }}</span>
                    <span v-if="scope.row.bandwidth" class="vps-config-tag vps-config-tag--bandwidth"><el-icon><Lightning /></el-icon>{{ formatBandwidth(scope.row.bandwidth) }}</span>
                    <span class="vps-config-tag vps-config-tag--traffic">
                      <el-icon><RefreshRight /></el-icon>{{ configTrafficText(scope.row) }}
                    </span>
                    <span v-if="networkTypeLabel(scope.row.networkType)" class="vps-config-tag vps-config-tag--network"><el-icon><Link /></el-icon>{{ networkTypeLabel(scope.row.networkType) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="分类" align="left" prop="categoryName" width="90" show-overflow-tooltip>
                <template #default="scope">
                  <span>{{ scope.row.categoryName }}</span>
                </template>
              </el-table-column>
              <el-table-column label="节点数" align="center" prop="nodeCount" width="72">
                <template #default="scope">
                  <span>{{ scope.row.nodeCount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="累计流量" align="center" prop="totalTrafficBytes" min-width="230" show-overflow-tooltip>
                <template #default="scope">
                  <div class="traffic-cell">
                    <div class="traffic-cell-total">{{ trafficTotalText(scope.row) }}</div>
                    <div class="traffic-cell-speed">{{ realtimeSpeedText(scope.row) }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="到期时间" align="center" prop="expireTime" width="128" show-overflow-tooltip>
                <template #default="scope">
                  <span>
                    <span v-if="!scope.row.expireTime">-</span>
                    <span v-else :class="{ 'expire-expired': isExpired(scope.row.expireTime) }">{{ formatExpireTime(scope.row.expireTime) }}</span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width vps-op-cell" fixed="right">
                <template #default="scope">
                  <div class="op-btns">
                    <el-button link icon="Connection" class="op-btn" @click="handleConnectServer(scope.row)" v-hasPermi="['resource:vps:list']">连接</el-button>
                    <el-button link icon="View" class="op-btn" @click="goDetail(scope.row.id)" v-hasPermi="['resource:vps:query']">详情</el-button>
                    <el-dropdown trigger="click" @command="(cmd) => handleInstanceCommand(cmd, scope.row)" v-hasPermi="['resource:vps:list', 'resource:vps:query', 'resource:vps:add', 'resource:vps:edit', 'resource:vps:remove']">
                      <el-button link icon="DArrowRight" class="op-btn op-dropdown-trigger">更多</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="accessLog" icon="View" v-hasPermi="['resource:vps:list', 'resource:vps:query']">访问日志</el-dropdown-item>
                          <el-dropdown-item command="clone" icon="CopyDocument" v-hasPermi="['resource:vps:add']">克隆</el-dropdown-item>
                          <el-dropdown-item command="edit" icon="Edit" v-hasPermi="['resource:vps:edit']">编辑</el-dropdown-item>
                          <el-dropdown-item command="delete" icon="Delete" v-hasPermi="['resource:vps:remove']">删除</el-dropdown-item>
                          <el-dropdown-item command="forceDelete" icon="DeleteFilled" divided v-hasPermi="['resource:vps:remove']">强制删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
          </el-col>
        </pane>
      </splitpanes>
    </el-row>

    <!-- 分类 新增/编辑 -->
    <el-dialog :title="categoryTitle" v-model="categoryOpen" width="480px" append-to-body>
      <el-form ref="categoryRef" :model="categoryForm" :rules="categoryRules" label-width="96px">
        <el-form-item label="上级分类" prop="parentId" v-if="categoryForm.id == null">
          <el-tree-select
            v-model="categoryForm.parentId"
            :data="categoryOptions"
            :props="{ value: 'id', label: 'name', children: 'children' }"
            value-key="id"
            placeholder="不选则为顶级"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="分类名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="categoryForm.orderNum" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitCategoryForm">确 定</el-button>
        <el-button @click="categoryOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- VPS 实例 新增/编辑/克隆 -->
    <el-dialog :title="instanceTitle" v-model="instanceOpen" width="760px" class="vps-instance-dialog" append-to-body>
      <el-form ref="instanceRef" :model="instanceForm" :rules="instanceRules" label-position="top" class="vps-instance-form">
        <div class="vps-form-section-title">
          <span>基本信息</span>
          <small>填写服务器名称、归属和系统信息</small>
        </div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="VPS名称" prop="name">
              <el-input v-model="instanceForm.name" placeholder="请输入名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-tree-select
                v-model="instanceForm.categoryId"
                :data="categoryOptions"
                :props="{ value: 'id', label: 'name', children: 'children' }"
                value-key="id"
                placeholder="请选择分类"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网络类型" prop="networkType">
              <el-select v-model="instanceForm.networkType" placeholder="请选择网络类型" clearable style="width: 100%">
                <el-option v-for="dict in res_instance_network_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统类型" prop="osType">
              <el-select v-model="instanceForm.osType" placeholder="先选系统类型" clearable style="width: 100%" @change="onOsTypeChange">
                <el-option v-for="t in osTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统版本" prop="osVersion">
              <el-select v-model="instanceForm.osVersion" placeholder="再选版本（可自动识别）" clearable filterable allow-create style="width: 100%">
                <el-option v-for="v in osVersionOptionsForType" :key="v" :label="v" :value="v" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="带宽" prop="bandwidth">
              <el-input v-model="instanceForm.bandwidth" placeholder="如 50M" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="vps-form-section-title">
          <span>连接信息</span>
          <small>用于连接服务器和自动识别硬件配置</small>
        </div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="IP" prop="ip">
              <el-input v-model="instanceForm.ip" placeholder="如 192.168.1.1" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SSH端口" prop="sshPort">
              <el-input-number v-model="instanceForm.sshPort" :min="1" :max="65535" controls-position="right" placeholder="22" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SSH账号" prop="sshUsername">
              <el-input v-model="instanceForm.sshUsername" placeholder="SSH登录账号" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SSH密码" prop="sshPassword">
              <el-input v-model="instanceForm.sshPassword" type="password" placeholder="SSH登录密码" maxlength="255" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="vps-connection-test">
          <div class="vps-connection-test__text">
            <strong>自动识别服务器配置</strong>
            <span>验证 SSH 连接，并自动回写 CPU、内存、磁盘及系统版本</span>
          </div>
          <el-button type="primary" plain icon="Connection" :loading="testConnectionLoading" @click="handleTestConnection">测试连接</el-button>
        </div>
        <div class="vps-form-section-title">
          <span>规格与备注</span>
          <small>完善资源规格、续费及到期信息</small>
        </div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="CPU" prop="cpu">
              <el-input v-model="instanceForm.cpu" placeholder="如 2核" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="内存" prop="memory">
              <el-input v-model="instanceForm.memory" placeholder="如 4G" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="磁盘" prop="disk">
              <el-input v-model="instanceForm.disk" placeholder="如 50G" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="流量限制" prop="trafficLimitGb">
              <el-input-number v-model="instanceForm.trafficLimitGb" :min="0" placeholder="留空或0为不限制(单位GB)" controls-position="right" style="width: 100%" clearable />
              <span class="form-tip">单位 GB，0 或留空表示不限制</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="续费金额" prop="renewalAmount">
              <el-input v-model="instanceForm.renewalAmount" placeholder="如 10/月、100/年" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="到期时间" prop="expireTime">
              <el-date-picker v-model="instanceForm.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择到期时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="instanceForm.remark" type="textarea" placeholder="备注" maxlength="500" show-word-limit :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="vps-instance-footer">
          <el-button @click="instanceOpen = false">取 消</el-button>
          <el-button type="primary" @click="submitInstanceForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <AccessLogDialog
      v-model="accessLogVisible"
      scope="vps"
      :instance-id="accessLogInstance?.id"
      :title="`${accessLogInstance?.name || accessLogInstance?.ip || 'VPS'} - 访问日志`"
    />

  </div>
</template>

<script setup name="Vps">
import { computed } from 'vue'
import { Cpu, Coin, Folder, Lightning, RefreshRight, Connection, Link, DocumentCopy } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/skyway'
import serverIcon from '@/assets/images/os/server.svg'
import ubuntuIcon from '@/assets/images/os/ubuntu.svg'
import centosIcon from '@/assets/images/os/centos.svg'
import debianIcon from '@/assets/images/os/debian.svg'
import alpineIcon from '@/assets/images/os/alpine.svg'
import otherIcon from '@/assets/images/os/other.svg'
import AccessLogDialog from './components/AccessLogDialog.vue'

const osIconMap = {
  ubuntu: ubuntuIcon,
  centos: centosIcon,
  debian: debianIcon,
  alpine: alpineIcon,
  other: otherIcon
}
function osIconUrl(osType) {
  return osIconMap[osType] || osIconMap.other
}
function osDisplayText(row) {
  if (!row.osType && !row.osVersion) return ''
  const parts = [osTypeLabel(row.osType), row.osVersion, 'x64'].filter(Boolean)
  return parts.join('-')
}
import useAppStore from '@/store/modules/app'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import {
  listCategoryTree,
  getCategory,
  addCategory,
  updateCategory,
  delCategory,
  listInstance,
  getInstance,
  getInstanceSpeedSnapshot,
  addInstance,
  updateInstance,
  delInstance,
  forceDelInstance,
  testConnection
} from '@/api/resource/vps'
const { proxy } = getCurrentInstance()
const { res_instance_status, res_instance_network_type } = proxy.useDict('res_instance_status', 'res_instance_network_type')
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const categoryOptions = ref([])
const categoryTreeRef = ref(null)
const categoryOpen = ref(false)
const categoryTitle = ref('')
const categoryForm = ref({ name: '', parentId: 0, orderNum: 0, type: '1' })
const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  orderNum: [
    { required: true, message: '请输入排序', trigger: 'blur' },
    {
      validator: (rule, value, cb) => {
        if (value == null || value === '') return cb(new Error('请输入排序'))
        const n = Number(value)
        if (Number.isInteger(n) && n >= 0) return cb()
        cb(new Error('排序为不小于 0 的整数'))
      },
      trigger: 'blur'
    }
  ]
}
const categoryRef = ref(null)

const instanceList = ref([])
const total = ref(0)
const loading = ref(true)
const showSearch = ref(true)
const speedMap = ref({})
const speedRefreshing = ref(false)
let speedTimer = null
const SPEED_REFRESH_MS = 5000
const instanceOpen = ref(false)
const instanceTitle = ref('')
const osTypeOptions = [
  { value: 'centos', label: 'CentOS' },
  { value: 'ubuntu', label: 'Ubuntu' },
  { value: 'debian', label: 'Debian' },
  { value: 'alpine', label: 'Alpine' },
  { value: 'other', label: '其他' }
]
const osVersionOptionsByType = {
  centos: ['6', '7', '7.9', '8', '8.5', '9'],
  ubuntu: ['18.04', '20.04', '22.04', '24.04', '25.04'],
  debian: ['9', '10', '11', '12'],
  alpine: ['3.18', '3.19'],
  other: []
}
const osVersionOptionsForType = computed(() => {
  const t = instanceForm.value.osType
  return (t && osVersionOptionsByType[t]) ? osVersionOptionsByType[t] : []
})
function osTypeLabel(value) {
  const o = osTypeOptions.find(t => t.value === value)
  return o ? o.label : (value || '')
}
function statusLabel(value) {
  const list = res_instance_status.value ?? res_instance_status
  const arr = Array.isArray(list) ? list : []
  return (arr.find(d => d.value === value) || {}).label || ''
}
function networkTypeLabel(value) {
  const list = res_instance_network_type.value ?? res_instance_network_type
  const arr = Array.isArray(list) ? list : []
  return (arr.find(d => d.value === value) || {}).label || ''
}
function configTrafficText(row) {
  const limit = row.trafficLimit
  if (limit == null || limit === 0) return '月流量无限制'
  return '月流量 ' + formatTraffic(limit)
}
function formatBandwidth(val) {
  if (val == null || String(val).trim() === '') return ''
  const s = String(val).trim()
  if (/[MmGgKk]$/.test(s)) return s
  return s + 'M'
}
function displayName(row) {
  const name = (row && row.name) ? String(row.name).trim() : ''
  const ip = (row && row.ip) ? String(row.ip).trim() : ''
  if (!name || name === ip) return '未命名服务器 (Unnamed Server)'
  return name
}
function hasOsInfo(row) {
  return !!(row && (row.osType || row.osVersion))
}
function copyIp(ip) {
  if (!ip) return
  const text = String(ip).trim()
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      proxy.$modal.msgSuccess('IP 已复制')
    }).catch(() => {
      fallbackCopy(text)
    })
  } else {
    fallbackCopy(text)
  }
}
function fallbackCopy(text) {
  try {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    proxy.$modal.msgSuccess('IP 已复制')
  } catch (e) {
    proxy.$modal.msgError('复制失败')
  }
}
function onOsTypeChange() {
  const t = instanceForm.value.osType
  const vers = (t && osVersionOptionsByType[t]) ? osVersionOptionsByType[t] : []
  if (instanceForm.value.osVersion && !vers.includes(instanceForm.value.osVersion)) {
    instanceForm.value.osVersion = ''
  }
}
const instanceForm = ref({
  name: '',
  categoryId: undefined,
  networkType: undefined,
  osType: '',
  osVersion: '',
  bandwidth: '',
  ip: '',
  sshPort: 22,
  sshUsername: 'root',
  sshPassword: '',
  cpu: '',
  memory: '',
  disk: '',
  status: 'running',
  trafficLimit: null,
  trafficLimitGb: undefined,
  renewalAmount: '',
  expireTime: null,
  remark: ''
})
const instanceRules = {
  name: [
    { required: true, message: '请输入VPS名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  ip: [
    { required: true, message: '请输入IP地址', trigger: 'blur' },
    {
      pattern: /^(\d{1,3}\.){3}\d{1,3}$/,
      message: '请输入有效的 IPv4 地址',
      trigger: 'blur'
    }
  ],
  sshPort: [
    { required: true, message: '请输入SSH端口', trigger: 'blur' },
    {
      validator: (rule, value, cb) => {
        if (value == null || value === '') return cb(new Error('请输入SSH端口'))
        const n = Number(value)
        if (Number.isInteger(n) && n >= 1 && n <= 65535) return cb()
        cb(new Error('端口范围为 1-65535'))
      },
      trigger: 'blur'
    }
  ],
  sshUsername: [
    { required: true, message: '请输入SSH账号', trigger: 'blur' },
    { max: 64, message: '长度不能超过 64 个字符', trigger: 'blur' }
  ],
  sshPassword: [
    { required: true, message: '请输入SSH密码', trigger: 'blur' },
    { max: 255, message: '长度不能超过 255 个字符', trigger: 'blur' }
  ]
}
const instanceRef = ref(null)
const testConnectionLoading = ref(false)

function handleTestConnection() {
  const form = instanceForm.value
  if (!form.ip || !form.sshUsername) {
    proxy.$modal.msgWarning('请先填写 IP 和 SSH 账号')
    return
  }
  testConnectionLoading.value = true
  testConnection({
    ip: form.ip,
    sshPort: form.sshPort != null ? form.sshPort : 22,
    sshUsername: form.sshUsername,
    sshPassword: form.sshPassword || ''
  }).then(res => {
    testConnectionLoading.value = false
    const data = res.data || res
    if (data.success) {
      if (data.cpu) form.cpu = data.cpu
      if (data.memory) form.memory = data.memory
      if (data.disk) form.disk = data.disk
      if (data.osType) form.osType = data.osType
      if (data.osVersion) form.osVersion = data.osVersion
      proxy.$modal.msgSuccess(data.message || '连接成功，已回写规格')
    } else {
      proxy.$modal.msgError(data.message || '连接失败')
    }
  }).catch(e => {
    testConnectionLoading.value = false
    proxy.$modal.msgError(e.msg || e.message || '连接测试失败')
  })
}

function handleConnectServer(row) {
  router.push({
    name: 'VpsTerminal',
    params: { id: row.id },
    query: { name: row.name || '', ip: row.ip || '' }
  })
}

const accessLogVisible = ref(false)
const accessLogInstance = ref(null)
function openVpsAccessLog(row) {
  accessLogInstance.value = row
  accessLogVisible.value = true
}

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  status: undefined,
  networkType: undefined,
  osType: undefined,
  categoryId: undefined
})

/** 加载分类树 */
function getCategoryTree() {
  listCategoryTree().then(res => {
    categoryOptions.value = res.data || []
    if (categoryOptions.value.length > 0 && !queryParams.value.categoryId) {
      const firstId = getFirstCategoryId(categoryOptions.value)
      if (firstId != null) {
        queryParams.value.categoryId = firstId
        nextTick(() => {
          if (categoryTreeRef.value) categoryTreeRef.value.setCurrentKey(firstId)
        })
      }
    }
    getList()
  })
}

function getFirstCategoryId(nodes) {
  if (!nodes || nodes.length === 0) return null
  return nodes[0].id
}

/** 分类节点点击：仅查当前分类的 VPS（精确 category_id，不包含子分类；上级节点无直接实例时列表为空） */
function handleCategoryNodeClick(data) {
  queryParams.value.categoryId = data.id
  queryParams.value.pageNum = 1
  getList()
}

/** 新增分类（data 为当前节点，作为父级） */
function handleAddCategory(data) {
  categoryForm.value = { id: undefined, name: '', parentId: data ? data.id : 0, orderNum: 0, type: '1' }
  categoryTitle.value = '新增分类'
  categoryOpen.value = true
  nextTick(() => proxy.resetForm('categoryRef'))
}

/** 新增同级分类 */
function handleAddSiblingCategory(data) {
  const parentId = data.parentId != null ? data.parentId : 0
  categoryForm.value = { id: undefined, name: '', parentId, orderNum: 0, type: '1' }
  categoryTitle.value = '新增分类（同级）'
  categoryOpen.value = true
  nextTick(() => proxy.resetForm('categoryRef'))
}

/** 编辑分类 */
function handleEditCategory(data) {
  getCategory(data.id).then(res => {
    categoryForm.value = { ...res.data, type: '1' }
    categoryTitle.value = '编辑分类'
    categoryOpen.value = true
  })
}

/** 删除分类 */
function handleDeleteCategory(data) {
  proxy.$modal.confirm('是否确认删除该分类？').then(() => {
    return delCategory(data.id)
  }).then(() => {
    getCategoryTree()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(e => {
    if (e && e.message) proxy.$modal.msgError(e.message)
  })
}

/** 树节点操作下拉命令 */
function handleTreeMenuCommand(command, data) {
  switch (command) {
    case 'addChild':
      handleAddCategory(data)
      break
    case 'addSibling':
      handleAddSiblingCategory(data)
      break
    case 'edit':
      handleEditCategory(data)
      break
    case 'delete':
      handleDeleteCategory(data)
      break
    default:
      break
  }
}

/** 提交分类表单 */
function submitCategoryForm() {
  proxy.$refs.categoryRef.validate(valid => {
    if (!valid) return
    const parentId = categoryForm.value.parentId ?? 0
    const payload = { name: categoryForm.value.name, orderNum: categoryForm.value.orderNum || 0, type: '1', parentId }
    if (categoryForm.value.id) {
      payload.id = categoryForm.value.id
      updateCategory(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        categoryOpen.value = false
        getCategoryTree()
      })
    } else {
      addCategory(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        categoryOpen.value = false
        getCategoryTree()
      })
    }
  })
}

/** 实例列表 */
function getList() {
  loading.value = true
  listInstance(queryParams.value).then(res => {
    instanceList.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
    pruneSpeedMap(instanceList.value)
    refreshCurrentPageSpeed()
    restartSpeedPolling()
  })
}

function pruneSpeedMap(rows) {
  const ids = new Set((rows || []).map(row => String(row.id)).filter(Boolean))
  const next = {}
  Object.keys(speedMap.value || {}).forEach(id => {
    if (ids.has(id)) next[id] = speedMap.value[id]
  })
  speedMap.value = next
}

function restartSpeedPolling() {
  clearSpeedPolling()
  speedTimer = setInterval(() => {
    refreshCurrentPageSpeed()
  }, SPEED_REFRESH_MS)
}

function clearSpeedPolling() {
  if (speedTimer != null) {
    clearInterval(speedTimer)
    speedTimer = null
  }
}

async function refreshCurrentPageSpeed() {
  if (speedRefreshing.value) return
  const rows = instanceList.value || []
  if (rows.length === 0) {
    speedMap.value = {}
    return
  }
  speedRefreshing.value = true
  try {
    const res = await getInstanceSpeedSnapshot()
    const all = (res && res.data) ? res.data : {}
    const next = {}
    rows.forEach(row => {
      if (row.id == null) return
      const id = String(row.id)
      if (!isInstanceRunning(row)) {
        next[row.id] = { skipped: true }
      } else {
        next[row.id] = all[id] || all[row.id] || { skipped: true, message: '实时网速等待后台采集中' }
      }
    })
    speedMap.value = next
  } catch (e) {
    const next = {}
    rows.forEach(row => {
      if (row.id != null) next[row.id] = { error: true }
    })
    speedMap.value = next
  } finally {
    speedRefreshing.value = false
  }
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function handleExport() {
  proxy.download('resource/vps/instance/export', {
    ...queryParams.value
  }, `VPS实例数据_${new Date().getTime()}.xlsx`)
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.keyword = undefined
  queryParams.value.status = undefined
  queryParams.value.networkType = undefined
  queryParams.value.osType = undefined
  queryParams.value.pageNum = 1
  if (categoryTreeRef.value) categoryTreeRef.value.setCurrentKey(null)
  queryParams.value.categoryId = undefined
  getList()
}

function handleAddInstance() {
  instanceForm.value = {
    id: undefined,
    name: '',
    categoryId: queryParams.value.categoryId,
    networkType: undefined,
    osType: '',
    osVersion: '',
    bandwidth: '',
    ip: '',
    sshPort: 22,
    sshUsername: 'root',
    sshPassword: '',
    cpu: '',
    memory: '',
    disk: '',
    status: 'running',
    trafficLimit: null,
    trafficLimitGb: undefined,
    renewalAmount: '',
    expireTime: null,
    remark: ''
  }
  instanceTitle.value = '新增VPS'
  instanceOpen.value = true
  nextTick(() => proxy.resetForm('instanceRef'))
}

function buildInstanceForm(data, clone = false) {
  const source = data || {}
  const sourceName = String(source.name || source.ip || 'VPS').trim()
  return {
    id: clone ? undefined : source.id,
    name: clone ? `${sourceName} - 副本` : (source.name || ''),
    categoryId: source.categoryId ?? undefined,
    networkType: source.networkType ?? undefined,
    osType: source.osType || '',
    osVersion: source.osVersion || '',
    bandwidth: source.bandwidth || '',
    ip: source.ip || '',
    sshPort: source.sshPort ?? 22,
    sshUsername: source.sshUsername || 'root',
    sshPassword: source.sshPassword || '',
    cpu: source.cpu || '',
    memory: source.memory || '',
    disk: source.disk || '',
    status: source.status || 'running',
    trafficLimit: source.trafficLimit ?? null,
    trafficLimitGb: source.trafficLimit != null && source.trafficLimit > 0
      ? Math.round(source.trafficLimit / (1024 * 1024 * 1024) * 100) / 100
      : undefined,
    renewalAmount: source.renewalAmount || '',
    expireTime: source.expireTime || null,
    remark: source.remark || ''
  }
}

function handleEditInstance(row) {
  getInstance(row.id).then(res => {
    instanceForm.value = buildInstanceForm(res.data)
    instanceTitle.value = '编辑VPS'
    instanceOpen.value = true
  })
}

function handleCloneInstance(row) {
  getInstance(row.id).then(res => {
    instanceForm.value = buildInstanceForm(res.data, true)
    instanceTitle.value = '克隆VPS'
    instanceOpen.value = true
    nextTick(() => proxy.$refs.instanceRef?.clearValidate())
  })
}

function handleInstanceCommand(command, row) {
  if (command === 'accessLog') openVpsAccessLog(row)
  else if (command === 'clone') handleCloneInstance(row)
  else if (command === 'edit') handleEditInstance(row)
  else if (command === 'delete') handleDeleteInstance(row)
  else if (command === 'forceDelete') handleForceDeleteInstance(row)
}

function handleDeleteInstance(row) {
  proxy.$modal.confirm('是否确认删除该VPS？').then(() => {
    return delInstance(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleForceDeleteInstance(row) {
  proxy.$modal.confirm(`确认要强制删除 VPS "${displayName(row)}" 吗？此操作只删除本地 VPS、节点和流量记录，不连接服务器，也不会清理服务器上的残留配置。`).then(() => {
    return forceDelInstance(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('强制删除成功')
  }).catch(() => {})
}

function submitInstanceForm() {
  proxy.$refs.instanceRef.validate(valid => {
    if (!valid) return
    const form = instanceForm.value
    const gb = form.trafficLimitGb != null && form.trafficLimitGb !== '' ? Number(form.trafficLimitGb) : null
    const payload = {
      ...form,
      trafficLimit: gb != null && gb > 0 ? Math.round(gb * 1024 * 1024 * 1024) : null
    }
    delete payload.trafficLimitGb
    if (payload.id) {
      updateInstance(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        instanceOpen.value = false
        getList()
      })
    } else {
      addInstance(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        instanceOpen.value = false
        getList()
      })
    }
  })
}

function formatTrafficLimit(bytes) {
  if (bytes == null || bytes === 0) return '不限'
  return formatTraffic(bytes)
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
function realtimeSpeedText(row) {
  const speed = speedMap.value[row.id]
  if (!isInstanceRunning(row) || speed?.skipped) return '实时：未监控'
  if (!speed || speed.error) return '实时：-'
  return '实时：↑ ' + formatSpeedFromMb(speed.totalUpMbps) + ' / ↓ ' + formatSpeedFromMb(speed.totalDownMbps)
}
function trafficTotalText(row) {
  return '累计：' + (row.totalTrafficBytes != null ? formatTraffic(row.totalTrafficBytes) : '-')
}
function isInstanceRunning(row) {
  return row && row.status === 'running'
}
function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}
function formatExpireTime(expireTime) {
  if (!expireTime) return '-'
  return parseTime(expireTime, '{y}-{m}-{d}')
}

function goDetail(id) {
  const q = queryParams.value
  router.push({
    path: '/resource/vps-detail/index/' + id,
    query: {
      pageNum: q.pageNum,
      pageSize: q.pageSize,
      keyword: q.keyword,
      status: q.status,
      networkType: q.networkType,
      osType: q.osType,
      categoryId: q.categoryId
    }
  })
}

onMounted(() => {
  const q = route.query
  if (q && (q.pageNum != null || q.keyword != null || q.status != null || q.networkType != null || q.osType != null || q.categoryId != null)) {
    if (q.pageNum != null) queryParams.value.pageNum = Number(q.pageNum) || 1
    if (q.pageSize != null) queryParams.value.pageSize = Number(q.pageSize) || 10
    if (q.keyword !== undefined) queryParams.value.keyword = q.keyword
    if (q.status !== undefined) queryParams.value.status = q.status
    if (q.networkType !== undefined) queryParams.value.networkType = q.networkType
    if (q.osType !== undefined) queryParams.value.osType = q.osType
    if (q.categoryId != null) queryParams.value.categoryId = q.categoryId ? Number(q.categoryId) : undefined
  }
  getCategoryTree()
})

onBeforeUnmount(() => {
  clearSpeedPolling()
})
</script>

<style scoped lang="scss">
.head-container {
  margin-bottom: 12px;
  .head-label {
    margin-right: 8px;
    font-weight: 500;
  }
}
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
  .tree-op-btn {
    padding: 0 4px;
  }
}
.op-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 2px;
}
.op-btns .el-dropdown {
  display: inline-flex;
  align-items: center;
}
.op-btns .el-button {
  padding: 0 4px;
}
.expire-expired {
  color: var(--el-color-danger);
  font-weight: 500;
}
.traffic-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1.25;
  white-space: nowrap;
}
.traffic-cell-total {
  color: var(--el-text-color-primary);
}
.traffic-cell-speed {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
/* 最多 2 行后省略，悬停用 title 看全文 */
.cell-ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
  max-width: 100%;
}
.query-form {
  margin-bottom: 4px;
}
.form-tip {
  display: block;
  font-size: 12px;
  line-height: 18px;
  color: var(--el-text-color-secondary);
  margin-top: 5px;
}
:global(.vps-instance-dialog) {
  width: min(760px, calc(100vw - 32px)) !important;
  padding: 0;
  overflow: hidden;
  border-radius: 10px;
  background: var(--el-bg-color-overlay);
}
:global(.vps-instance-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 14px 20px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
:global(.vps-instance-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
:global(.vps-instance-dialog .el-dialog__headerbtn) {
  top: 3px;
  right: 4px;
  width: 46px;
  height: 46px;
}
:global(.vps-instance-dialog .el-dialog__body) {
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 16px 20px 2px;
}
:global(.vps-instance-dialog .el-dialog__footer) {
  padding: 12px 20px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.vps-instance-form {
  .el-form-item {
    margin-bottom: 16px;
  }
  :deep(.el-form-item__label) {
    height: auto;
    margin-bottom: 7px;
    padding: 0;
    font-weight: 500;
    line-height: 20px;
    color: var(--el-text-color-regular);
  }
  :deep(.el-input-number) {
    width: 100%;
  }
}
.vps-form-section-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 5px 0 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}
.vps-form-section-title:not(:first-child) {
  margin-top: 12px;
}
.vps-form-section-title::before {
  width: 3px;
  height: 15px;
  border-radius: 2px;
  background: var(--el-color-primary);
  content: '';
}
.vps-form-section-title small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 400;
}
.vps-connection-test {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 18px;
  padding: 13px 15px;
  border: 1px solid rgba(64, 158, 255, 0.24);
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.08);
}
.vps-connection-test__text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.vps-connection-test__text strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}
.vps-connection-test__text span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}
.vps-connection-test .el-button {
  flex-shrink: 0;
}
.vps-instance-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
@media (max-width: 640px) {
  :global(.vps-instance-dialog .el-dialog__body) {
    padding-right: 16px;
    padding-left: 16px;
  }
  .vps-instance-form :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
  .vps-form-section-title small {
    display: none;
  }
  .vps-connection-test {
    align-items: stretch;
    flex-direction: column;
  }
  .vps-connection-test .el-button {
    width: 100%;
  }
}
.vps-row-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 4px 0;
}
.vps-icon-box {
  position: relative;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.vps-icon-box-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
  display: block;
}
.vps-status-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--el-color-danger);
  border: 2px solid var(--el-fill-color);
  box-sizing: border-box;
}
.vps-status-badge--running {
  background: var(--el-color-success);
  border-color: var(--el-fill-color);
}
.vps-status-badge--stopped,
.vps-status-badge--error {
  background: var(--el-color-danger);
  border-color: var(--el-fill-color);
}
.vps-name-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  flex: 1;
  min-width: 0;
  text-align: left;
}
.vps-name-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}
.vps-name-link {
  display: block;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  text-align: left;
}
.vps-name-link .el-link__inner {
  text-align: left;
}
.vps-name-link.el-link {
  text-decoration: none;
}
.vps-name-link.el-link:hover {
  text-decoration: none;
}
.vps-name-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  width: 100%;
  min-height: 22px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.vps-name-ip-wrap {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  max-width: 160px;
}
.vps-name-ip {
  font-variant-numeric: tabular-nums;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.vps-name-ip-copy {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
}
.vps-name-ip-wrap:hover .vps-name-ip-copy {
  color: var(--el-color-primary);
}
.vps-name-ip-copy:hover {
  color: var(--el-color-primary);
}
.vps-name-sep {
  flex-shrink: 0;
  width: 1px;
  height: 10px;
  background: var(--el-border-color-lighter);
}
.vps-name-os {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.2;
}
.vps-name-os .vps-os-icon {
  width: 16px;
  height: 16px;
  min-width: 16px;
  min-height: 16px;
  object-fit: contain;
}

.vps-config-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.vps-config-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  line-height: 1.3;
  padding: 4px 10px;
  border-radius: 6px;
  color: var(--el-text-color-primary);
}
.vps-config-tag .el-icon {
  font-size: 14px;
}
.vps-config-tag--cpu {
  background: rgba(64, 158, 255, 0.14);
  color: var(--el-color-primary);
}
.vps-config-tag--memory {
  background: rgba(103, 194, 58, 0.14);
  color: var(--el-color-success);
}
.vps-config-tag--disk {
  background: rgba(230, 162, 60, 0.2);
  color: #b88230;
}
.vps-config-tag--bandwidth {
  background: rgba(144, 147, 153, 0.2);
  color: var(--el-text-color-regular);
}
.vps-config-tag--traffic {
  background: rgba(103, 194, 58, 0.12);
  color: var(--el-color-success);
}
.vps-config-tag--network {
  background: rgba(102, 126, 234, 0.14);
  color: #667eea;
}

.vps-os-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.vps-os-icon {
  display: block;
  width: 20px;
  height: 20px;
  min-width: 20px;
  min-height: 20px;
  flex-shrink: 0;
  object-fit: contain;
  object-position: center;
}

/* 名称列增加内边距，提升行高与舒适度 */
:deep(.el-table .el-table__cell.vps-name-column) {
  padding-top: 12px;
  padding-bottom: 12px;
}
/* 列表行 hover 高亮 */
:deep(.el-table .vps-table-row:hover > td.el-table__cell) {
  background-color: var(--el-fill-color-light);
}
/* 分割线调暗 */
:deep(.el-table td.el-table__cell) {
  border-bottom-color: var(--el-border-color-lighter);
}
:deep(.el-table th.el-table__cell) {
  border-bottom-color: var(--el-border-color-lighter);
}

/* 操作区默认置灰，hover 变亮 */
.op-btns .op-btn {
  color: var(--el-text-color-secondary);
}
.op-btns .op-btn:hover {
  color: var(--el-color-primary);
}
.op-btns .el-dropdown .op-btn {
  color: var(--el-text-color-secondary);
}
.op-btns:hover .op-btn,
.op-btns .op-btn:hover {
  color: var(--el-color-primary);
}
</style>
