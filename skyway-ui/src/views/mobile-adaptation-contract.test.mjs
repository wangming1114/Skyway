import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const root = new URL('.', import.meta.url)
const read = (path) => fs.readFileSync(new URL(path, root), 'utf8')

const listFiles = [
  'index.vue',
  'member/customer/index.vue',
  'resource/vps/index.vue',
  'resource/vps/proxyNode/index.vue',
  'system/user/index.vue',
  'resource/vps/components/ProxyNodePanel.vue'
]

test('shared mobile foundation follows the existing 992px device breakpoint', () => {
  const source = read('../assets/styles/skyway.scss')
  assert.match(source, /@media \(max-width: 992px\)/)
  assert.match(source, /\.mobile-card-list/)
  assert.match(source, /background: var\(--el-bg-color-overlay\)/)
  assert.match(source, /border: 1px solid var\(--el-border-color-lighter\)/)
})

test('target lists derive isMobile from the app store', () => {
  for (const file of listFiles) {
    const source = read(file)
    assert.match(source, /useAppStore/)
    assert.match(source, /appStore\.device === 'mobile'/)
  }
})

test('dashboard renders cards instead of tables on mobile', () => {
  const source = read('index.vue')
  assert.equal((source.match(/<el-table v-if="!isMobile"/g) || []).length, 3)
  assert.equal((source.match(/<div v-else[^>]*mobile-card-list/g) || []).length, 3)
  assert.match(source, /vpsRankList/)
  assert.match(source, /customerNodeRankList/)
  assert.match(source, /expiringNodeList/)
})

test('customer, VPS, proxy node and system user lists use exclusive table/card branches', () => {
  for (const file of ['resource/vps/index.vue', 'resource/vps/proxyNode/index.vue', 'member/customer/index.vue', 'system/user/index.vue']) {
    const source = read(file)
    assert.match(source, /<el-table v-if="!isMobile"/)
    assert.match(source, /v-else[^>]*mobile-card-list/)
    assert.match(source, /el-drawer/)
  }
})

test('shared ProxyNodePanel exposes mobile cards and direct VPS detail actions', () => {
  const panel = read('resource/vps/components/ProxyNodePanel.vue')
  const detail = read('resource/vps/detail.vue')
  const customerDetail = read('member/customer/detail.vue')

  assert.match(panel, /<el-table v-if="!isMobile"/)
  assert.match(panel, /<div v-else[^>]*mobile-card-list/)
  assert.match(panel, />新增节点</)
  assert.match(panel, />批量新增</)
  assert.match(panel, />批量删除</)
  assert.match(panel, /queryParams\.nodeType/)
  assert.match(panel, /queryParams\.expireStatus/)
  assert.match(detail, /<ProxyNodePanel/)
  assert.match(customerDetail, /<ProxyNodePanel/)
})

test('proxy node cards preserve selection, batch deletion and all primary operations', () => {
  for (const file of ['resource/vps/components/ProxyNodePanel.vue', 'resource/vps/proxyNode/index.vue']) {
    const source = read(file)
    assert.match(source, /<el-checkbox/)
    assert.match(source, /toggleNodeSelection/)
    assert.match(source, /selectedIds/)
    assert.match(source, /handleBatchDelete/)
    for (const label of ['详情', '订阅信息', '复制链接', '访问日志', '编辑', '设置限速', '删除']) {
      assert.ok(source.includes(label), `${file} should retain ${label}`)
    }
  }
})

test('node and list dialogs use viewport-constrained mobile widths', () => {
  for (const file of ['resource/vps/components/ProxyNodePanel.vue', 'resource/vps/proxyNode/index.vue', 'resource/vps/index.vue', 'member/customer/index.vue', 'system/user/index.vue']) {
    assert.match(read(file), /calc\(100vw - 20px\)/)
  }
  const panel = read('resource/vps/components/ProxyNodePanel.vue')
  assert.match(panel, /batch-common-grid[\s\S]*grid-template-columns: 1fr/)
  assert.match(panel, /batch-mobile-list/)
  assert.match(panel, /max-height: calc\(100dvh - 144px\)/)
})

test('details and terminal retain narrow layouts and terminal resize fitting', () => {
  assert.match(read('resource/vps/detail.vue'), /:column="isMobile \? 1 : 2"/)
  assert.match(read('member/customer/detail.vue'), /:column="isMobile \? 1 : 2"/)
  assert.match(read('resource/vps/terminal/index.vue'), /@media \(max-width: 992px\)/)

  const terminalPanel = read('resource/vps/components/TerminalPanel.vue')
  assert.match(terminalPanel, /ResizeObserver\(scheduleFit\)/)
  assert.match(terminalPanel, /window\.addEventListener\('resize', scheduleFit/)
  assert.match(terminalPanel, /window\.removeEventListener\('resize', scheduleFit/)
})
