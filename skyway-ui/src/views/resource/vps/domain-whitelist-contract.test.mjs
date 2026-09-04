import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const root = new URL('.', import.meta.url)
const read = (path) => fs.readFileSync(new URL(path, root), 'utf8')
const panel = read('components/ProxyNodePanel.vue')
const list = read('proxyNode/index.vue')
const editor = read('components/DomainWhitelistEditor.vue')
const api = read('../../../api/resource/vps.js')

test('shared editor defaults to unrestricted and exposes five-group shortcut', () => {
  assert.match(editor, /const enabled = ref\(false\)/)
  assert.match(editor, />全部常用</)
  assert.match(editor, /presetKeys\.value = presets\.value\.map/)
  assert.match(editor, /直接 IP 和无法识别域名/)
  assert.match(editor, /getProxyDomainWhitelistPresets/)
})

test('both node management surfaces support editing and batch applying or clearing policy', () => {
  for (const source of [panel, list]) {
    assert.match(source, /<DomainWhitelistEditor v-model="editNodeForm\.domainWhitelist"/)
    assert.match(source, /batchUpdateProxyDomainWhitelist/)
    assert.match(source, /nodeIds: \[\.\.\.selectedIds\.value\]/)
    assert.match(source, /关闭白名单即清除限制/)
    assert.match(source, /batchDomainResults/)
    assert.match(source, /scope\.row\.success \? '成功' : '失败'/)
    assert.match(source, /'不限制'/)
  }
})

test('single and batch creation payloads carry domain whitelist policy', () => {
  assert.match(panel, /domainWhitelist: addForm\.domainWhitelist/)
  assert.match(panel, /domainWhitelist: batchForm\.domainWhitelist/)
  assert.match(panel, /<DomainWhitelistEditor v-model="addForm\.domainWhitelist"/)
  assert.match(panel, /<DomainWhitelistEditor v-model="batchForm\.domainWhitelist"/)
})

test('API client exposes preset, single-update and batch-update endpoints', () => {
  assert.match(api, /proxyNode\/domainWhitelist\/presets/)
  assert.match(api, /proxyNode\/' \+ nodeId \+ '\/domainWhitelist/)
  assert.match(api, /proxyNode\/domainWhitelist\/batch/)
})
