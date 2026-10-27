import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import { parse, babelParse } from '@vue/compiler-sfc'
import { parseProxyShareUrl } from '../../../utils/proxyShare.js'
import { DEFAULT_REALITY_SNI, isValidRealitySni, readRealitySni } from '../../../utils/realitySni.js'

function loadFormFunctions(path, names, bindings) {
  const { descriptor } = parse(readFileSync(new URL(path, import.meta.url), 'utf8'))
  const source = descriptor.scriptSetup.content
  const ast = babelParse(source, { sourceType: 'module' })
  const functions = names.map(name => {
    const declaration = ast.program.body.find(node => node.type === 'FunctionDeclaration' && node.id.name === name)
    assert.ok(declaration, name)
    return source.slice(declaration.start, declaration.end)
  })
  const context = vm.createContext(bindings)
  vm.runInContext(functions.join('\n'), context)
  return context
}

for (const path of ['./components/ProxyNodePanel.vue', './proxyNode/index.vue']) {
  test(`${path} echoes SNI, rejects invalid input and refreshes saved config and share link`, async () => {
    const sent = []
    const warnings = []
    let qrRefreshes = 0
    const row = { id: 1, nodeType: 'VLESS-REALITY', port: 443, configJson: '{"sni":"old.example"}', url: 'vless://id@server:443?sni=url.example' }
    const state = {
      readRealitySni, isValidRealitySni, supportsSocks5Relay: type => ['VLESS-REALITY', 'Hysteria2'].includes(type),
      editNodeRow: { value: null }, editNodeForm: {}, editNodePermanent: {},
      editOriginalDomainPolicy: {}, editNodeVisible: {}, editNodeSaving: {},
      detailData: { value: { ...row } }, shareData: { value: { ...row } }, canEditRelay: { value: true }, canEditSni: { value: true },
      shareVisible: { value: true }, refreshShareQrCode() { qrRefreshes++ },
      cloneDomainPolicy: () => null, resetEditRelayForm() { state.editNodeForm.enableRelay = false },
      relayTextFromRow: () => '', domainPolicyValid: () => true, domainPolicyChanged: () => false,
      proxy: { $modal: { msgWarning: msg => warnings.push(msg), msgSuccess() {} } },
      getList() {}, updateProxyNode(payload) {
        sent.push(payload)
        return Promise.resolve({ data: { configJson: '{"sni":"www.bing.com"}', url: 'vless://id@server:443?sni=www.bing.com' } })
      }
    }
    const form = loadFormFunctions(path, ['openNodeEdit', 'submitNodeEdit', 'syncUpdatedNodeName'], state)
    form.openNodeEdit(row)
    assert.equal(state.editNodeForm.sni, 'old.example')
    form.openNodeEdit({ ...row, configJson: '{}' })
    assert.equal(state.editNodeForm.sni, 'url.example')
    form.openNodeEdit(row)
    state.editNodeForm.sni = 'https://bing.com'
    form.submitNodeEdit()
    assert.equal(sent.length, 0)
    assert.equal(warnings.length, 1)
    state.editNodeForm.sni = ' www.bing.com '
    form.submitNodeEdit()
    await new Promise(resolve => setImmediate(resolve))
    assert.equal(sent[0].sni, 'www.bing.com')
    for (const updated of [row, state.detailData.value, state.shareData.value]) {
      assert.equal(JSON.parse(updated.configJson).sni, 'www.bing.com')
      assert.equal(new URL(updated.url).searchParams.get('sni'), 'www.bing.com')
    }
    assert.equal(state.editNodeVisible.value, false)
    assert.equal(qrRefreshes, 1)
  })

  test(`${path} late QR results never overwrite the current share link`, async () => {
    const pending = []
    const shareVlessUrl = { value: 'vless://id@server:443?sni=old.example' }
    const shareQrDataUrl = { value: 'previous QR' }
    const form = loadFormFunctions(path, ['refreshShareQrCode'], {
      shareVlessUrl, shareQrDataUrl,
      QRCode: { toDataURL(url) { return new Promise(resolve => pending.push({ url, resolve })) } },
      proxy: { $modal: { msgWarning() {} } }
    })
    form.refreshShareQrCode()
    assert.equal(shareQrDataUrl.value, '')
    shareVlessUrl.value = 'vless://id@server:443?sni=www.bing.com'
    form.refreshShareQrCode()
    assert.equal(pending[1].url, shareVlessUrl.value)
    pending[1].resolve('new QR')
    await new Promise(resolve => setImmediate(resolve))
    assert.equal(shareQrDataUrl.value, 'new QR')
    pending[0].resolve('stale QR')
    await new Promise(resolve => setImmediate(resolve))
    assert.equal(shareQrDataUrl.value, 'new QR')
  })

  test(`${path} other protocols omit SNI and do not require it`, async () => {
    let sent
    const form = loadFormFunctions(path, ['submitNodeEdit', 'syncUpdatedNodeName'], {
      editNodeRow: { value: { id: 1 } }, editNodeForm: {}, editNodePermanent: {},
      editOriginalDomainPolicy: {}, editNodeVisible: {}, editNodeSaving: {},
      canEditRelay: { value: false }, canEditSni: { value: false }, detailData: {}, shareData: {},
      domainPolicyValid: () => true, domainPolicyChanged: () => false,
      updateProxyNode(payload) { sent = payload; return Promise.resolve({}) },
      proxy: { $modal: { msgSuccess() {} } }, getList() {}
    })
    form.submitNodeEdit()
    await new Promise(resolve => setImmediate(resolve))
    assert.equal(Object.hasOwn(sent, 'sni'), false)
  })
}

test('single creation includes trimmed SNI independently of SOCKS relay', () => {
  const state = { addForm: { sni: ` ${DEFAULT_REALITY_SNI} `, enableRelay: false }, canConfigureSni: { value: true } }
  const form = loadFormFunctions('./components/ProxyNodePanel.vue', ['addRelayPayload'], state)
  assert.equal(form.addRelayPayload({ type: 'add_proxy_node' }).sni, 'www.bing.com')
  assert.equal(form.addRelayPayload({ customerId: 1 }).sni, 'www.bing.com')
  state.canConfigureSni.value = false
  assert.equal(Object.hasOwn(form.addRelayPayload({ customerId: 1 }), 'sni'), false)
})

for (const path of ['./components/ProxyNodePanel.vue', './proxyNode/index.vue']) {
  test(`${path} Hysteria2 editing supports relay without validating or submitting SNI`, async () => {
    let sent
    const form = loadFormFunctions(path, ['submitNodeEdit', 'syncUpdatedNodeName'], {
      editNodeRow: { value: { id: 1, nodeType: 'Hysteria2', port: 32332 } },
      editNodeForm: { port: 32332, sni: 'invalid SNI', enableRelay: false }, editNodePermanent: { value: true },
      editOriginalDomainPolicy: {}, editNodeVisible: {}, editNodeSaving: {},
      canEditRelay: { value: true }, canEditSni: { value: false }, detailData: {}, shareData: {},
      domainPolicyValid: () => true, domainPolicyChanged: () => false,
      updateProxyNode(payload) { sent = payload; return Promise.resolve({}) },
      proxy: { $modal: { msgSuccess() {} } }, getList() {}
    })
    form.submitNodeEdit()
    await new Promise(resolve => setImmediate(resolve))
    assert.equal(sent.port, 32332)
    assert.equal(Object.hasOwn(sent, 'sni'), false)
  })
}

test('Hysteria2 creation sends SOCKS relay without a leftover REALITY SNI', () => {
  const form = loadFormFunctions('./components/ProxyNodePanel.vue', ['addRelayPayload'], {
    addForm: { sni: 'old.example', enableRelay: true, relayText: ' host:1080:user:password ' },
    canConfigureSni: { value: false }, canConfigureRelay: { value: true }
  })
  const payload = form.addRelayPayload({ nodeType: 'Hysteria2' })
  assert.equal(payload.relayText, 'host:1080:user:password')
  assert.equal(Object.hasOwn(payload, 'sni'), false)
})

for (const path of ['./components/ProxyNodePanel.vue', './proxyNode/index.vue']) {
  test(`${path} opens Hysteria2 sharing and generates its QR`, () => {
    let refreshed = 0
    const state = {
      parseProxyShareUrl, shareData: {}, shareVisible: {},
      refreshShareQrCode() { refreshed++ },
      proxy: { $modal: { msgWarning(message) { throw new Error(message) } } }
    }
    const form = loadFormFunctions(path, ['handleShare'], state)
    const row = { nodeType: 'Hysteria2', url: 'hysteria2://pw@203.0.113.8:32332?alpn=h3&insecure=1' }
    form.handleShare(row)
    assert.equal(state.shareData.value.url, row.url)
    assert.equal(state.shareVisible.value, true)
    assert.equal(refreshed, 1)
  })
}
