<template>
  <div class="customer-share-root">
    <section v-if="!unlocked" class="share-unlock-page">
      <img class="unlock-art" :src="heroOverview" alt="" />
      <div class="unlock-panel">
        <div class="unlock-logo" aria-hidden="true"></div>
        <h1>订阅信息访问</h1>
        <p>请输入管理员提供的访问密码，查看节点订阅、客户端教程与下载信息。</p>
        <el-form ref="unlockFormRef" :model="unlockForm" :rules="unlockRules" @submit.prevent>
          <el-form-item prop="accessPassword">
            <el-input
              v-model="unlockForm.accessPassword"
              type="password"
              show-password
              size="large"
              placeholder="请输入访问密码"
              @keyup.enter="unlockAndShow"
            />
          </el-form-item>
          <button type="button" class="share-primary unlock-submit" :disabled="loading" @click="unlockAndShow">
            {{ loading ? '正在验证...' : '查看订阅' }}
          </button>
        </el-form>
      </div>
    </section>

    <main v-else class="share-page-canvas">
      <OverviewScreen
        v-if="activeView === 'overview'"
        v-model:keyword="keyword"
        v-model:status-filter="statusFilter"
        :nodes="filteredNodes"
        :summary="summary"
        :loading="loading"
        :show-detail-guide="showDetailGuide"
        @refresh="refreshNodes"
        @navigate="showView"
        @detail="openNodeDetail"
        @copy-subscription="copyNodeClash"
        @close-detail-guide="closeDetailGuide"
      />
      <DesktopGuideScreen
        v-else-if="activeView === 'desktop'"
        @navigate="showView"
        @copy-vless="copyPrimaryVless"
        @copy-clash="copyPrimaryClash"
        @open-guide="openGuide"
      />
      <NodeDetailScreen
        v-else-if="activeView === 'nodeDetail'"
        :node="selectedNode"
        :qr-url="detailQrUrl"
        @navigate="showView"
        @copy-vless="copyNodeVless"
        @copy-clash="copyNodeClash"
        @download-qr="downloadQrCode"
        @open-guide="openGuide"
      />
      <MobileGuideScreen
        v-else-if="activeView === 'mobile'"
        @navigate="showView"
      />
      <DownloadCenterScreen
        v-else-if="activeView === 'download'"
        @navigate="showView"
        @download-client="downloadClient"
      />
      <GuideDirectoryScreen
        v-else
        @navigate="showView"
        @copy-text="copyToClipboard"
        @open-url="openUrl"
      />
    </main>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { unlockCustomerTempShare } from '@/api/member/customerTempShare'
import { buildShareNodeSummary, filterShareNodes, normalizeShareNode, safeProxyShareFilename } from '@/utils/proxyShare'
import heroOverview from '@/assets/share/customer/hero-overview.png'
import OverviewScreen from './screens/OverviewScreen.vue'
import DesktopGuideScreen from './screens/DesktopGuideScreen.vue'
import NodeDetailScreen from './screens/NodeDetailScreen.vue'
import MobileGuideScreen from './screens/MobileGuideScreen.vue'
import DownloadCenterScreen from './screens/DownloadCenterScreen.vue'
import GuideDirectoryScreen from './screens/GuideDirectoryScreen.vue'
import { clientLinks, guideCards } from './data'
import './style.scss'

const { proxy } = getCurrentInstance()
const route = useRoute()

const token = computed(() => String(route.params.token || ''))
const unlocked = ref(false)
const loading = ref(false)
const activeView = ref('overview')
const keyword = ref('')
const statusFilter = ref('all')
const nodeList = ref([])
const selectedNode = ref(null)
const detailQrUrl = ref('')
const showDetailGuide = ref(false)
const detailGuideStorageKey = 'skyway.customerShare.detailGuideSeen'

const unlockFormRef = ref(null)
const unlockForm = reactive({ accessPassword: '' })
const unlockRules = {
  accessPassword: [{ required: true, message: '请输入访问密码', trigger: 'blur' }]
}

const normalizedNodes = computed(() => {
  return nodeList.value.map((node, index) => {
    const normalized = normalizeShareNode(node)
    return {
      ...normalized,
      isPrimary: index === 0,
      remainingLabel: normalized.remainingDays === null ? '' : `剩余 ${Math.max(normalized.remainingDays, 0)} 天`
    }
  })
})

const filteredNodes = computed(() => {
  return filterShareNodes(normalizedNodes.value, {
    keyword: keyword.value,
    status: statusFilter.value
  })
})

const summary = computed(() => buildShareNodeSummary(normalizedNodes.value))
const primaryNode = computed(() => normalizedNodes.value.find(node => node.vlessUrl) || normalizedNodes.value[0] || null)

function unlockAndShow() {
  unlockFormRef.value?.validate(valid => {
    if (!valid) return
    loadNodes({ nextView: 'overview' })
  })
}

function refreshNodes() {
  loadNodes({ nextView: activeView.value })
}

function loadNodes({ nextView }) {
  if (!unlockForm.accessPassword) {
    proxy.$modal.msgWarning('请输入访问密码')
    unlocked.value = false
    return
  }
  loading.value = true
  unlockCustomerTempShare(token.value, unlockForm.accessPassword)
    .then(res => {
      const nodes = res.data || []
      const targetView = nextView || 'overview'
      nodeList.value = nodes
      selectedNode.value = null
      unlocked.value = true
      activeView.value = targetView
      showDetailGuide.value = targetView === 'overview' && shouldShowDetailGuide(nodes)
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

function showView(view) {
  activeView.value = view
  showDetailGuide.value = view === 'overview' && shouldShowDetailGuide(normalizedNodes.value)
  nextTick(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function openNodeDetail(node) {
  closeDetailGuide()
  selectedNode.value = node
  activeView.value = 'nodeDetail'
  nextTick(() => {
    refreshDetailQrCode()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function shouldShowDetailGuide(nodes) {
  return Array.isArray(nodes) && nodes.length > 0 && !hasSeenDetailGuide()
}

function hasSeenDetailGuide() {
  try {
    return localStorage.getItem(detailGuideStorageKey) === '1'
  } catch (e) {
    return false
  }
}

function closeDetailGuide() {
  showDetailGuide.value = false
  try {
    localStorage.setItem(detailGuideStorageKey, '1')
  } catch (e) {}
}

function copyPrimaryVless() {
  copyNodeVless(primaryNode.value)
}

function copyPrimaryClash() {
  copyNodeClash(primaryNode.value)
}

function copyNodeVless(node) {
  if (!node?.vlessUrl) {
    proxy.$modal.msgWarning('暂无 VLESS 链接')
    return
  }
  copyToClipboard(node.vlessUrl)
}

function copyNodeClash(node) {
  if (!node?.clashUrl) {
    proxy.$modal.msgWarning('暂无 Clash 订阅地址')
    return
  }
  copyToClipboard(node.clashUrl)
}

function refreshDetailQrCode() {
  detailQrUrl.value = ''
  if (!selectedNode.value?.vlessUrl) return
  QRCode.toDataURL(selectedNode.value.vlessUrl, { width: 220, margin: 1, errorCorrectionLevel: 'M' }).then(url => {
    detailQrUrl.value = url
  }).catch(() => {
    proxy.$modal.msgWarning('二维码生成失败，请直接复制链接')
  })
}

function downloadQrCode() {
  const qrUrl = detailQrUrl.value
  const node = selectedNode.value
  if (!qrUrl) {
    proxy.$modal.msgWarning('二维码尚未生成')
    return
  }
  const a = document.createElement('a')
  a.href = qrUrl
  a.download = safeProxyShareFilename((node?.name || 'proxy-share') + '-qrcode', 'png')
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

function openGuide(type) {
  const map = {
    v2rayN: 'https://doc.theojs.cn/serve/antiwall/v2rayn',
    clash: 'https://doc.theojs.cn/serve/antiwall/clash-verge-rev',
    android: 'https://v2rayng.wiki/'
  }
  openUrl(map[type] || guideCards[0].url)
}

function downloadClient(client) {
  const keyMap = {
    v2rayN: 'v2rayN',
    clash: 'clash',
    v2rayNG: 'v2rayNG',
    shadowrocket: 'shadowrocket'
  }
  openUrl(clientLinks[keyMap[client] || client])
}

function openUrl(url) {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

function copyToClipboard(text) {
  if (!text) {
    proxy.$modal.msgWarning('暂无可复制内容')
    return
  }
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess('已复制到剪贴板')
  }).catch(() => {
    const textarea = document.createElement('textarea')
    textarea.value = text
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    proxy.$modal.msgSuccess('已复制到剪贴板')
  })
}

</script>
