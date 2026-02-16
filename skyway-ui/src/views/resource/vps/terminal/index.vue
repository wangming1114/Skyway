<template>
  <div class="vps-terminal-page">
    <div class="page-header">
      <el-button link icon="Back" @click="goBack">返回</el-button>
      <span class="page-title">连接服务器 - {{ instanceName || 'VPS' }}</span>
    </div>
    <Splitpanes class="ssh-split">
      <Pane :size="15" :min-size="10" :max-size="25" class="pane-left">
        <div class="pane-inner scroll-thin">
          <ServerMonitorPanel
            :send-sysinfo="sendSysinfo"
            :sysinfo-data="sysinfoData"
            :visible="!!instanceId"
          />
        </div>
      </Pane>
      <Pane :size="85" :min-size="60" class="pane-right">
        <Splitpanes horizontal class="ssh-split-inner">
          <Pane :size="55" :min-size="20" class="pane-terminal">
            <div class="pane-inner scroll-thin terminal-wrap">
              <TerminalPanel
                ref="terminalRef"
                :instance-id="instanceId"
                :visible="!!instanceId"
                @sysinfo="onSysinfo"
                @sftp="onSftp"
                @connected-change="onWsConnectedChange"
                @exec-output="onExecOutput"
                @exec-end="onExecEnd"
              />
            </div>
          </Pane>
          <Pane :size="45" :min-size="20" class="pane-files">
            <div class="pane-inner scroll-thin file-wrap">
              <el-tabs v-model="bottomActiveTab" class="bottom-tabs" type="border-card">
                <el-tab-pane name="file">
                  <template #label>
                    <span class="tab-label"><el-icon><Folder /></el-icon> 文件</span>
                  </template>
                  <SftpFilePanel
                    :send-json="sendJson"
                    :sftp-message="sftpMessage"
                    :connected="wsConnected"
                  />
                </el-tab-pane>
                <el-tab-pane name="command">
                  <template #label>
                    <span class="tab-label"><el-icon><Monitor /></el-icon> 命令</span>
                  </template>
                  <div class="command-panel">
                    <div class="command-cards">
                      <div class="command-card" @click="wsConnected && openInstallDrawer()">
                        <span class="command-card-icon">📦</span>
                        <span class="command-card-title">sing-box</span>
                        <span class="command-card-desc">233boy 官方一键安装</span>
                        <el-button
                          type="primary"
                          size="small"
                          :disabled="!wsConnected"
                          @click.stop="openInstallDrawer"
                          class="command-card-btn"
                        >
                          安装
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </Pane>
        </Splitpanes>
      </Pane>
    </Splitpanes>

    <!-- 一键安装 sing-box 实时日志 -->
    <el-drawer
      v-model="installDrawerVisible"
      title="安装 sing-box - 实时日志"
      direction="rtl"
      size="520"
      :close-on-click-modal="!installRunning"
      @closed="onInstallDrawerClosed"
    >
      <div class="install-drawer-body">
        <div v-if="installRunning" class="install-status">安装中…</div>
        <div v-else-if="installExitCode != null" :class="['install-status', installExitCode === 0 ? 'success' : 'fail']">
          {{ installExitCode === 0 ? '安装完成' : '安装失败 (exit ' + installExitCode + ')，请查看下方日志' }}
        </div>
        <pre ref="installLogRef" class="install-log">{{ installLog }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="VpsTerminal">
import { ref, computed, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import { Folder, Monitor } from '@element-plus/icons-vue'
import TerminalPanel from '../components/TerminalPanel.vue'
import ServerMonitorPanel from '../components/ServerMonitorPanel.vue'
import SftpFilePanel from '../components/SftpFilePanel.vue'

const INSTALL_CMD = 'bash <(wget -qO- -o- https://github.com/233boy/sing-box/raw/main/install.sh)'

const route = useRoute()
const router = useRouter()

const instanceId = computed(() => {
  const id = route.params.id
  if (id == null || id === '') return null
  const n = Number(id)
  return Number.isInteger(n) && n > 0 ? n : null
})
const instanceName = computed(() => (route.query.name != null ? String(route.query.name) : ''))

const terminalRef = ref(null)
const sysinfoData = ref(null)
const sftpMessage = ref(null)
const wsConnected = ref(false)

const bottomActiveTab = ref('file')
const installDrawerVisible = ref(false)
const installLog = ref('')
const installLogRef = ref(null)
const installRunning = ref(false)
const installExitCode = ref(null)
let installReqId = 1

function onWsConnectedChange(connected) {
  wsConnected.value = !!connected
  if (connected && route.query.install === 'singbox') {
    bottomActiveTab.value = 'command'
    nextTick(() => {
      openInstallDrawer()
      router.replace({ ...route, query: { ...route.query, install: undefined } })
    })
  }
}

function onSysinfo(data) {
  sysinfoData.value = data
}

function onSftp(msg) {
  sftpMessage.value = msg
}

function sendSysinfo() {
  terminalRef.value?.sendSysinfo?.()
}

function sendJson(obj) {
  terminalRef.value?.sendJson?.(obj)
}

function openInstallDrawer() {
  if (!wsConnected.value) {
    ElMessage.warning('请先连接 SSH')
    return
  }
  ElMessageBox.confirm('将在当前服务器上执行 233boy 官方 sing-box 安装脚本，是否继续？', '确认执行', {
    confirmButtonText: '执行',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    installLog.value = ''
    installRunning.value = true
    installExitCode.value = null
    installDrawerVisible.value = true
    nextTick(() => {
      sendJson({
        type: 'exec',
        command: INSTALL_CMD,
        reqId: installReqId
      })
    })
  }).catch(() => {})
}

function onExecOutput(msg) {
  if (msg.reqId !== installReqId) return
  const data = msg.data != null ? String(msg.data) : ''
  installLog.value += data
  nextTick(() => {
    const el = installLogRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function isInstallSuccessByLog(log) {
  if (!log || typeof log !== 'string') return false
  const s = log.toLowerCase()
  return /installed|安装成功|success|完成|started|running|\[✓\]|\[ok\]/.test(s) && /sing-box|singbox/.test(s)
}

function onExecEnd(msg) {
  if (msg.reqId !== installReqId) return
  installRunning.value = false
  const code = msg.code != null ? msg.code : (msg.type === 'exec_error' ? -1 : null)
  const success = code === 0 || (code === 1 && isInstallSuccessByLog(installLog.value))
  installExitCode.value = success ? 0 : code
  if (success) {
    ElMessage.success('sing-box 安装完成')
  } else if (code != null && code !== 0) {
    ElMessage.error('安装失败，请查看日志')
  }
  installReqId += 1
}

function onInstallDrawerClosed() {
  installExitCode.value = null
}

function goBack() {
  router.push({ path: '/resource/vps' })
}

onBeforeUnmount(() => {
  sysinfoData.value = null
  sftpMessage.value = null
  wsConnected.value = false
})
</script>

<style scoped lang="scss">
.vps-terminal-page {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--el-bg-color-page);
}
.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
  .page-title {
    font-weight: 600;
    font-size: 15px;
    flex: 1;
  }
}
.install-drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 12px 12px;
}
.install-status {
  flex-shrink: 0;
  padding: 8px 0;
  color: var(--el-text-color-regular);
}
.install-status.success {
  color: var(--el-color-success);
}
.install-status.fail {
  color: var(--el-color-danger);
}
.install-log {
  flex: 1;
  margin: 0;
  padding: 12px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
.ssh-split {
  flex: 1;
  min-height: 0;
  :deep(.splitpanes__pane) {
    overflow: hidden;
  }
  :deep(.splitpanes__splitter) {
    background: var(--el-border-color-lighter);
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      right: 0;
      bottom: 0;
      z-index: 1;
    }
  }
}
.ssh-split-inner {
  height: 100%;
  :deep(.splitpanes__splitter) {
    background: var(--el-border-color-lighter);
  }
}
.pane-left {
  background: var(--el-fill-color-light);
  max-width: 280px;
}
.pane-right {
  background: var(--el-bg-color);
}
.pane-inner {
  height: 100%;
  overflow: auto;
  padding: 12px;
  box-sizing: border-box;
}
.scroll-thin {
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color) transparent;
}
.terminal-wrap {
  padding: 4px;
  background: #1a1a1a;
}
.file-wrap {
  padding: 4px;
  overflow: hidden;
}
.bottom-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: none;
  :deep(.el-tabs__header) {
    margin: 0;
    background: var(--el-fill-color-light);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  :deep(.el-tabs__nav-wrap) {
    padding: 0 8px;
  }
  :deep(.el-tabs__item) {
    padding: 0 12px;
    height: 32px;
    line-height: 32px;
    font-size: 13px;
  }
  :deep(.el-tabs__item .el-icon) {
    font-size: 14px;
  }
  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    padding: 0;
  }
  :deep(.el-tab-pane) {
    height: 100%;
    overflow: hidden;
  }
  :deep(.el-tabs__item.is-active) {
    color: var(--el-color-primary);
    font-weight: 500;
  }
  :deep(.el-tabs__indicator) {
    background-color: var(--el-color-primary);
  }
  :deep(.el-tabs__nav) {
    border: none;
  }
  :deep(.el-tabs__active-bar) {
    height: 2px;
  }
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.command-panel {
  padding: 10px 12px;
  height: 100%;
  overflow: auto;
}
.command-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.command-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  &:hover {
    border-color: var(--el-border-color);
    background: var(--el-fill-color-light);
  }
  &:hover .command-card-btn:not(:disabled) {
    opacity: 1;
  }
}
.command-card-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.command-card-title {
  font-weight: 600;
  font-size: 13px;
  color: var(--el-text-color-primary);
  min-width: 72px;
}
.command-card-desc {
  flex: 1;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  min-width: 0;
}
.command-card-btn {
  flex-shrink: 0;
  opacity: 0.9;
}
</style>
