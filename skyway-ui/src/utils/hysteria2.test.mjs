import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import {
  parseProxyShareUrl, normalizeShareNode, buildClashSubscribeUrl, supportsSocks5Relay,
  HYSTERIA2_SUBCONVERTER_URL, HYSTERIA2_CLASH_CONFIG_URL
} from './proxyShare.js'

const pin = 'AB'.repeat(32)
const raw = `hysteria2://p%40ss%3Aword%2B%20%23%26@[2001:db8::1]:32332?alpn=h3&insecure=1&allowInsecure=1&pinSHA256=${pin}#%E6%B5%8B%E8%AF%95%20%E8%8A%82%E7%82%B9`

test('Hysteria2 parses encoded credentials, names and IPv6 without SNI', () => {
  for (const value of [raw, raw.replace('hysteria2:', 'hy2:')]) {
    const node = parseProxyShareUrl(value)
    assert.equal(node.password, 'p@ss:word+ #&')
    assert.equal(node.host, '2001:db8::1')
    assert.equal(node.port, '32332')
    assert.equal(node.name, '测试 节点')
    assert.equal(node.params.pinSHA256, pin)
    assert.equal(node.params.sni, undefined)
  }
})

test('Hysteria2 conversion URL exactly preserves the supplied conversion settings', () => {
  const url = new URL(buildClashSubscribeUrl(raw))
  assert.equal(url.origin + url.pathname, HYSTERIA2_SUBCONVERTER_URL)
  assert.deepEqual(Object.fromEntries(url.searchParams), {
    target: 'clash', url: raw, config: HYSTERIA2_CLASH_CONFIG_URL,
    insert: 'false', emoji: 'true', tfo: 'false', scv: 'true', fdn: 'false',
    expand: 'false', sort: 'false', append_type: 'false', new_name: 'true'
  })
})

test('customer share normalization exposes Hysteria2 raw link and conversion for copy and QR', () => {
  const node = normalizeShareNode({ url: raw })
  assert.equal(node.name, '测试 节点')
  assert.equal(node.protocol, 'Hysteria2')
  assert.equal(node.shareProtocol, 'Hysteria2')
  assert.equal(node.shareUrl, raw)
  assert.equal(node.vlessUrl, '')
  assert.equal(new URL(node.clashUrl).searchParams.get('url'), raw)
  assert.equal(supportsSocks5Relay('Hysteria2'), true)
  assert.equal(supportsSocks5Relay('VMess-TCP'), false)
})

test('all admin forms offer Hysteria2 and separate relay from REALITY SNI', () => {
  for (const file of ['../views/resource/vps/components/ProxyNodePanel.vue', '../views/resource/vps/proxyNode/index.vue']) {
    const source = readFileSync(new URL(file, import.meta.url), 'utf8')
    assert.match(source, /value: 'Hysteria2', label: 'Hysteria2'/)
    assert.match(source, /canEditRelay = computed\(\(\) => supportsSocks5Relay/)
    assert.match(source, /canEditSni = computed\(\(\) => editNodeRow.value\?\.nodeType === 'VLESS-REALITY'/)
    assert.match(source, /v-if="canEditSni" label="SNI"/)
    assert.doesNotMatch(source, /v-if="canEditRelay" label="SNI"/)
  }
})
