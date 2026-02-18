<template>
  <div class="sftp-file-panel">
    <div class="panel-body">
      <div class="sftp-layout">
        <div class="tree-pane scroll-thin">
          <el-tree
            ref="treeRef"
            :props="treeProps"
            node-key="path"
            lazy
            :load="loadTreeNode"
            :expand-on-click-node="true"
            highlight-current
            @node-click="onTreeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">📁 {{ data.name }}</span>
            </template>
          </el-tree>
        </div>
        <div class="right-pane">
          <div class="toolbar">
            <div v-if="!pathEditing" class="toolbar-breadcrumb path-clickable" @click="startPathEdit">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item><span class="link" @click.stop="navigateTo('/')">根</span></el-breadcrumb-item>
                <el-breadcrumb-item v-for="(seg, i) in pathSegments" :key="i">
                  <span class="link" @click.stop="navigateTo('/' + pathSegments.slice(0, i + 1).join('/'))">{{ seg }}</span>
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            <el-input
              v-else
              ref="pathInputRef"
              v-model="pathInputValue"
              size="small"
              class="path-input"
              placeholder="/path"
              @keyup.enter="submitPathEdit"
              @keyup.escape="cancelPathEdit"
              @blur="cancelPathEdit"
            />
            <span class="toolbar-spacer" />
            <el-tooltip content="新建文件夹" placement="top"><el-button size="small" :icon="FolderAdd" :disabled="!connected" @click="openNewFolder" /></el-tooltip>
            <el-tooltip content="新建文件" placement="top"><el-button size="small" :icon="DocumentAdd" :disabled="!connected" @click="openNewFile" /></el-tooltip>
            <el-upload ref="uploadRef" :show-file-list="false" :before-upload="beforeUpload" :http-request="() => {}">
              <el-tooltip content="上传" placement="top"><el-button size="small" :icon="Upload" :disabled="!connected" /></el-tooltip>
            </el-upload>
            <el-tooltip v-if="copiedPath" content="粘贴" placement="top"><el-button size="small" :icon="DocumentCopy" @click="openPaste" /></el-tooltip>
            <el-tooltip content="刷新" placement="top"><el-button size="small" :icon="Refresh" :disabled="!connected" @click="loadList(currentPath)" /></el-tooltip>
          </div>
          <div v-if="uploadProgress != null" class="upload-progress-bar">
            <span class="upload-progress-label">{{ uploadFileName }} {{ uploadProgress }}%</span>
            <el-progress :percentage="uploadProgress" :stroke-width="6" style="flex:1" />
            <el-button size="small" type="danger" plain style="margin-left:8px" @click="cancelUpload">取消</el-button>
          </div>
          <div class="file-list scroll-thin" @contextmenu="onBlankContextMenu">
            <el-table
              ref="tableRef"
              :data="sortedFileList"
              size="small"
              height="100%"
              v-loading="loading"
              :default-sort="{ prop: 'type', order: 'ascending' }"
              @sort-change="onSortChange"
              @row-click="onTableRowClick"
              @row-dblclick="onRowDblClick"
              @row-contextmenu="onRowContextMenu"
              @selection-change="onSelectionChange"
            >
              <el-table-column type="selection" width="36" />
              <el-table-column prop="name" label="名称" min-width="180" sortable="custom">
                <template #default="{ row }">
                  <span>{{ row.directory ? '📁 ' : '📄 ' }}{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="size" label="大小" width="88" sortable="custom">
                <template #default="{ row }">
                  {{ row.directory ? '-' : formatSize(row.size) }}
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="72" sortable="custom">
                <template #default="{ row }">
                  {{ row.directory ? '文件夹' : '文件' }}
                </template>
              </el-table-column>
              <el-table-column label="修改时间" width="140">
                <template #default="{ row }">{{ formatMtime(row.mtime) }}</template>
              </el-table-column>
              <el-table-column label="权限" width="100">
                <template #default="{ row }">{{ formatMode(row.mode) }}</template>
              </el-table-column>
              <el-table-column label="属主" width="80">
                <template #default="{ row }">{{ row.uid != null && row.gid != null ? `${row.uid}/${row.gid}` : '-' }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <teleport to="body">
      <div
        ref="ctxMenuRef"
        v-show="ctxMenuVisible"
        class="ctx-menu"
        :style="{ left: ctxMenuX + 'px', top: ctxMenuY + 'px' }"
      >
        <!-- 多选批量操作 -->
        <template v-if="selectedRows.length > 1 && ctxMenuRow">
          <div class="ctx-item ctx-batch-label">已选 {{ selectedRows.length }} 项</div>
          <div class="ctx-divider"></div>
          <div class="ctx-item" @click="ctxAction(() => batchCopy())">📋 批量复制</div>
          <div class="ctx-item" @click="ctxAction(() => batchCut())">✂️ 批量剪切</div>
          <div v-if="copiedPath || copiedPaths.length" class="ctx-item" @click="ctxAction(() => openPaste())">📌 粘贴到此处</div>
          <div class="ctx-divider"></div>
          <div class="ctx-item" @click="ctxAction(() => batchChmod())">🔒 批量权限</div>
          <div class="ctx-divider"></div>
          <div class="ctx-item ctx-danger" @click="ctxAction(() => batchDelete())">🗑️ 批量删除</div>
        </template>
        <!-- 单项操作 -->
        <template v-else-if="ctxMenuRow">
          <div class="ctx-item" @click="ctxAction(() => onRowDblClick(ctxMenuRow))">
            {{ ctxMenuRow.directory ? '📂 打开' : '📝 编辑' }}
          </div>
          <div v-if="!ctxMenuRow.directory" class="ctx-item" @click="ctxAction(() => downloadFile(ctxMenuRow))">📥 下载</div>
          <div v-if="ctxMenuRow.directory" class="ctx-item" @click="ctxAction(() => triggerUploadToFolder(ctxMenuRow))">📤 上传到此处</div>
          <div class="ctx-divider"></div>
          <div class="ctx-item" @click="ctxAction(() => openRenameFor(ctxMenuRow))">✏️ 重命名</div>
          <div class="ctx-item" @click="ctxAction(() => copyPathFor(ctxMenuRow))">📋 复制</div>
          <div class="ctx-item" @click="ctxAction(() => cutPathFor(ctxMenuRow))">✂️ 剪切</div>
          <div v-if="copiedPath || copiedPaths.length" class="ctx-item" @click="ctxAction(() => openPaste())">📌 粘贴到此处</div>
          <div class="ctx-divider"></div>
          <div class="ctx-item" @click="ctxAction(() => openChmodFor(ctxMenuRow))">🔒 权限</div>
          <div class="ctx-item" @click="ctxAction(() => openChownFor(ctxMenuRow))">👤 归属</div>
          <div class="ctx-divider"></div>
          <div class="ctx-item ctx-danger" @click="ctxAction(() => confirmDeleteFor(ctxMenuRow))">🗑️ 删除</div>
        </template>
        <!-- 空白处右键(无选中行) -->
        <template v-else>
          <div class="ctx-item" @click="ctxAction(() => openNewFolder())">📁 新建文件夹</div>
          <div class="ctx-item" @click="ctxAction(() => openNewFile())">📄 新建文件</div>
          <div class="ctx-item" @click="ctxAction(() => triggerUpload())">📤 上传</div>
          <div v-if="copiedPath" class="ctx-item" @click="ctxAction(() => openPaste())">📌 粘贴到此处</div>
          <div class="ctx-item" @click="ctxAction(() => loadList(currentPath))">🔄 刷新</div>
        </template>
      </div>
    </teleport>

    <el-dialog v-model="newFolderVisible" title="新建文件夹" width="400px">
      <el-input v-model="newFolderName" placeholder="文件夹名" />
      <template #footer>
        <el-button @click="newFolderVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNewFolder">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="newFileVisible" title="新建文件" width="400px">
      <el-input v-model="newFileName" placeholder="文件名" />
      <template #footer>
        <el-button @click="newFileVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNewFile">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="renameVisible" title="重命名" width="400px">
      <el-input v-model="renameValue" placeholder="新名称" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRename">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="chmodVisible" title="修改权限" width="400px">
      <el-input v-model="chmodValue" placeholder="八进制，如 0644" />
      <template #footer>
        <el-button @click="chmodVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChmod">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="chownVisible" title="修改归属" width="400px">
      <el-input v-model="chownUid" placeholder="UID" type="number" />
      <el-input v-model="chownGid" placeholder="GID" type="number" style="margin-top:8px" />
      <template #footer>
        <el-button @click="chownVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChown">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="pasteVisible" title="粘贴" width="400px">
      <p>将 {{ copiedPath }} {{ pasteMode === 'move' ? '移动' : '复制' }}到当前目录？</p>
      <el-input v-model="pasteName" placeholder="目标名称（留空则同名）" />
      <template #footer>
        <el-button @click="pasteVisible = false">取消</el-button>
        <el-button v-if="pasteMode === 'copy'" type="primary" @click="submitCopy">复制到此处</el-button>
        <el-button v-if="pasteMode === 'move'" type="primary" @click="submitMove">移动到此</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="editorVisible" :title="editorPath ? editorPath.split('/').pop() : '编辑'" size="60%" direction="rtl">
      <div class="editor-wrap">
        <el-input v-model="editorContent" type="textarea" :rows="24" placeholder="文件内容" />
        <div class="editor-actions">
          <el-button type="primary" @click="saveEditor">保存</el-button>
          <el-button @click="editorVisible = false">关闭</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderAdd, DocumentAdd, Upload, Refresh, DocumentCopy } from '@element-plus/icons-vue'

const props = defineProps({
  sendJson: { type: Function, default: null },
  sftpMessage: { type: Object, default: null },
  connected: { type: Boolean, default: false }
})

const currentPath = ref('/')
const fileList = ref([])
const sortProp = ref('type')
const sortOrder = ref('ascending') // ascending = 文件夹在上 / 名称 A-Z / 小到大
const loading = ref(false)
const treeRef = ref(null)
const treeProps = { label: 'name', children: 'children', isLeaf: 'isLeaf' }
const tableRef = ref(null)
const selectedRow = ref(null)
const selectedRows = ref([])
const copiedPath = ref('')
const copiedPaths = ref([]) // 批量复制/剪切
const batchChmodRows = ref([]) // 批量chmod
const pasteMode = ref('copy') // 'copy' | 'move'
const LOADING_TIMEOUT_MS = 10000
let sftpReqId = 0
const pending = ref({})
let lastListReqId = null
let loadingTimeoutId = null

const pathSegments = computed(() => {
  const p = (currentPath.value || '/').replace(/^\/+|\/+$/g, '')
  return p ? p.split('/') : []
})

function compareType(a, b) {
  if (a.directory !== b.directory) return a.directory ? -1 : 1
  return 0
}
function compareName(a, b) {
  const ta = compareType(a, b)
  if (ta !== 0) return ta
  return (a.name || '').localeCompare(b.name || '', undefined, { sensitivity: 'base' })
}
function compareSize(a, b) {
  const ta = compareType(a, b)
  if (ta !== 0) return ta
  const sa = a.directory ? -1 : (a.size ?? 0)
  const sb = b.directory ? -1 : (b.size ?? 0)
  return sa - sb
}

const sortedFileList = computed(() => {
  const list = fileList.value || []
  const prop = sortProp.value
  const asc = sortOrder.value === 'ascending'
  const cmp = (a, b) => {
    let c = 0
    if (prop === 'type') c = compareType(a, b) || (a.name || '').localeCompare(b.name || '', undefined, { sensitivity: 'base' })
    else if (prop === 'name') c = compareName(a, b)
    else if (prop === 'size') c = compareSize(a, b)
    return asc ? c : -c
  }
  return [...list].sort(cmp)
})

function onSortChange({ prop, order }) {
  if (prop) {
    sortProp.value = prop
    sortOrder.value = order || 'ascending'
  }
}

const newFolderVisible = ref(false)
const newFolderName = ref('')
const newFileVisible = ref(false)
const newFileName = ref('')
const renameVisible = ref(false)
const renameValue = ref('')
const chmodVisible = ref(false)
const chmodValue = ref('0644')
const chownVisible = ref(false)
const chownUid = ref('')
const chownGid = ref('')
const pasteVisible = ref(false)
const pasteName = ref('')
const editorVisible = ref(false)
const editorPath = ref('')
const editorContent = ref('')
const ctxMenuVisible = ref(false)
const ctxMenuX = ref(0)
const ctxMenuY = ref(0)
const ctxMenuRow = ref(null)
const ctxMenuRef = ref(null)
const pathEditing = ref(false)
const pathInputValue = ref('')
const pathInputRef = ref(null)
const uploadProgress = ref(null)
const uploadFileName = ref('')
const uploadRef = ref(null)
/** 右击「上传到此处」时设为目标目录，beforeUpload 使用后清空 */
const uploadTargetPath = ref(null)
let uploadCancelled = false
/** SSHJ SFTP 单次写入约 32KB 限制，过大会导致 EOF while reading packet，故分片不超过 32KB */
const CHUNK_SIZE = 32 * 1024
const UPLOAD_CHUNK_THRESHOLD = 1024 * 1024
const pendingWaits = {}

function clearLoadingTimeout() {
  if (loadingTimeoutId != null) {
    clearTimeout(loadingTimeoutId)
    loadingTimeoutId = null
  }
}

function send(type, payload = {}) {
  if (!props.sendJson) return
  const id = ++sftpReqId
  pending.value[id] = { type, ...payload }
  props.sendJson({ type, _id: id, ...payload })
  return id
}

function loadList(path) {
  if (!props.sendJson || !path) return
  if (!props.connected) return
  clearLoadingTimeout()
  lastListReqId = send('sftp_list', { path })
  loading.value = true
  loadingTimeoutId = setTimeout(() => {
    loadingTimeoutId = null
    if (loading.value) {
      loading.value = false
      ElMessage.warning('文件列表请求超时，请点击刷新重试')
    }
  }, LOADING_TIMEOUT_MS)
}

// tree reqId -> resolve callback
const treePending = {}
let rootAutoExpanded = false

function expandRootNode() {
  if (rootAutoExpanded) return
  const rootNode = treeRef.value?.getNode('/')
  if (rootNode) {
    rootAutoExpanded = true
    if (!rootNode.expanded) {
      rootNode.expand()
    }
  }
}

// 连接建立后自动展开 '/' 节点
watch(() => props.connected, (v) => {
  if (v && !rootAutoExpanded) {
    nextTick(() => expandRootNode())
  }
})

function loadTreeNode(node, resolve) {
  if (node.level === 0) {
    // level-0 虚拟根：返回 '/' 节点
    resolve([{ name: '/', path: '/', isLeaf: false }])
    // 如已连接，立即展开
    if (props.connected) {
      nextTick(() => expandRootNode())
    }
    return
  }
  const path = node.data?.path ?? '/'
  if (!props.connected || !props.sendJson) {
    resolve([])
    return
  }
  const id = send('sftp_list', { path })
  treePending[id] = resolve
  // timeout fallback
  setTimeout(() => {
    if (treePending[id]) {
      delete treePending[id]
      resolve([])
    }
  }, 8000)
}

function handleTreeSftpList(msg) {
  const resolve = treePending[msg.reqId]
  if (!resolve) return
  delete treePending[msg.reqId]
  if (msg.error) {
    ElMessage.warning('目录加载失败: ' + (msg.error || '未知错误'))
    resolve([])
    return
  }
  const dirs = (msg.data || [])
    .filter((r) => r.directory)
    .map((r) => ({ name: r.name, path: r.path, isLeaf: false }))
  resolve(dirs)
}

function onTableRowClick(row) {
  selectedRow.value = row
}

function onSelectionChange(rows) {
  selectedRows.value = rows
  if (rows.length === 1) selectedRow.value = rows[0]
}

function onTreeClick(data) {
  currentPath.value = data.path
  loadList(data.path)
  // 同步高亮
  treeRef.value?.setCurrentKey(data.path)
}

function navigateTo(path) {
  const p = (path || '/').trim().replace(/\/+/g, '/').replace(/\/$/, '') || '/'
  currentPath.value = p
  loadList(p)
  treeRef.value?.setCurrentKey(p)
}

function startPathEdit() {
  pathInputValue.value = currentPath.value || '/'
  pathEditing.value = true
  nextTick(() => pathInputRef.value?.focus())
}

function submitPathEdit() {
  const p = (pathInputValue.value || '').trim().replace(/\/+/g, '/').replace(/\/$/, '') || '/'
  pathEditing.value = false
  if (p) navigateTo(p)
}

function cancelPathEdit() {
  pathEditing.value = false
}

function waitForReqId(id, timeoutMs = 60000) {
  return new Promise((resolve, reject) => {
    pendingWaits[id] = { resolve, reject }
    setTimeout(() => {
      if (pendingWaits[id]) {
        delete pendingWaits[id]
        reject(new Error('timeout'))
      }
    }, timeoutMs)
  })
}

function parentDir(path) {
  if (!path || path === '/') return '/'
  const i = path.lastIndexOf('/')
  return i <= 0 ? '/' : path.substring(0, i)
}

function rowFromMsg(msg, pathKey = 'path') {
  const path = msg[pathKey] ?? msg.path
  return {
    name: msg.name ?? path?.split('/').pop(),
    path,
    directory: !!msg.directory,
    size: msg.size ?? 0,
    mtime: msg.mtime,
    mode: msg.mode,
    uid: msg.uid,
    gid: msg.gid
  }
}

function isInCurrentDir(path) {
  const cur = currentPath.value || '/'
  return parentDir(path) === cur
}

function onSftpMessage(msg) {
  if (!msg || !msg.type) return
  if (msg.reqId != null && pendingWaits[msg.reqId]) {
    const w = pendingWaits[msg.reqId]
    delete pendingWaits[msg.reqId]
    w.resolve(msg)
  }
  if (msg.type === 'sftp_home') {
    // 首次连接获取到家目录后，自动导航过去
    if (msg.home) {
      const home = msg.home
      currentPath.value = home
      loadList(home)
      nextTick(() => {
        treeRef.value?.setCurrentKey(home)
      })
    }
    return
  }
  if (msg.type === 'sftp_list') {
    // route to tree if it's a tree request
    if (msg.reqId != null && treePending[msg.reqId]) {
      handleTreeSftpList(msg)
      return
    }
    if (msg.reqId === lastListReqId) {
      clearLoadingTimeout()
      loading.value = false
      if (msg.error) {
        ElMessage.error(msg.error)
        return
      }
      fileList.value = (msg.data || []).map((r) => ({
        name: r.name,
        path: r.path,
        directory: !!r.directory,
        size: r.size || 0,
        mtime: r.mtime,
        mode: r.mode,
        uid: r.uid,
        gid: r.gid
      }))
      currentPath.value = msg.path || currentPath.value
    }
  } else if (msg.type === 'sftp_download') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      try {
        const bin = atob(msg.base64 || '')
        const arr = new Uint8Array(bin.length)
        for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i)
        const blob = new Blob([arr])
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = (msg.path || 'download').split('/').pop()
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        ElMessage.error('下载失败')
      }
    }
  } else if (msg.type === 'sftp_upload') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('上传成功')
      if (isInCurrentDir(msg.path)) {
        fileList.value = [...fileList.value, rowFromMsg(msg)]
      }
    }
  } else if (msg.type === 'sftp_mkdir') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      if (isInCurrentDir(msg.path)) {
        fileList.value = [...fileList.value, rowFromMsg(msg)]
      }
    }
  } else if (msg.type === 'sftp_touch') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      if (isInCurrentDir(msg.path)) {
        fileList.value = [...fileList.value, rowFromMsg(msg)]
      }
    }
  } else if (msg.type === 'sftp_rename') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      const i = fileList.value.findIndex((r) => r.path === msg.path)
      if (i !== -1) {
        const next = [...fileList.value]
        next[i] = rowFromMsg({ ...msg, path: msg.newPath })
        fileList.value = next
      }
    }
  } else if (msg.type === 'sftp_delete') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('已删除')
      fileList.value = fileList.value.filter((r) => r.path !== msg.path)
    }
  } else if (msg.type === 'sftp_read_text') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      editorContent.value = msg.content ?? ''
      editorPath.value = msg.path
      editorVisible.value = true
    }
  } else if (msg.type === 'sftp_write_text') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('保存成功')
      editorVisible.value = false
    }
  } else if (msg.type === 'sftp_copy') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      pasteVisible.value = false
      if (isInCurrentDir(msg.dest)) {
        fileList.value = [...fileList.value, rowFromMsg({ ...msg, path: msg.dest })]
      }
    }
  } else if (msg.type === 'sftp_move') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      pasteVisible.value = false
      fileList.value = fileList.value.filter((r) => r.path !== msg.path)
      if (isInCurrentDir(msg.dest)) {
        fileList.value = [...fileList.value, rowFromMsg({ ...msg, path: msg.dest })]
      }
    }
  } else if (msg.type === 'sftp_chmod') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      chmodVisible.value = false
      const i = fileList.value.findIndex((r) => r.path === msg.path)
      if (i !== -1) {
        const next = [...fileList.value]
        next[i] = { ...next[i], mode: msg.mode }
        fileList.value = next
      }
    }
  } else if (msg.type === 'sftp_chown') {
    if (msg.error) ElMessage.error(msg.error)
    else {
      ElMessage.success('操作成功')
      chownVisible.value = false
      const i = fileList.value.findIndex((r) => r.path === msg.path)
      if (i !== -1) {
        const next = [...fileList.value]
        next[i] = { ...next[i], uid: msg.uid, gid: msg.gid }
        fileList.value = next
      }
    }
  }
}

watch(() => props.sftpMessage, (m) => {
  if (m) onSftpMessage(m)
}, { deep: true })

function showCtxMenu(event, row) {
  event.preventDefault()
  event.stopPropagation()
  if (row) selectedRow.value = row
  ctxMenuRow.value = row || null
  ctxMenuX.value = event.clientX
  ctxMenuY.value = event.clientY
  ctxMenuVisible.value = true
  // 等渲染后检查是否超出视口，自动上移
  nextTick(() => {
    const el = ctxMenuRef.value
    if (!el) return
    const rect = el.getBoundingClientRect()
    const vw = window.innerWidth
    const vh = window.innerHeight
    if (rect.bottom > vh) {
      ctxMenuY.value = Math.max(4, vh - rect.height - 4)
    }
    if (rect.right > vw) {
      ctxMenuX.value = Math.max(4, vw - rect.width - 4)
    }
  })
}

function onRowContextMenu(row, col, event) {
  showCtxMenu(event, row)
}

function onBlankContextMenu(event) {
  // 只在 file-list 空白处触发
  if (event.target.closest('.el-table__row')) return
  showCtxMenu(event, null)
}

function ctxAction(fn) {
  ctxMenuVisible.value = false
  fn()
}

function hideCtxMenu() {
  ctxMenuVisible.value = false
}

function openRenameFor(row) {
  if (!row) return
  selectedRow.value = row
  openRename()
}

function copyPathFor(row) {
  if (!row) return
  selectedRow.value = row
  copyPath()
}

function cutPathFor(row) {
  if (!row) return
  selectedRow.value = row
  cutPath()
}

function openChmodFor(row) {
  if (!row) return
  selectedRow.value = row
  openChmod()
}

function openChownFor(row) {
  if (!row) return
  selectedRow.value = row
  openChown()
}

function confirmDeleteFor(row) {
  if (!row) return
  selectedRow.value = row
  confirmDelete()
}

function onRowDblClick(row) {
  if (!row) return
  if (row.directory) {
    currentPath.value = row.path
    loadList(row.path)
    // 同步树高亮和展开
    nextTick(() => {
      treeRef.value?.setCurrentKey(row.path)
      const treeNode = treeRef.value?.getNode(row.path)
      if (treeNode && !treeNode.expanded) treeNode.expand()
    })
  } else {
    openEditor(row)
  }
}

function formatSize(n) {
  if (n == null || n === 0) return '-'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatMode(mask) {
  if (mask == null || mask === undefined) return '-'
  const m = Number(mask) & 0o7777
  const s = ['---', '--x', '-w-', '-wx', 'r--', 'r-x', 'rw-', 'rwx']
  const u = s[(m >> 6) & 7]
  const g = s[(m >> 3) & 7]
  const o = s[m & 7]
  return u + g + o
}

function formatMtime(ts) {
  if (ts == null || ts === undefined) return '-'
  const t = typeof ts === 'number' ? ts : parseInt(ts, 10)
  if (Number.isNaN(t)) return '-'
  const d = new Date(t * 1000)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function downloadFile(row) {
  if (!props.sendJson || !row || row.directory) return
  send('sftp_download', { path: row.path })
}

function openNewFolder() {
  newFolderName.value = ''
  newFolderVisible.value = true
}

function validateName(name) {
  if (name == null || (typeof name === 'string' && name.trim() === '')) {
    ElMessage.error('名称不能为空')
    return false
  }
  const s = String(name).trim()
  if (/\.\.|[\\/]/.test(s)) {
    ElMessage.error('名称不能包含 ..、/、\\')
    return false
  }
  return true
}

function submitNewFolder() {
  const name = (newFolderName.value || '').trim()
  if (!validateName(name)) return
  const path = currentPath.value.endsWith('/') ? currentPath.value + name : currentPath.value + '/' + name
  send('sftp_mkdir', { path })
  newFolderVisible.value = false
}

function openNewFile() {
  newFileName.value = ''
  newFileVisible.value = true
}

function submitNewFile() {
  const name = (newFileName.value || '').trim()
  if (!validateName(name)) return
  const path = currentPath.value.endsWith('/') ? currentPath.value + name : currentPath.value + '/' + name
  send('sftp_touch', { path, name })
  newFileVisible.value = false
}

function openRename() {
  if (!selectedRow.value) return
  renameValue.value = selectedRow.value.name
  renameVisible.value = true
}

function submitRename() {
  const newName = (renameValue.value || '').trim()
  if (!validateName(newName) || !selectedRow.value) return
  // 从文件自身路径提取父目录，而非 currentPath
  const oldPath = selectedRow.value.path
  const lastSlash = oldPath.lastIndexOf('/')
  const parentDir = lastSlash > 0 ? oldPath.substring(0, lastSlash) : '/'
  const newPath = parentDir + (parentDir.endsWith('/') ? '' : '/') + newName
  send('sftp_rename', { path: oldPath, newPath })
  renameVisible.value = false
}

function confirmDelete() {
  if (!selectedRow.value) return
  const { name, path } = selectedRow.value
  ElMessageBox.confirm(`确定删除「${name}」？目录将递归删除。`, '确认删除', {
    type: 'warning'
  }).then(() => {
    send('sftp_delete', { path })
  }).catch(() => {})
}

// ── 批量操作 ──

function batchDelete() {
  const rows = selectedRows.value
  if (!rows.length) return
  ElMessageBox.confirm(`确定删除选中的 ${rows.length} 个项目？目录将递归删除。`, '批量删除', {
    type: 'warning'
  }).then(() => {
    rows.forEach(r => send('sftp_delete', { path: r.path }))
  }).catch(() => {})
}

function batchChmod() {
  const rows = selectedRows.value
  if (!rows.length) return
  // 预填第一个文件的权限
  const first = rows[0]
  if (first && first.mode != null) {
    chmodValue.value = (Number(first.mode) & 0o7777).toString(8).padStart(4, '0')
  } else {
    chmodValue.value = '0644'
  }
  chmodVisible.value = true
  // 标记为批量模式
  batchChmodRows.value = [...rows]
}

function batchCopy() {
  const rows = selectedRows.value
  if (!rows.length) return
  copiedPaths.value = rows.map(r => r.path)
  pasteMode.value = 'copy'
  copiedPath.value = rows[0].path // 兼容单文件逻辑
  ElMessage.success(`已复制 ${rows.length} 个项目`)
}

function batchCut() {
  const rows = selectedRows.value
  if (!rows.length) return
  copiedPaths.value = rows.map(r => r.path)
  pasteMode.value = 'move'
  copiedPath.value = rows[0].path // 兼容单文件逻辑
  ElMessage.success(`已剪切 ${rows.length} 个项目`)
}

function readSliceAsBase64(file, offset, length) {
  return new Promise((resolve, reject) => {
    const blob = file.slice(offset, offset + length)
    const reader = new FileReader()
    reader.onload = () => {
      const base64 = (reader.result || '').split(',')[1] || ''
      resolve(base64)
    }
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(blob)
  })
}

function cancelUpload() {
  uploadCancelled = true
  send('sftp_upload_cancel', { path: '' })
  ElMessage.warning('上传已取消')
  uploadProgress.value = null
}

/** 右击菜单触发的上传：打开文件选择器 */
function triggerUpload() {
  if (!props.connected) return
  nextTick(() => {
    const el = uploadRef.value?.$el
    const input = el?.querySelector?.('input[type=file]')
    if (input) input.click()
  })
}

/** 右击文件夹「上传到此处」：设为目标目录后打开文件选择器 */
function triggerUploadToFolder(row) {
  if (!row?.directory || !props.connected) return
  uploadTargetPath.value = row.path
  triggerUpload()
}

async function doChunkedUpload(file, basePathOverride) {
  const basePath = (basePathOverride || currentPath.value).endsWith('/')
    ? (basePathOverride || currentPath.value)
    : (basePathOverride || currentPath.value) + '/'
  uploadProgress.value = 0
  uploadFileName.value = file.name
  uploadCancelled = false
  try {
    const startId = send('sftp_upload_start', { path: basePath, name: file.name, size: file.size })
    const startResp = await waitForReqId(startId)
    if (startResp.error) {
      ElMessage.error(startResp.error)
      return
    }
    const targetPath = startResp.path
    let offset = 0
    while (offset < file.size) {
      if (uploadCancelled) return
      const len = Math.min(CHUNK_SIZE, file.size - offset)
      const base64 = await readSliceAsBase64(file, offset, len)
      if (uploadCancelled) return
      const chunkId = send('sftp_upload_chunk', { path: targetPath, offset, base64 })
      const chunkResp = await waitForReqId(chunkId)
      if (uploadCancelled) return
      if (chunkResp.error) {
        ElMessage.error(chunkResp.error)
        return
      }
      offset += len
      uploadProgress.value = Math.round(100 * offset / file.size)
    }
    if (uploadCancelled) return
    const endId = send('sftp_upload_end', { path: targetPath })
    const endResp = await waitForReqId(endId)
    if (endResp.error) {
      ElMessage.error(endResp.error)
    } else {
      ElMessage.success('上传成功')
    }
  } finally {
    if (!uploadCancelled) {
      setTimeout(() => { uploadProgress.value = null }, 800)
    }
  }
}

function beforeUpload(file) {
  if (!props.sendJson || !props.connected) return false
  const rawPath = uploadTargetPath.value || currentPath.value
  const basePath = rawPath.endsWith('/') ? rawPath : rawPath + '/'
  uploadTargetPath.value = null
  if (file.size > UPLOAD_CHUNK_THRESHOLD) {
    doChunkedUpload(file, basePath)
    return false
  }
  const reader = new FileReader()
  reader.onload = () => {
    const base64 = (reader.result || '').split(',')[1] || ''
    if (!base64) return
    send('sftp_upload', { path: basePath, name: file.name, base64 })
  }
  reader.readAsDataURL(file)
  return false
}

function copyPath() {
  if (!selectedRow.value) return
  copiedPath.value = selectedRow.value.path
  pasteMode.value = 'copy'
  ElMessage.success('已复制路径，可点击「粘贴」复制到当前目录')
}

function cutPath() {
  if (!selectedRow.value) return
  copiedPath.value = selectedRow.value.path
  pasteMode.value = 'move'
  ElMessage.success('已剪切，可进入目标目录后点击「粘贴」移动')
}

function openPaste() {
  if (!copiedPath.value && !copiedPaths.value.length) return
  // 批量模式直接执行，不需要重命名
  if (copiedPaths.value.length > 1) {
    doBatchPaste()
    return
  }
  pasteName.value = copiedPath.value.split('/').pop()
  pasteVisible.value = true
}

function doBatchPaste() {
  const base = currentPath.value.endsWith('/') ? currentPath.value : currentPath.value + '/'
  const paths = [...copiedPaths.value]
  const mode = pasteMode.value
  if (mode === 'move') {
    ElMessageBox.confirm(`移动 ${paths.length} 个项目到当前目录？`, '确认批量移动', {
      type: 'warning'
    }).then(() => {
      paths.forEach(p => {
        const name = p.split('/').pop()
        send('sftp_move', { path: p, dest: base + name })
      })
      copiedPaths.value = []
      copiedPath.value = ''
    }).catch(() => {})
  } else {
    paths.forEach(p => {
      const name = p.split('/').pop()
      send('sftp_copy', { path: p, dest: base + name })
    })
    ElMessage.success(`已粘贴 ${paths.length} 个项目`)
  }
}

function submitCopy() {
  const base = currentPath.value.endsWith('/') ? currentPath.value : currentPath.value + '/'
  const name = (pasteName.value || '').trim() || copiedPath.value.split('/').pop()
  if (!validateName(name)) return
  const dest = base + name
  pasteVisible.value = false
  send('sftp_copy', { path: copiedPath.value, dest })
}

function submitMove() {
  const base = currentPath.value.endsWith('/') ? currentPath.value : currentPath.value + '/'
  const name = (pasteName.value || '').trim() || copiedPath.value.split('/').pop()
  if (!validateName(name)) return
  const dest = base + name
  const src = copiedPath.value
  pasteVisible.value = false
  ElMessageBox.confirm('移动后源文件将不存在，目标若已存在会被覆盖。确定继续？', '确认移动', {
    type: 'warning'
  }).then(() => {
    send('sftp_move', { path: src, dest })
  }).catch(() => {})
}

function openChmod() {
  if (!selectedRow.value) return
  // 预填当前文件的权限（八进制）
  const cur = selectedRow.value.mode
  if (cur != null) {
    const octal = (Number(cur) & 0o7777).toString(8).padStart(4, '0')
    chmodValue.value = octal
  } else {
    chmodValue.value = '0644'
  }
  chmodVisible.value = true
}

function submitChmod() {
  const mode = (chmodValue.value || '').trim()
  if (!/^[0-7]{3,4}$/.test(mode)) {
    ElMessage.error('请输入有效的八进制权限，如 0644')
    return
  }
  // 批量模式
  const batchRows = batchChmodRows.value
  if (batchRows.length > 0) {
    const paths = batchRows.map(r => r.path)
    chmodVisible.value = false
    batchChmodRows.value = []
    ElMessageBox.confirm(`修改 ${paths.length} 个项目的权限，确定继续？`, '确认批量修改权限', {
      type: 'warning'
    }).then(() => {
      paths.forEach(p => send('sftp_chmod', { path: p, mode }))
    }).catch(() => {})
    return
  }
  // 单文件模式
  if (!selectedRow.value) return
  const path = selectedRow.value.path
  chmodVisible.value = false
  ElMessageBox.confirm('修改权限可能影响程序运行或安全，确定继续？', '确认修改权限', {
    type: 'warning'
  }).then(() => {
    send('sftp_chmod', { path, mode })
  }).catch(() => {})
}

function openChown() {
  if (!selectedRow.value) return
  // 预填当前文件的 UID/GID
  chownUid.value = selectedRow.value.uid != null ? String(selectedRow.value.uid) : ''
  chownGid.value = selectedRow.value.gid != null ? String(selectedRow.value.gid) : ''
  chownVisible.value = true
}

function submitChown() {
  if (!selectedRow.value) return
  const uid = parseInt(chownUid.value, 10)
  const gid = parseInt(chownGid.value, 10)
  if (isNaN(uid) || isNaN(gid)) {
    ElMessage.error('请填写有效的 UID 和 GID')
    return
  }
  const path = selectedRow.value.path
  chownVisible.value = false
  ElMessageBox.confirm('修改归属可能影响权限与安全，确定继续？', '确认修改归属', {
    type: 'warning'
  }).then(() => {
    send('sftp_chown', { path, uid, gid })
  }).catch(() => {})
}

function openEditor(row) {
  if (!row || row.directory) return
  editorPath.value = row.path
  editorContent.value = ''
  editorVisible.value = true
  send('sftp_read_text', { path: row.path })
}

function saveEditor() {
  if (!editorPath.value) return
  const path = editorPath.value
  const content = editorContent.value
  ElMessageBox.confirm('确定保存？将覆盖远程文件内容。', '确认保存', {
    type: 'info'
  }).then(() => {
    send('sftp_write_text', { path, content })
  }).catch(() => {})
}

let homeRequested = false
watch(() => props.connected, (v) => {
  if (v) {
    if (!homeRequested) {
      // 首次连接：请求家目录
      homeRequested = true
      send('sftp_home', {})
    } else if (currentPath.value) {
      loadList(currentPath.value)
    }
  }
}, { immediate: true })

onMounted(() => {
  document.addEventListener('click', hideCtxMenu)
})

onBeforeUnmount(() => {
  clearLoadingTimeout()
  document.removeEventListener('click', hideCtxMenu)
})

defineExpose({ loadList })
</script>

<style scoped lang="scss">
.sftp-file-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 6px;
  overflow: hidden;
}
.panel-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sftp-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 6px;
}
.tree-pane {
  width: 190px;
  min-width: 140px;
  min-height: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 4px;
  overflow: auto;
}
.tree-node { font-size: 12px; white-space: nowrap; }
.right-pane {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  padding: 2px 0;
}
.toolbar-breadcrumb {
  flex-shrink: 1;
  min-width: 0;
  .link { cursor: pointer; color: var(--el-color-primary); font-size: 12px; }
}
.path-clickable {
  cursor: text;
  min-width: 80px;
}
.path-input {
  width: 280px;
  flex-shrink: 0;
}
.toolbar-spacer { flex: 1; }
.upload-progress-bar {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  padding: 4px 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  .upload-progress-label { font-size: 12px; margin-right: 8px; white-space: nowrap; }
}
.file-list {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  :deep(.el-table) {
    height: 100%;
    th.el-table__cell {
      padding: 4px 0;
      height: 28px;
      font-size: 12px;
      .cell { padding: 0 8px; line-height: 20px; }
    }
    td.el-table__cell {
      padding: 2px 0;
      .cell { padding: 0 8px; line-height: 22px; font-size: 13px; }
    }
  }
}
.editor-wrap {
  padding: 12px;
  .editor-actions { margin-top: 12px; }
}
.scroll-thin {
  scrollbar-width: thin;
}
</style>

<style lang="scss">
/* context menu -- unscoped so teleport works */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--el-bg-color-overlay, #fff);
  border: 1px solid var(--el-border-color-lighter, #e4e7ed);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
  min-width: 140px;
  font-size: 13px;
}
.ctx-item {
  padding: 6px 16px;
  cursor: pointer;
  white-space: nowrap;
  &:hover {
    background: var(--el-fill-color-light, #f5f7fa);
    color: var(--el-color-primary, #409eff);
  }
}
.ctx-batch-label {
  padding: 4px 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  cursor: default;
  &:hover { background: none; color: var(--el-text-color-secondary, #909399); }
}
.ctx-danger {
  color: var(--el-color-danger, #f56c6c);
  &:hover {
    background: var(--el-color-danger-light-9, #fef0f0);
    color: var(--el-color-danger, #f56c6c);
  }
}
.ctx-divider {
  height: 1px;
  background: var(--el-border-color-lighter, #e4e7ed);
  margin: 4px 0;
}
</style>
