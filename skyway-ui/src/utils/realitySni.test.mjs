import assert from 'node:assert/strict'
import { test } from 'node:test'
import { DEFAULT_REALITY_SNI, isValidRealitySni, readRealitySni } from './realitySni.js'

test('SNI defaults and domain validation', () => {
  assert.equal(DEFAULT_REALITY_SNI, 'www.bing.com')
  for (const value of ['www.bing.com', ' dash.cloudflare.com ', 'xn--bcher-kva.example', 'A-B.example']) {
    assert.equal(isValidRealitySni(value), true, value)
  }
  for (const value of ['', null, 123, 'bing', 'https://www.bing.com', 'bing.com/path', 'bing.com:443',
    '127.0.0.1', '[::1]', '-bad.com', 'bad_.com', 'a..com', 'a b.com', `${'a'.repeat(64)}.com`]) {
    assert.equal(isValidRealitySni(value), false, String(value))
  }
})

test('editing prefers saved SNI, then decodes URL SNI, and never invents a default', () => {
  const url = 'vless://id@example.com:443?sni=dash%2Ecloudflare.com#node'
  assert.equal(readRealitySni({ configJson: '{"sni":" www.bing.com "}', url }), 'www.bing.com')
  assert.equal(readRealitySni({ configJson: '{}', url }), 'dash.cloudflare.com')
  assert.equal(readRealitySni({ configJson: 'broken', url }), 'dash.cloudflare.com')
  assert.equal(readRealitySni({ configJson: '{"sni":""}', url }), 'dash.cloudflare.com')
  assert.equal(readRealitySni({ configJson: '{}', url: 'broken' }), '')
})
