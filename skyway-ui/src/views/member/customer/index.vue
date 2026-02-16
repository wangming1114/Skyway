<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键字" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="用户名/手机号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="0" />
          <el-option label="禁用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['member:customer:add']">新增客户</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['member:customer:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="customerList">
      <el-table-column label="客户ID" align="center" prop="id" width="80" />
      <el-table-column label="用户名" align="center" prop="username" min-width="100" :show-overflow-tooltip="true" />
      <el-table-column label="邮箱" align="center" min-width="160" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.email || '-' }}</template>
      </el-table-column>
      <el-table-column label="手机号" align="center" min-width="120" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">{{ scope.row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联节点" align="center" width="90">
        <template #default="scope">
          <el-link type="primary" @click="goDetail(scope.row)" :underline="false">{{ scope.row.nodeBindCount != null ? scope.row.nodeBindCount : 0 }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <div class="op-btns">
            <el-button link type="primary" icon="View" @click="goDetail(scope.row)" v-hasPermi="['member:customer:query']">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['member:customer:edit']">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, scope.row)" v-hasPermi="['member:customer:edit', 'member:customer:resetPwd', 'member:customer:remove']">
              <el-button link type="primary" icon="DArrowRight" class="op-dropdown-trigger">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="resetPwd" icon="Key" v-if="checkPermi(['member:customer:resetPwd'])">重置密码</el-dropdown-item>
                  <el-dropdown-item command="toggleStatus" :icon="scope.row.status === '0' ? 'SwitchButton' : 'CircleCheck'" v-if="checkPermi(['member:customer:edit'])">
                    {{ scope.row.status === '0' ? '禁用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" icon="Delete" v-if="checkPermi(['member:customer:remove'])">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!form.id">
          <el-input v-model="form.username" placeholder="请输入用户名" maxlength="64" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" maxlength="20" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="128" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="微信号" prop="wechat">
          <el-input v-model="form.wechat" placeholder="请输入微信号" maxlength="64" />
        </el-form-item>
        <el-form-item label="QQ号" prop="qq">
          <el-input v-model="form.qq" placeholder="请输入QQ号" maxlength="20" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="form.avatar" placeholder="头像URL（可选）" maxlength="255" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="备注" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MemberCustomer">
import { listCustomer, getCustomer, addCustomer, updateCustomer, delCustomer, resetCustomerPwd, changeCustomerStatus } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'
import { checkPermi } from '@/utils/permission'

const { proxy } = getCurrentInstance()
const router = useRouter()

const customerList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: undefined,
    status: undefined
  },
  rules: {
    username: [
      { required: true, message: '用户名不能为空', trigger: 'blur' },
      { min: 2, max: 64, message: '长度 2-64 个字符', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '密码不能为空', trigger: 'blur' },
      { min: 5, max: 20, message: '长度 5-20 个字符', trigger: 'blur' }
    ],
    email: [
      { required: true, message: '邮箱不能为空', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
    ],
    phone: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    qq: [
      { pattern: /^\d{5,12}$/, message: 'QQ号为5-12位数字', trigger: 'blur' }
    ],
    avatar: [
      { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' }
    ]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCustomer(queryParams.value).then(res => {
    loading.value = false
    customerList.value = res.rows
    total.value = res.total
  }).catch(() => { loading.value = false })
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
  getList()
}

function handleExport() {
  proxy.download('member/customer/export', {
    ...queryParams.value
  }, `客户数据_${new Date().getTime()}.xlsx`)
}

function goDetail(row) {
  const q = { ...queryParams.value }
  router.push({ path: '/member/customer-detail/index/' + row.id, query: { pageNum: q.pageNum, pageSize: q.pageSize, keyword: q.keyword, status: q.status } })
}

function handleAdd() {
  resetForm()
  title.value = '新增客户'
  form.value = { username: '', password: '', email: '', phone: '', wechat: '', qq: '', avatar: '', status: '0', remark: '' }
  open.value = true
}

function handleUpdate(row) {
  resetForm()
  getCustomer(row.id).then(res => {
    form.value = { ...res.data }
    title.value = '编辑客户'
    open.value = true
  })
}

function resetForm() {
  form.value = {}
  proxy.resetForm('formRef')
}

function cancel() {
  open.value = false
  resetForm()
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    if (form.value.id) {
      updateCustomer(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addCustomer(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleCommand(command, row) {
  switch (command) {
    case 'resetPwd':
      handleResetPwd(row)
      break
    case 'toggleStatus':
      handleStatusChange(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

function handleResetPwd(row) {
  proxy.$prompt('请输入新密码', '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{5,20}$/,
    inputErrorMessage: '密码长度 5-20'
  }).then(({ value }) => {
    return resetCustomerPwd(row.id, value)
  }).then(() => {
    proxy.$modal.msgSuccess('重置成功，新密码已生效')
  }).catch(() => {})
}

function handleStatusChange(row) {
  const newStatus = row.status === '0' ? '1' : '0'
  const text = newStatus === '1' ? '禁用' : '启用'
  proxy.$modal.confirm('确认要' + text + '客户"' + row.username + '"吗？').then(() => {
    return changeCustomerStatus(row.id, newStatus)
  }).then(() => {
    proxy.$modal.msgSuccess(text + '成功')
    row.status = newStatus
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除客户"' + row.username + '"？').then(() => {
    return delCustomer(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
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
</style>
