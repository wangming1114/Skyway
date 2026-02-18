<template>
  <div class="vps-terminal-page">
    <div class="page-header">
      <el-button link icon="Back" @click="goBack">返回</el-button>
      <span class="page-title">{{ instanceName || 'VPS' }}{{ instanceIp ? ' - ' + instanceIp : '' }}</span>
    </div>
    <Splitpanes class="ssh-split">
      <Pane :size="15" :min-size="10" :max-size="25" class="pane-left">
        <div class="pane-inner scroll-thin">
          <ServerMonitorPanel
            :ip="instanceIp"
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
                @goecs-menu="onGoecsMenu"
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
                      <div class="command-card" @click="wsConnected && openExecDrawer('backtrace', '三网回程检查')">
                        <span class="command-card-icon">🌐</span>
                        <span class="command-card-title">三网回程检查</span>
                        <span class="command-card-desc">zhanghanyun/backtrace，兼容 curl/wget</span>
                        <el-button
                          type="primary"
                          size="small"
                          :disabled="!wsConnected"
                          @click.stop="openExecDrawer('backtrace', '三网回程检查')"
                          class="command-card-btn"
                        >
                          执行
                        </el-button>
                      </div>
                      <div class="command-card" @click="wsConnected && openGoecsOptionDialog()">
                        <span class="command-card-icon">🖥</span>
                        <span class="command-card-title">融合怪脚本</span>
                        <span class="command-card-desc">oneclickvirt/ecs，可选 1-10 或 0 退出</span>
                        <el-button
                          type="primary"
                          size="small"
                          :disabled="!wsConnected"
                          @click.stop="openGoecsOptionDialog"
                          class="command-card-btn"
                        >
                          执行
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
          <div v-if="installExitCode !== 0 && installErrorMessage" class="install-status-msg">{{ installErrorMessage }}</div>
        </div>
        <pre ref="installLogRef" class="install-log">{{ installLog }}</pre>
      </div>
    </el-drawer>

    <!-- 融合怪选项 -->
    <el-dialog
      v-model="goecsOptionDialogVisible"
      title="融合怪脚本 - 选择项目"
      width="680"
      :close-on-click-modal="true"
      :class="['goecs-option-dialog', isDark && 'goecs-option-dialog--dark']"
      @closed="selectedGoecsOption = 1; goecsOptionsLoading = false"
    >
      <div v-if="goecsOptionsLoading" class="goecs-option-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在从服务器获取选项…</span>
      </div>
      <div v-else class="goecs-option-list">
        <div
          v-for="opt in goecsOptions"
          :key="opt.value"
          class="goecs-option-row"
          :class="{ 'is-selected': selectedGoecsOption === opt.value }"
          @click="selectedGoecsOption = opt.value"
        >
          <span class="goecs-option-dot" />
          <span class="goecs-option-num">{{ opt.value }}</span>
          <span class="goecs-option-text">{{ opt.label }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="goecsOptionDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="goecsOptionsLoading" @click="confirmGoecsOption">执行</el-button>
      </template>
    </el-dialog>

    <!-- 一键执行（三网回程 / 融合怪）实时日志 -->
    <el-drawer
      v-model="execDrawerVisible"
      :title="execDrawerTitle + ' - 实时日志'"
      direction="rtl"
      size="800"
      :close-on-click-modal="!execDrawerRunning"
      @closed="onExecDrawerClosed"
    >
      <div class="install-drawer-body">
        <div v-if="execDrawerRunning" class="install-status">执行中…</div>
        <div v-else-if="execDrawerExitCode != null" :class="['install-status', execDrawerExitCode === 0 ? 'success' : 'fail']">
          {{ execDrawerExitCode === 0 ? '执行完成' : '执行结束 (exit ' + execDrawerExitCode + ')，请查看下方日志' }}
          <div v-if="execDrawerExitCode !== 0 && execDrawerErrorMessage" class="install-status-msg">{{ execDrawerErrorMessage }}</div>
        </div>
        <pre ref="execLogRef" class="install-log">{{ execDrawerLog }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="VpsTerminal">
import { ref, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useSettingsStore from '@/store/modules/settings'
import useTagsViewStore from '@/store/modules/tagsView'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import { Folder, Monitor, Loading } from '@element-plus/icons-vue'
import TerminalPanel from '../components/TerminalPanel.vue'
import ServerMonitorPanel from '../components/ServerMonitorPanel.vue'
import SftpFilePanel from '../components/SftpFilePanel.vue'

const INSTALL_CMD = 'bash <(wget -qO- -o- https://github.com/233boy/sing-box/raw/main/install.sh)'

const GOECS_OPTIONS_FALLBACK = [
  { value: 1, label: '融合怪完全体(能测全测)' },
  { value: 2, label: '极简版(系统信息+CPU+内存+磁盘+测速节点5个)' },
  { value: 3, label: '精简版(系统信息+CPU+内存+磁盘+跨国平台解锁+路由+测速节点5个)' },
  { value: 4, label: '精简网络版(系统信息+CPU+内存+磁盘+回程+路由+测速节点5个)' },
  { value: 5, label: '精简解锁版(系统信息+CPU+内存+磁盘IO+跨国平台解锁+测速节点5个)' },
  { value: 6, label: '网络单项(IP质量检测+上游及三网回程+广州三网回程详细路由+全国延迟+TGDC+网站延迟+测速节点11个)' },
  { value: 7, label: '解锁单项(跨国平台解锁)' },
  { value: 8, label: '硬件单项(系统信息+CPU+dd磁盘测试+fio磁盘测试)' },
  { value: 9, label: 'IP质量检测(15个数据库的IP质量检测+邮件端口检测)' },
  { value: 10, label: '三网回程线路检测+三网回程详细路由(北京上海广州成都)+全国延迟+TGDC+网站延迟' },
  { value: 0, label: '退出程序' }
]
const goecsOptions = ref([...GOECS_OPTIONS_FALLBACK])
const goecsOptionsLoading = ref(false)

const route = useRoute()
const router = useRouter()
const isDark = computed(() => useSettingsStore().isDark)

// 仅在本页为终端路由时从 route 更新，避免 keep-alive 下切到非终端时 route 变成别页导致 instanceId 清空、终端被 cleanup 重连
const lastTerminalState = ref({ id: null, name: '', ip: '' })
watch(
  () =>
    route.name === 'VpsTerminal'
      ? {
          id: route.params.id,
          name: route.query.name,
          ip: route.query.ip
        }
      : null,
  (payload) => {
    if (payload == null) return
    const n = payload.id != null && payload.id !== '' ? Number(payload.id) : NaN
    lastTerminalState.value = {
      id: Number.isInteger(n) && n > 0 ? n : null,
      name: payload.name != null ? String(payload.name) : '',
      ip: payload.ip != null ? String(payload.ip) : ''
    }
  },
  { immediate: true }
)
const instanceId = computed(() => lastTerminalState.value.id)
const instanceName = computed(() => lastTerminalState.value.name)
const instanceIp = computed(() => lastTerminalState.value.ip)

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
const installErrorMessage = ref('')
let installReqId = 1

const execDrawerVisible = ref(false)
const execDrawerTitle = ref('')
const execDrawerLog = ref('')
const execLogRef = ref(null)
const execDrawerRunning = ref(false)
const execDrawerExitCode = ref(null)
const execDrawerErrorMessage = ref('')
const execDrawerReqId = ref(null)
let execDrawerReqIdCounter = 100000

/** 移除 ANSI 转义序列，避免在日志里显示 [32m、[0m 等控制码 */
function stripAnsiEscapes(str) {
  if (typeof str !== 'string') return ''
  return str.replace(/\x1b\[[0-9;]*[a-zA-Z]/g, '')
}

const goecsOptionDialogVisible = ref(false)
const selectedGoecsOption = ref(1)

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

function openExecDrawer(commandId, title, option) {
  if (!wsConnected.value) {
    ElMessage.warning('请先连接 SSH')
    return
  }
  const desc = title === '三网回程检查' ? '三网回程检查脚本（兼容 curl/wget）' : (option != null ? `融合怪脚本（已选选项 ${option}）` : '融合怪脚本')
  ElMessageBox.confirm(`将在当前服务器上执行「${title}」，是否继续？\n${desc}`, '确认执行', {
    confirmButtonText: '执行',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    execDrawerTitle.value = title
    execDrawerLog.value = ''
    execDrawerRunning.value = true
    execDrawerExitCode.value = null
    execDrawerErrorMessage.value = ''
    execDrawerReqId.value = ++execDrawerReqIdCounter
    execDrawerVisible.value = true
    nextTick(() => {
      const payload = { type: 'exec', commandId, reqId: execDrawerReqId.value }
      if (commandId === 'goecs' && option != null) payload.option = option
      sendJson(payload)
    })
  }).catch(() => {})
}

function openGoecsOptionDialog() {
  if (!wsConnected.value) {
    ElMessage.warning('请先连接 SSH')
    return
  }
  selectedGoecsOption.value = 1
  goecsOptionDialogVisible.value = true
  goecsOptionsLoading.value = true
  sendJson({ type: 'get_goecs_menu' })
}

function onGoecsMenu(options) {
  goecsOptionsLoading.value = false
  if (options && options.length > 0) {
    goecsOptions.value = options.map(o => ({ value: o.value, label: o.label || String(o.value) })).sort((a, b) => a.value - b.value)
  }
}

function confirmGoecsOption() {
  const opt = selectedGoecsOption.value
  goecsOptionDialogVisible.value = false
  const label = goecsOptions.value.find(o => o.value === opt)?.label || `选项 ${opt}`
  openExecDrawer('goecs', `融合怪脚本 - ${label}`, opt)
}

function onExecDrawerClosed() {
  execDrawerExitCode.value = null
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
    installErrorMessage.value = ''
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
  const raw = msg.data != null ? String(msg.data) : ''
  const data = stripAnsiEscapes(raw)
  if (msg.reqId === installReqId) {
    installLog.value += data
    nextTick(() => {
      const el = installLogRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
    return
  }
  if (msg.reqId === execDrawerReqId.value) {
    execDrawerLog.value += data
    nextTick(() => {
      const el = execLogRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  }
}

function isInstallSuccessByLog(log) {
  if (!log || typeof log !== 'string') return false
  const s = log.toLowerCase()
  return /installed|安装成功|success|完成|started|running|\[✓\]|\[ok\]/.test(s) && /sing-box|singbox/.test(s)
}

function onExecEnd(msg) {
  if (msg.reqId === installReqId) {
    if (msg.type === 'exec_error' && msg.message) {
      installErrorMessage.value = String(msg.message)
    }
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
    return
  }
  if (msg.reqId === execDrawerReqId.value) {
    if (msg.type === 'exec_error' && msg.message) {
      execDrawerErrorMessage.value = String(msg.message)
    }
    execDrawerRunning.value = false
    execDrawerExitCode.value = msg.code != null ? msg.code : (msg.type === 'exec_error' ? -1 : null)
    if (execDrawerExitCode.value === 0) {
      ElMessage.success(execDrawerTitle.value + ' 执行完成')
    }
  }
}

function onInstallDrawerClosed() {
  installExitCode.value = null
}

function goBack() {
  router.push({ path: '/resource/vps' })
}

// 切换实例时清空左侧/下方数据，避免串台
watch(instanceId, () => {
  sysinfoData.value = null
  sftpMessage.value = null
  wsConnected.value = false
})

// 仅在本页为终端路由时更新标题，避免 keep-alive 下切到非终端时用当前 route 把别的标签改成「VPS」
watch(
  () => (route.name === 'VpsTerminal' ? [instanceName.value, instanceIp.value, route.path] : null),
  (payload) => {
    if (payload == null) return
    const [name, ip, path] = payload
    const displayTitle = [name || 'VPS', ip].filter(Boolean).join(' - ') || '连接服务器'
    useSettingsStore().setTitle(displayTitle)
    useTagsViewStore().updateVisitedView({ path, title: displayTitle })
  },
  { immediate: true }
)

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
.install-status-msg {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.95;
}
.install-log {
  flex: 1;
  margin: 0;
  padding: 12px;
  overflow: auto;
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
  white-space: pre;
  word-break: normal;
  overflow-wrap: normal;
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

.goecs-option-dialog :deep(.el-dialog__body) {
  width: 100%;
  box-sizing: border-box;
}
.goecs-option-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--el-text-color-secondary);
}
.goecs-option-list {
  max-height: 60vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  box-sizing: border-box;
}
.goecs-option-row {
  display: grid;
  grid-template-columns: 20px 28px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  padding: 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
  flex-shrink: 0;
  overflow: visible;
}
.goecs-option-row:hover {
  border-color: var(--el-border-color-hover);
  background-color: var(--el-fill-color-light);
}
.goecs-option-row.is-selected {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
.goecs-option-dialog--dark .goecs-option-row.is-selected {
  background-color: rgba(30, 58, 95, 0.55);
  border-color: var(--el-color-primary);
}
.goecs-option-dialog--dark .goecs-option-row.is-selected .goecs-option-text {
  color: var(--el-text-color-primary);
}
.goecs-option-dot {
  width: 14px;
  height: 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  background-color: var(--el-bg-color);
  flex-shrink: 0;
}
.goecs-option-row.is-selected .goecs-option-dot {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary);
  box-shadow: inset 0 0 0 2px var(--el-bg-color);
}
.goecs-option-num {
  font-weight: 600;
  font-size: 13px;
  color: var(--el-color-primary);
  line-height: 1.5;
  text-align: left;
  min-width: 0;
}
.goecs-option-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
  text-align: left;
  word-break: break-word;
  overflow-wrap: break-word;
  min-width: 0;
  max-width: 100%;
  overflow: visible;
}
</style>
