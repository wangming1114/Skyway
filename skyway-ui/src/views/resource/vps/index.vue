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
            <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
              <el-form-item label="关键字" prop="keyword">
                <el-input v-model="queryParams.keyword" placeholder="名称/ID/IP" clearable style="width: 200px" @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
                  <el-option v-for="dict in res_instance_status" :key="dict.value" :label="dict.label" :value="dict.value" />
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
              <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
            </el-row>

            <el-table v-loading="loading" :data="instanceList">
              <el-table-column label="编号" align="center" prop="id" width="72" />
              <el-table-column label="名称" align="center" prop="name" min-width="120">
                <template #default="scope">
                  <el-link type="primary" @click="goDetail(scope.row.id)" class="cell-ellipsis-2" :title="scope.row.name">{{ scope.row.name }}</el-link>
                </template>
              </el-table-column>
              <el-table-column label="分类" align="center" prop="categoryName" width="90" show-overflow-tooltip />
              <el-table-column label="节点数" align="center" prop="nodeCount" width="72" />
              <el-table-column label="IP" align="center" prop="ip" min-width="136" show-overflow-tooltip />
              <el-table-column label="状态" align="center" prop="status" width="88">
                <template #default="scope">
                  <dict-tag :options="res_instance_status" :value="scope.row.status" />
                </template>
              </el-table-column>
              <el-table-column label="网络类型" align="center" prop="networkType" min-width="120" show-overflow-tooltip>
                <template #default="scope">
                  <dict-tag :options="res_instance_network_type" :value="scope.row.networkType" />
                </template>
              </el-table-column>
              <el-table-column label="累计流量" align="center" min-width="100" show-overflow-tooltip>
                <template #default="scope">
                  {{ scope.row.totalTrafficBytes != null ? formatTraffic(scope.row.totalTrafficBytes) : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="备注" align="center" prop="remark" min-width="100">
                <template #default="scope">
                  <span class="cell-ellipsis-2" :title="scope.row.remark">{{ scope.row.remark || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width" fixed="right">
                <template #default="scope">
                  <div class="op-btns">
                    <el-button link type="primary" icon="Connection" @click="handleConnectServer(scope.row)" v-hasPermi="['resource:vps:list']">连接</el-button>
                    <el-button link type="primary" icon="View" @click="goDetail(scope.row.id)" v-hasPermi="['resource:vps:query']">详情</el-button>
                    <el-dropdown trigger="click" @command="(cmd) => handleInstanceCommand(cmd, scope.row)" v-hasPermi="['resource:vps:edit', 'resource:vps:remove']">
                      <el-button link type="primary" icon="DArrowRight" class="op-dropdown-trigger">更多</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="edit" icon="Edit" v-hasPermi="['resource:vps:edit']">编辑</el-dropdown-item>
                          <el-dropdown-item command="delete" icon="Delete" v-hasPermi="['resource:vps:remove']">删除</el-dropdown-item>
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

    <!-- VPS 实例 新增/编辑 -->
    <el-dialog :title="instanceTitle" v-model="instanceOpen" width="600px" append-to-body>
      <el-form ref="instanceRef" :model="instanceForm" :rules="instanceRules" label-width="96px">
        <el-divider content-position="left">基本信息</el-divider>
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
        </el-row>
        <el-divider content-position="left">连接信息</el-divider>
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
        <el-form-item label=" " :span="2">
          <el-button type="primary" plain :loading="testConnectionLoading" @click="handleTestConnection">
            连接测试（自动回写 CPU/内存/磁盘）
          </el-button>
        </el-form-item>
        <el-divider content-position="left">规格与备注</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="CPU" prop="cpu">
              <el-input v-model="instanceForm.cpu" placeholder="如 2核，可点连接测试自动填充" maxlength="50" />
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
        <el-button type="primary" @click="submitInstanceForm">确 定</el-button>
        <el-button @click="instanceOpen = false">取 消</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="Vps">
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
  addInstance,
  updateInstance,
  delInstance,
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
const instanceOpen = ref(false)
const instanceTitle = ref('')
const instanceForm = ref({
  name: '',
  categoryId: undefined,
  networkType: undefined,
  ip: '',
  sshPort: 22,
  sshUsername: '',
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
    query: { name: row.name || '' }
  })
}

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  status: undefined,
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
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.keyword = undefined
  queryParams.value.status = undefined
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
    ip: '',
    sshPort: 22,
    sshUsername: '',
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

function handleEditInstance(row) {
  getInstance(row.id).then(res => {
    const d = res.data
    instanceForm.value = {
      ...d,
      networkType: d.networkType ?? undefined,
      trafficLimitGb: d.trafficLimit != null && d.trafficLimit > 0
        ? Math.round(d.trafficLimit / (1024 * 1024 * 1024) * 100) / 100
        : undefined
    }
    instanceTitle.value = '编辑VPS'
    instanceOpen.value = true
  })
}

function handleInstanceCommand(command, row) {
  if (command === 'edit') handleEditInstance(row)
  else if (command === 'delete') handleDeleteInstance(row)
}

function handleDeleteInstance(row) {
  proxy.$modal.confirm('是否确认删除该VPS？').then(() => {
    return delInstance(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
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
function isExpired(expireTime) {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
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
      categoryId: q.categoryId
    }
  })
}

onMounted(() => {
  const q = route.query
  if (q && (q.pageNum != null || q.keyword != null || q.status != null || q.categoryId != null)) {
    if (q.pageNum != null) queryParams.value.pageNum = Number(q.pageNum) || 1
    if (q.pageSize != null) queryParams.value.pageSize = Number(q.pageSize) || 10
    if (q.keyword !== undefined) queryParams.value.keyword = q.keyword
    if (q.status !== undefined) queryParams.value.status = q.status
    if (q.categoryId != null) queryParams.value.categoryId = q.categoryId ? Number(q.categoryId) : undefined
  }
  getCategoryTree()
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
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: 8px;
}
</style>
