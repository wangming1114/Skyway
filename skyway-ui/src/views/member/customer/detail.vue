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
          <ProxyNodePanel
            v-if="customer.id"
            :customer-id="customer.id"
            :fixed-customer="true"
            :hide-customer-column="true"
            :show-instance-column="true"
            :use-http-exec="true"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="MemberCustomerDetail">
import { getCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'
import ProxyNodePanel from '@/views/resource/vps/components/ProxyNodePanel.vue'

const route = useRoute()
const router = useRouter()

const customerId = computed(() => Number(route.params.customerId))
const customer = ref({})
const infoLoading = ref(true)
const activeTab = ref('bindings')
const listQuery = ref({})

function goBack() {
  router.push({ path: '/member/customer', query: listQuery.value })
}

onMounted(() => {
  listQuery.value = {
    pageNum: route.query.pageNum,
    pageSize: route.query.pageSize,
    keyword: route.query.keyword,
    status: route.query.status
  }
  getCustomer(customerId.value).then(res => {
    customer.value = res.data || {}
    infoLoading.value = false
  }).catch(() => {
    infoLoading.value = false
  })
})
</script>
