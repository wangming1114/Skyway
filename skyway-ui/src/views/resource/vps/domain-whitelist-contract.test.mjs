import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const root = new URL('.', import.meta.url)
const read = (path) => fs.readFileSync(new URL(path, root), 'utf8')
const panel = read('components/ProxyNodePanel.vue')
const list = read('proxyNode/index.vue')
const editor = read('components/DomainWhitelistEditor.vue')
const api = read('../../../api/resource/vps.js')

test('shared editor exposes mutually-exclusive unrestricted, whitelist and blacklist modes', () => {
  assert.match(editor, /const mode = ref\(''\)/)
  assert.match(editor, /value="whitelist">白名单/)
  assert.match(editor, /value="blacklist">黑名单/)
  assert.match(editor, /function onModeChange\(\)/)
  assert.match(editor, />全部常用</)
  assert.match(editor, /presetKeys\.value = presets\.value\.map/)
  assert.match(editor, /直接 IP.*无法识别域名/)
  assert.match(editor, /getProxyDomainPolicyPresets/)
})

test('both node management surfaces support editing and batch applying or clearing policy', () => {
  for (const source of [panel, list]) {
    assert.match(source, /<DomainWhitelistEditor v-model="editNodeForm\.domainPolicy"/)
    assert.match(source, /batchUpdateProxyDomainPolicy/)
    assert.match(source, /nodeIds: \[\.\.\.selectedIds\.value\]/)
    assert.match(source, /选择“不限制”即清除策略/)
    assert.match(source, /batchDomainResults/)
    assert.match(source, /scope\.row\.success \? '成功' : '失败'/)
    assert.match(source, /'不限制'/)
  }
})

test('single and batch creation payloads carry unified domain policy', () => {
  assert.match(panel, /domainPolicy: addForm\.domainPolicy/)
  assert.match(panel, /domainPolicy: batchForm\.domainPolicy/)
  assert.match(panel, /<DomainWhitelistEditor v-model="addForm\.domainPolicy"/)
  assert.match(panel, /<DomainWhitelistEditor v-model="batchForm\.domainPolicy"/)
})

test('API client exposes new policy endpoints and retains whitelist compatibility endpoints', () => {
  assert.match(api, /proxyNode\/domainPolicy\/presets/)
  assert.match(api, /proxyNode\/' \+ nodeId \+ '\/domainPolicy/)
  assert.match(api, /proxyNode\/domainPolicy\/batch/)
  assert.match(api, /proxyNode\/domainWhitelist\/presets/)
  assert.match(api, /proxyNode\/' \+ nodeId \+ '\/domainWhitelist/)
  assert.match(api, /proxyNode\/domainWhitelist\/batch/)
})
