<template>
  <div class="app-container">
    <el-card v-loading="infoLoading" class="box-card">
      <template #header>
        <div class="detail-card-header">
          <span>客户详情</span>
          <span class="detail-card-actions">
          <el-button v-if="customer.id" type="primary" link icon="Link" @click="tempShareVisible = true">订阅信息访问</el-button>
          <el-button type="primary" link icon="Back" @click="goBack">返回</el-button>
          </span>
        </div>
      </template>

      <el-descriptions :column="isMobile ? 1 : 2" border v-if="customer.id" class="customer-descriptions">
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
            :link-node-name-to-instance="true"
            :use-http-exec="true"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <CustomerTempShareDialog
      v-if="customer.id"
      v-model="tempShareVisible"
      :customer-id="customer.id"
    />
  </div>
</template>

<script setup name="MemberCustomerDetail">
import { getCustomer } from '@/api/member/customer'
import { parseTime } from '@/utils/skyway'
import ProxyNodePanel from '@/views/resource/vps/components/ProxyNodePanel.vue'
import CustomerTempShareDialog from './components/CustomerTempShareDialog.vue'
import useAppStore from '@/store/modules/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const isMobile = computed(() => appStore.device === 'mobile')

const customerId = computed(() => Number(route.params.customerId))
const customer = ref({})
const infoLoading = ref(true)
const activeTab = ref('bindings')
const listQuery = ref({})
const tempShareVisible = ref(false)

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

<style scoped>
.detail-card-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.detail-card-actions { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; }
@media (max-width: 992px) {
  .box-card :deep(.el-card__header) { padding: 12px; }
  .detail-card-header { align-items: flex-start; flex-direction: column; }
  .detail-card-actions { justify-content: flex-start; }
  .customer-descriptions :deep(.el-descriptions__body),
  .customer-descriptions :deep(.el-descriptions__table) { width: 100%; }
  .customer-descriptions :deep(.el-descriptions__cell) { padding: 8px 10px !important; }
  .customer-descriptions :deep(.el-descriptions__label) { width: 92px; }
}
</style>
