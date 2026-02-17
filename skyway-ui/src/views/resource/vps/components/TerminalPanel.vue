<template>
  <div class="terminal-panel" @click="hideTermCtx">
    <div v-if="status === 'connecting'" class="terminal-status">正在连接 SSH...</div>
    <div v-else-if="status === 'error'" class="terminal-status error">{{ errorMessage }}</div>
    <div ref="terminalRef" class="terminal-container" @contextmenu.prevent="onTermCtx" />
    <!-- 选中后悬浮复制小图标 -->
    <div
      v-show="selBtnVisible"
      class="sel-copy-btn"
      :style="{ left: selBtnX + 'px', top: selBtnY + 'px' }"
      @mousedown.prevent.stop="doCopy"
      title="复制选中内容"
    >
      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z"/></svg>
    </div>
    <!-- 右键菜单 -->
    <teleport to="body">
      <div
        v-show="termCtxVisible"
        class="term-ctx-menu"
        :style="{ left: termCtxX + 'px', top: termCtxY + 'px' }"
      >
        <div class="term-ctx-item" @click="doCopy">📋 复制</div>
        <div class="term-ctx-item" @click="doPaste">📌 粘贴</div>
        <div class="term-ctx-divider"></div>
        <div class="term-ctx-item" @click="doSelectAll">全选</div>
        <div class="term-ctx-item" @click="doClear">清屏</div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import '@xterm/xterm/css/xterm.css'
import { getToken } from '@/utils/auth'

const props = defineProps({
  instanceId: { type: Number, required: true },
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['sysinfo', 'sftp', 'connected-change', 'exec-output', 'exec-end'])

const terminalRef = ref(null)
const status = ref('idle') // idle | connecting | connected | error
const errorMessage = ref('')
let term = null
let fitAddon = null
let ws = null
let resizeObserver = null
let fitThrottleId = null
let destroyed = false

// 选中悬浮复制按钮（复制后短暂不显示，避免打包后图标一直出现）
const selBtnVisible = ref(false)
const selBtnX = ref(0)
const selBtnY = ref(0)
let copyJustHappenedUntil = 0

// 右键菜单
const termCtxVisible = ref(false)
const termCtxX = ref(0)
const termCtxY = ref(0)

function getWsUrl() {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = location.host
  const path = base + '/ws/ssh'
  const token = getToken() || ''
  return `${protocol}//${host}${path}?instanceId=${props.instanceId}&token=${encodeURIComponent(token)}`
}

function connect() {
  if (ws && ws.readyState === WebSocket.OPEN) return
  if (!term || destroyed) return
  status.value = 'connecting'
  errorMessage.value = ''
  const url = getWsUrl()
  const socket = new WebSocket(url)
  socket.binaryType = 'arraybuffer'

  socket.onopen = () => {
    if (destroyed || !term) return
    status.value = 'connected'
    emit('connected-change', true)
    term.focus()
  }

  socket.onmessage = (ev) => {
    if (destroyed || !term) return
    if (typeof ev.data === 'string') {
      try {
        const obj = JSON.parse(ev.data)
        if (obj.type === 'error') {
          errorMessage.value = obj.message || '连接错误'
          status.value = 'error'
          emit('connected-change', false)
        } else if (obj.type === 'sysinfo') {
          emit('sysinfo', obj.data)
          return
        } else if (obj.type && obj.type.startsWith('sftp_')) {
          emit('sftp', obj)
          return
        } else if (obj.type === 'exec_output') {
          emit('exec-output', obj)
          return
        } else if (obj.type === 'exec_end' || obj.type === 'exec_error') {
          emit('exec-end', obj)
          return
        }
      } catch (_) {}
    }
    if (ev.data instanceof ArrayBuffer) {
      const buf = new Uint8Array(ev.data)
      term.write(buf)
    } else if (typeof ev.data === 'string') {
      term.write(ev.data)
    }
  }

  socket.onerror = () => {
    if (status.value === 'connecting' && !errorMessage.value) {
      errorMessage.value = 'WebSocket 连接失败'
    }
  }

  socket.onclose = (ev) => {
    if (status.value === 'connected' && term && !destroyed) {
      term.write('\r\n\r\n[连接已关闭]\r\n')
    }
    emit('connected-change', false)
    const wasConnecting = status.value === 'connecting'
    if (wasConnecting) status.value = 'error'
    if (status.value === 'error' && wasConnecting) {
      const detail = `连接失败 (code: ${ev.code}${ev.reason ? ', ' + ev.reason : ''})`
      let hint = '请确认后端服务已启动；若为 401/403 请重新登录后再试'
      if (ev.code === 1006) hint = 'code 1006 多为网络不可达或后端未启动，请检查后端与代理配置；若后端日志出现无权限，请检查握手权限规则'
      errorMessage.value = `${detail}\n${hint}`
    } else if (status.value === 'error' && !errorMessage.value) {
      errorMessage.value = '连接已断开'
    }
  }

  ws = socket
}

function disconnect() {
  if (ws) {
    ws.onopen = null
    ws.onmessage = null
    ws.onerror = null
    ws.onclose = null
    try {
      ws.close()
    } catch (_) {}
    ws = null
  }
  status.value = 'idle'
  emit('connected-change', false)
}

function sendSysinfo() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'sysinfo' }))
  }
}

function sendJson(obj) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(obj))
  }
}

defineExpose({ sendSysinfo, sendJson })

/** 仅在容器有尺寸且终端未销毁时调用 fit，避免 dimensions 异常 */
function doFit() {
  if (!fitAddon || !term || destroyed) return
  const el = terminalRef.value
  if (!el) return
  try {
    const rect = el.getBoundingClientRect()
    if (rect.width <= 0 || rect.height <= 0) return
    fitAddon.fit()
  } catch (_) {}
}

function scheduleFit() {
  if (fitThrottleId != null) return
  fitThrottleId = requestAnimationFrame(() => {
    fitThrottleId = null
    doFit()
  })
}

function initTerminal() {
  if (!terminalRef.value || term) return
  term = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Consolas, Monaco, monospace',
    theme: { background: '#1e1e1e', foreground: '#d4d4d4' }
  })
  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  term.open(terminalRef.value)
  term.onData((data) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const enc = new TextEncoder()
      ws.send(enc.encode(data))
    }
  })
  // 鼠标松开时检测选区，显示复制图标；按下时隐藏（准备新选区）
  const termEl = terminalRef.value
  if (termEl) {
    termEl.addEventListener('mouseup', onTermMouseUp)
    termEl.addEventListener('mousedown', onTermMouseDown)
  }
  // 终端 open 后再做一次 fit 并建立连接，避免在布局未就绪时 fit 导致 dimensions 异常
  nextTick(() => {
    scheduleFit()
    requestAnimationFrame(() => {
      if (destroyed || !term) return
      doFit()
      connect()
      if (terminalRef.value && !resizeObserver) {
        resizeObserver = new ResizeObserver(scheduleFit)
        resizeObserver.observe(terminalRef.value)
      }
    })
  })
}

// ── 剪贴板（兼容无 clipboard API 的环境，如打包后非 HTTPS）──
function writeClipboardText(text) {
  if (!text) return Promise.resolve()
  if (typeof navigator !== 'undefined' && navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
    return navigator.clipboard.writeText(text).catch(() => fallbackWriteText(text))
  }
  return Promise.resolve(fallbackWriteText(text))
}

function fallbackWriteText(text) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.left = '-9999px'
  ta.style.top = '0'
  document.body.appendChild(ta)
  ta.select()
  try {
    document.execCommand('copy')
  } finally {
    document.body.removeChild(ta)
  }
}

function readClipboardText() {
  if (typeof navigator !== 'undefined' && navigator.clipboard && typeof navigator.clipboard.readText === 'function') {
    return navigator.clipboard.readText()
  }
  return Promise.resolve('')
}

/** 无法读取剪贴板时，聚焦终端并静默模拟 Ctrl+V（非 HTTPS 下多数仍无效，不弹提示） */
function fallbackPasteBySimulateCtrlV() {
  if (!term) return
  term.focus()
  requestAnimationFrame(() => {
    const target = document.activeElement || terminalRef.value
    if (!target) return
    const opts = { key: 'v', code: 'KeyV', keyCode: 86, ctrlKey: true, bubbles: true }
    target.dispatchEvent(new KeyboardEvent('keydown', opts))
    target.dispatchEvent(new KeyboardEvent('keyup', opts))
  })
}

// ── 选中 & 右键功能 ──

function onTermMouseDown() {
  // 开始新选区前隐藏图标
  selBtnVisible.value = false
}

function onTermMouseUp(e) {
  // 鼠标松开后延迟一帧，让 xterm 完成选区更新再检测
  requestAnimationFrame(() => {
    if (!term) return
    if (Date.now() < copyJustHappenedUntil) return
    const sel = term.getSelection()
    if (sel) {
      selBtnVisible.value = true
      // 图标紧贴鼠标松开位置的右上方
      const el = terminalRef.value
      if (el) {
        const rect = el.getBoundingClientRect()
        // 靠近鼠标位置但限制在终端范围内
        let x = e.clientX + 6 - rect.left
        let y = e.clientY - 30 - rect.top
        if (x + 28 > rect.width) x = rect.width - 28
        if (y < 0) y = 0
        selBtnX.value = x
        selBtnY.value = y
      }
    } else {
      selBtnVisible.value = false
    }
  })
}

function doCopy() {
  if (!term) return
  const sel = term.getSelection()
  selBtnVisible.value = false
  termCtxVisible.value = false
  copyJustHappenedUntil = Date.now() + 200
  if (sel) {
    writeClipboardText(sel).catch(() => {})
    term.clearSelection()
  }
}

async function doPaste() {
  termCtxVisible.value = false
  if (!term) return
  try {
    const text = await readClipboardText()
    if (text && ws && ws.readyState === WebSocket.OPEN) {
      const enc = new TextEncoder()
      ws.send(enc.encode(text))
      term.focus()
    } else if (!text && term) {
      // 无 clipboard API 时（如打包后非 HTTPS）：模拟 Ctrl+V 让浏览器执行粘贴
      fallbackPasteBySimulateCtrlV()
    }
  } catch (_) {
    if (term) fallbackPasteBySimulateCtrlV()
  }
}

function doSelectAll() {
  termCtxVisible.value = false
  if (term) term.selectAll()
}

function doClear() {
  termCtxVisible.value = false
  if (term) {
    term.clear()
    // 发送 Ctrl+L 清屏
    if (ws && ws.readyState === WebSocket.OPEN) {
      const enc = new TextEncoder()
      ws.send(enc.encode('\x0c'))
    }
  }
}

function onTermCtx(event) {
  event.preventDefault()
  event.stopPropagation()
  termCtxX.value = event.clientX
  termCtxY.value = event.clientY
  termCtxVisible.value = true
  // 自动调整溢出
  nextTick(() => {
    const vh = window.innerHeight
    const vw = window.innerWidth
    if (termCtxY.value + 160 > vh) termCtxY.value = Math.max(4, vh - 160)
    if (termCtxX.value + 140 > vw) termCtxX.value = Math.max(4, vw - 140)
  })
}

function hideTermCtx() {
  termCtxVisible.value = false
}

function cleanup() {
  disconnect()
  if (resizeObserver && terminalRef.value) {
    try {
      resizeObserver.unobserve(terminalRef.value)
    } catch (_) {}
    resizeObserver = null
  }
  if (fitThrottleId != null) {
    cancelAnimationFrame(fitThrottleId)
    fitThrottleId = null
  }
  if (term) {
    try {
      term.dispose()
    } catch (_) {}
    term = null
  }
  fitAddon = null
}

watch(() => [props.visible, props.instanceId], ([v, id]) => {
  if (v && id) {
    destroyed = false
    nextTick(() => {
      initTerminal()
    })
  } else {
    destroyed = true
    cleanup()
  }
}, { immediate: true })

function globalClickClose() {
  termCtxVisible.value = false
}

onMounted(() => {
  document.addEventListener('click', globalClickClose)
})

onBeforeUnmount(() => {
  destroyed = true
  if (terminalRef.value) {
    terminalRef.value.removeEventListener('mouseup', onTermMouseUp)
    terminalRef.value.removeEventListener('mousedown', onTermMouseDown)
  }
  cleanup()
  document.removeEventListener('click', globalClickClose)
})
</script>

<style scoped lang="scss">
.terminal-panel {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  background: #1e1e1e;
  border-radius: 4px;
}
.terminal-status {
  padding: 12px;
  color: #999;
  font-size: 14px;
}
.terminal-status.error {
  color: #f56c6c;
  white-space: pre-line;
}
.terminal-container {
  width: 100%;
  height: 100%;
  padding: 8px;
  box-sizing: border-box;
}
.terminal-container :deep(.xterm) {
  height: 100%;
}
.terminal-container :deep(.xterm-viewport) {
  overflow: auto !important;
}
.sel-copy-btn {
  position: absolute;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: rgba(64, 158, 255, 0.85);
  color: #fff;
  border-radius: 4px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.25);
  user-select: none;
  transition: background 0.15s;
  &:hover { background: rgba(64, 158, 255, 1); }
}
</style>

<style lang="scss">
/* 终端右键菜单 -- unscoped for teleport */
.term-ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--el-bg-color-overlay, #fff);
  border: 1px solid var(--el-border-color-lighter, #e4e7ed);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
  padding: 4px 0;
  min-width: 120px;
  font-size: 13px;
}
.term-ctx-item {
  padding: 6px 16px;
  cursor: pointer;
  white-space: nowrap;
  &:hover {
    background: var(--el-fill-color-light, #f5f7fa);
    color: var(--el-color-primary, #409eff);
  }
}
.term-ctx-divider {
  height: 1px;
  background: var(--el-border-color-lighter, #e4e7ed);
  margin: 4px 0;
}
</style>
