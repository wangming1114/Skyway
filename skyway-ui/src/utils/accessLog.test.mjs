import assert from 'node:assert/strict'
import { ACCESS_LOG_MAX_ENTRIES, appendAccessLogEntries, buildAccessLogWsUrl } from './accessLog.js'

const locationLike = { protocol: 'https:', host: 'skyway.example.com' }

const vpsUrl = buildAccessLogWsUrl({
  scope: 'vps',
  instanceId: 42,
  token: 'token with spaces',
  locationLike,
  baseApi: '/prod-api'
})
assert.equal(vpsUrl, 'wss://skyway.example.com/prod-api/ws/access-log?scope=vps&token=token+with+spaces&instanceId=42')

const nodeUrl = buildAccessLogWsUrl({
  scope: 'node',
  nodeId: 7,
  token: 'abc',
  locationLike: { protocol: 'http:', host: 'localhost:80' }
})
assert.equal(nodeUrl, 'ws://localhost:80/ws/access-log?scope=node&token=abc&nodeId=7')

const oversized = Array.from({ length: ACCESS_LOG_MAX_ENTRIES + 5 }, (_, id) => ({ id }))
const bounded = appendAccessLogEntries([], oversized)
assert.equal(bounded.length, ACCESS_LOG_MAX_ENTRIES)
assert.equal(bounded[0].id, 5)

assert.throws(() => buildAccessLogWsUrl({ scope: 'node', token: 'x', locationLike }), /nodeId/)
console.log('accessLog utility contract tests passed')
