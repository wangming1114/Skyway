<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`连接服务器 - ${instanceName || 'VPS'}`"
    width="90vw"
    fullscreen
    destroy-on-close
    class="ssh-connect-dialog"
    @closed="handleClosed"
  >
    <div class="ssh-layout">
      <div class="ssh-left">
        <ServerMonitorPanel
          :ip="instanceIp"
          :send-sysinfo="sendSysinfo"
          :sysinfo-data="sysinfoData"
          :visible="dialogVisible && !!instanceId"
        />
      </div>
      <div class="ssh-right">
        <div class="ssh-terminal-wrap">
          <TerminalPanel
            ref="terminalRef"
            :instance-id="instanceId"
            :visible="dialogVisible && !!instanceId"
            @sysinfo="onSysinfo"
            @sftp="onSftp"
            @connected-change="onWsConnectedChange"
          />
        </div>
        <div class="ssh-file-wrap">
          <SftpFilePanel
            :send-json="sendJson"
            :send-binary="sendBinary"
            :sftp-message="sftpMessage"
            :connected="wsConnected"
          />
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import TerminalPanel from './TerminalPanel.vue'
import ServerMonitorPanel from './ServerMonitorPanel.vue'
import SftpFilePanel from './SftpFilePanel.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  instanceId: { type: Number, default: null },
  instanceName: { type: String, default: '' },
  instanceIp: { type: String, default: '' }
})

const emit = defineEmits(['update:visible'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})

const terminalRef = ref(null)
const sysinfoData = ref(null)
const sftpMessage = ref(null)
/** 实际 WebSocket 是否已连接，用于文件面板仅在连接后请求 */
const wsConnected = ref(false)

function onWsConnectedChange(connected) {
  wsConnected.value = !!connected
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

function sendBinary(data) {
  terminalRef.value?.sendBinary?.(data)
}

function handleClosed() {
  sysinfoData.value = null
  wsConnected.value = false
  emit('update:visible', false)
}
</script>

<style scoped lang="scss">
.ssh-connect-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
    height: calc(100vh - 54px);
    overflow: hidden;
  }
}
.ssh-layout {
  display: flex;
  height: 100%;
}
.ssh-left {
  width: 280px;
  min-width: 200px;
  border-right: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light);
}
.ssh-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.ssh-terminal-wrap {
  flex: 1;
  min-height: 200px;
}
.ssh-file-wrap {
  height: 220px;
  border-top: 1px solid var(--el-border-color);
}
</style>
