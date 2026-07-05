import assert from 'node:assert/strict'
import {
  buildClashSubscribeUrl,
  buildQrImageUrl,
  buildShareNodeSummary,
  normalizeShareNode,
  parseVlessUrl,
  safeProxyShareFilename
} from './proxyShare.js'

const vlessUrl = 'vless://c65d5d96-dc47-4868-8d3b-30f3ac44a20d@23.144.68.66:10008?encryption=none&security=reality&flow=xtls-rprx-vision&type=tcp&sni=www.cloudflare.com&pbk=eQzYH9RQzpoOwaumcOdT7SgWqNIqarl8krWbX4o6_Qc&fp=chrome#VLESS-REALITY-23.144.68.66-10008-40-20270317'

const parsed = parseVlessUrl(vlessUrl)
assert.equal(parsed.uuid, 'c65d5d96-dc47-4868-8d3b-30f3ac44a20d')
assert.equal(parsed.host, '23.144.68.66')
assert.equal(parsed.port, '10008')
assert.equal(parsed.name, 'VLESS-REALITY-23.144.68.66-10008-40-20270317')
assert.equal(parsed.params.sni, 'www.cloudflare.com')
assert.equal(parsed.params.pbk, 'eQzYH9RQzpoOwaumcOdT7SgWqNIqarl8krWbX4o6_Qc')
assert.equal(parsed.params.fp, 'chrome')

const clashUrl = buildClashSubscribeUrl(vlessUrl)
assert.equal(
  clashUrl,
  'https://api.wcc.best/sub?target=clash&url=vless%3A%2F%2Fc65d5d96-dc47-4868-8d3b-30f3ac44a20d%4023.144.68.66%3A10008%3Fencryption%3Dnone%26security%3Dreality%26flow%3Dxtls-rprx-vision%26type%3Dtcp%26sni%3Dwww.cloudflare.com%26pbk%3DeQzYH9RQzpoOwaumcOdT7SgWqNIqarl8krWbX4o6_Qc%26fp%3Dchrome%23VLESS-REALITY-23.144.68.66-10008-40-20270317&insert=false&config=https%3A%2F%2Fraw.githubusercontent.com%2FACL4SSR%2FACL4SSR%2Fmaster%2FClash%2Fconfig%2FACL4SSR_Online.ini'
)

const qrUrl = buildQrImageUrl(vlessUrl, 220)
assert.ok(qrUrl.startsWith('https://api.qrserver.com/v1/create-qr-code/?size=220x220&data='))
assert.ok(qrUrl.includes(encodeURIComponent(vlessUrl)))

assert.equal(safeProxyShareFilename('VLESS/REALITY:23.144.68.66?10008', 'png'), 'VLESS-REALITY-23.144.68.66-10008.png')
assert.equal(safeProxyShareFilename('', 'yaml'), 'proxy-share.yaml')

const normalized = normalizeShareNode({
  nodeName: 'VLESS-REALITY-64.83.19.211-10011-82-20260705',
  nodeType: 'VLESS-REALITY',
  address: '64.83.19.211',
  port: 10011,
  expireTime: '2026-07-05 00:00:00',
  status: '0',
  url: vlessUrl
}, new Date('2026-06-30T00:00:00Z'))
assert.equal(normalized.name, 'VLESS-REALITY-64.83.19.211-10011-82-20260705')
assert.equal(normalized.protocol, 'VLESS-REALITY')
assert.equal(normalized.endpoint, '64.83.19.211:10011')
assert.equal(normalized.statusText, '正常')
assert.equal(normalized.remainingDays, 5)
assert.equal(normalized.isExpiringSoon, true)

const summary = buildShareNodeSummary([
  normalized,
  normalizeShareNode({ status: '1', expireTime: '2026-07-20' }, new Date('2026-06-30T00:00:00Z')),
  normalizeShareNode({ status: '0', expireTime: '2026-08-01' }, new Date('2026-06-30T00:00:00Z'))
])
assert.equal(summary.activeCount, 2)
assert.equal(summary.expiringSoonCount, 1)

const clashNode = normalizeShareNode({
  nodeName: 'CLASH-SUB-23.91.12.20-443-20260618',
  nodeType: 'Clash 订阅',
  url: 'https://api.example.com/sub/001',
  expireTime: '2026-06-18',
  status: '0'
}, new Date('2026-06-11T00:00:00Z'))
assert.equal(clashNode.vlessUrl, '')
assert.equal(clashNode.clashUrl, 'https://api.example.com/sub/001')
assert.equal(clashNode.endpoint, 'api.example.com')
assert.equal(clashNode.isExpiringSoon, true)
