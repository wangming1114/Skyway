import assert from 'node:assert/strict'
import {
  buildClashSubscribeUrl,
  buildQrImageUrl,
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
