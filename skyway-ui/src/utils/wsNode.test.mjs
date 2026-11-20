import test from 'node:test'
import assert from 'node:assert/strict'
import { fixedSubscription, validSubdomain, WS_TLS } from './wsNode.js'
import QRCode from 'qrcode'
import { normalizeShareNode, proxyQrOptions } from './proxyShare.js'

test('a renamed WS node keeps its first-party subscription and exposes the new raw URI fields', () => {
  const token = 'a'.repeat(64)
  const path = '/share/subscription/' + token + '/clash'
  const origin = 'https://skyway.example'
  const previous = globalThis.location
  globalThis.location = new URL(origin)
  try {
    const make = host => normalizeShareNode({
      nodeType: WS_TLS, address: host, port: 443, subscriptionPath: path,
      url: `vless://a7093a96-7401-42af-928b-51ed015db5cc@${host}:443?security=tls&type=ws&host=${host}&sni=${host}&path=%2Fsample`
    })
    const first = make('first.example.test')
    const next = make('next.example.test')
    assert.equal(first.clashUrl, next.clashUrl)
    assert.equal(next.clashUrl, origin + path)
    assert.equal(next.wsParameters.host, 'next.example.test')
    assert.equal(next.wsParameters.path, '/sample')
    assert.equal(next.wsParameters.tls, 'tls')
    assert.notEqual(first.shareUrl, next.shareUrl)
    assert.equal(fixedSubscription(path, origin, '/prod-api'), origin + path)
    assert.equal(fixedSubscription(path, origin, 'https://api.example'), 'https://api.example' + path)
    assert.equal(fixedSubscription('//other.example/' + token, origin), '')
  } finally {
    if (previous === undefined) delete globalThis.location
    else globalThis.location = previous
  }
})

test('custom labels accept one DNS label and reject embedded names and invalid edges', () => {
  for (const label of ['edge-1', 'a', 'a'.repeat(63)]) assert.equal(validSubdomain(label), true)
  for (const label of ['test.example.test', 'a-', '-a', '*.test', 'a'.repeat(64)]) assert.equal(validSubdomain(label), false)
})

test('long WS share links render whole QR modules with a four-module quiet zone', async () => {
  const host = 'a'.repeat(63) + '.example.test'
  const id = 'a7093a96-7401-42af-928b-51ed015db5cc'
  const url = `vless://${id}@${host}:8443?security=tls&type=ws&host=${host}&sni=${host}&path=%2F${id}`
  const options = proxyQrOptions(WS_TLS)
  const png = await QRCode.toBuffer(url, options)
  const size = (QRCode.create(url, options).modules.size + 8) * 6
  assert.equal(png.readUInt32BE(16), size)
  assert.equal(png.readUInt32BE(20), size)
  for (const type of ['VLESS-REALITY', 'Hysteria2', 'VMess-TCP']) {
    assert.deepEqual(proxyQrOptions(type), { width: 220, margin: 1, errorCorrectionLevel: 'M' })
  }
})
