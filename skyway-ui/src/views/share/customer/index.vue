<template>
  <div class="customer-share-page">
    <main class="customer-share-main">
      <section v-if="!unlocked" class="unlock-panel">
        <h1>订阅信息</h1>
        <el-form ref="unlockFormRef" :model="unlockForm" :rules="unlockRules" @submit.prevent>
          <el-form-item prop="accessPassword">
            <el-input
              v-model="unlockForm.accessPassword"
              type="password"
              show-password
              size="large"
              placeholder="请输入访问密码"
              @keyup.enter="submitUnlock"
            />
          </el-form-item>
          <el-button type="primary" size="large" :loading="unlocking" class="unlock-button" @click="submitUnlock">查看订阅</el-button>
        </el-form>
      </section>

      <section v-else class="node-section">
        <div class="node-section-head">
          <div>
            <h1>订阅信息</h1>
            <p>当前链接仅可查看节点订阅，不提供管理操作。</p>
          </div>
          <el-button icon="Refresh" :loading="unlocking" @click="submitUnlock">刷新</el-button>
        </div>

        <el-table :data="nodeList" border size="small" empty-text="暂无可用节点">
          <el-table-column label="节点信息" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="node-info-cell">
                <span class="node-name">{{ row.nodeName || '-' }}</span>
                <el-tag size="small" :type="getNodeTypeTagColor(row.nodeType)">{{ row.nodeType || '-' }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="地址端口" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.address || '-' }}<span v-if="row.port">:{{ row.port }}</span>
            </template>
          </el-table-column>
          <el-table-column label="有效期" width="150" align="center">
            <template #default="{ row }">
              <span v-if="!row.expireTime" class="expire-forever">永久</span>
              <span v-else :class="{ 'expire-expired': isExpired(row.expireTime) }">{{ parseTime(row.expireTime, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="!row.url" @click="handleShare(row)">订阅信息</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>

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
            <el-button type="primary" size="small" @click="copyToClipboard(shareVlessUrl)">复制 VLESS</el-button>
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
              <el-button type="primary" size="small" @click="copyToClipboard(shareClashUrl)">复制 Clash 订阅</el-button>
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
  </div>
</template>

<script setup>
import QRCode from 'qrcode'
import { unlockCustomerTempShare } from '@/api/member/customerTempShare'
import { buildClashSubscribeUrl, parseVlessUrl, safeProxyShareFilename } from '@/utils/proxyShare'
import { parseTime } from '@/utils/skyway'

const { proxy } = getCurrentInstance()
const route = useRoute()

const token = computed(() => String(route.params.token || ''))
const unlocked = ref(false)
const unlocking = ref(false)
const unlockFormRef = ref(null)
const unlockForm = reactive({ accessPassword: '' })
const unlockRules = {
  accessPassword: [{ required: true, message: '请输入访问密码', trigger: 'blur' }]
}
const nodeList = ref([])

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

function submitUnlock() {
  unlockFormRef.value?.validate(valid => {
    if (!valid) return
    unlocking.value = true
    unlockCustomerTempShare(token.value, unlockForm.accessPassword).then(res => {
      nodeList.value = res.data || []
      unlocked.value = true
    }).catch(() => {}).finally(() => {
      unlocking.value = false
    })
  })
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
</script>

<style scoped lang="scss">
.customer-share-page {
  min-height: 100vh;
  background: #f6f8fb;
  color: #1f2937;
}
.customer-share-main {
  width: min(1120px, calc(100vw - 32px));
  margin: 0 auto;
  padding: 40px 0;
}
.unlock-panel {
  max-width: 420px;
  margin: 12vh auto 0;
  padding: 28px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}
.unlock-panel h1,
.node-section h1 {
  margin: 0 0 18px;
  font-size: 24px;
  font-weight: 650;
}
.unlock-button {
  width: 100%;
}
.node-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
}
.node-section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.node-section-head p {
  margin: 0;
  color: #6b7280;
}
.node-info-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.node-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.expire-forever {
  color: var(--el-color-success);
  font-weight: 500;
}
.expire-expired {
  color: var(--el-color-danger);
  font-weight: 500;
}
.proxy-share-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.proxy-share-header,
.proxy-share-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.proxy-share-title {
  font-size: 18px;
  font-weight: 650;
}
.proxy-share-subtitle,
.proxy-share-section-desc {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}
.proxy-share-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
}
.proxy-share-section-title {
  font-weight: 650;
}
.proxy-share-grid {
  display: grid;
  grid-template-columns: 1fr 240px;
  gap: 16px;
  align-items: center;
}
.proxy-share-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}
.proxy-share-qr {
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: #fff;
}
.proxy-share-qr img {
  width: 220px;
  height: 220px;
}
.proxy-share-qr-loading {
  color: #6b7280;
}

@media (max-width: 720px) {
  .customer-share-main {
    width: min(100vw - 20px, 1120px);
    padding: 20px 0;
  }
  .node-section-head,
  .proxy-share-section-head {
    flex-direction: column;
  }
  .proxy-share-grid {
    grid-template-columns: 1fr;
  }
}
</style>
