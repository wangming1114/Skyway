import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(__dirname, 'components/ProxyNodePanel.vue'), 'utf8')

test('batch create is only exposed for a fixed VPS instance and keeps single create', () => {
  assert.match(source, /@click="handleAdd"[\s\S]*?>新增节点<\/el-button>/)
  assert.match(source, /v-if="instanceId"[\s\S]*?@click="handleBatchAdd"[\s\S]*?>批量新增<\/el-button>/)
})

test('batch create serially reuses the existing single-node HTTP API', () => {
  const executor = source.match(/async function executeBatchRows\(targetRows\)[\s\S]*?\n}\n\nfunction submitBatchAdd/)?.[0] || ''
  assert.match(executor, /for \(const row of targetRows\)/)
  assert.match(source, /async function resolveAutoBatchPort[\s\S]*?excludePorts[\s\S]*?await getRecommendPort\(props\.instanceId,/)
  assert.match(executor, /await addProxyNodeOnInstance\(props\.instanceId, payload\)/)
  assert.match(executor, /autoPort: row\.autoPort/)
  assert.match(executor, /res\?\.data\?\.port[\s\S]*?row\.port = Number\(res\.data\.port\)/)
  assert.match(executor, /row\.status = 'failed'/)
  assert.doesNotMatch(source, /batchProxyNode|proxyNode\/batch/)
})

test('batch rows support optional per-node relays and failed-only retry', () => {
  assert.match(source, /if \(relayText\) payload\.relayText = relayText/)
  assert.match(source, /parseSocks5RelayLines\(batchForm\.relayPaste\)/)
  assert.match(source, /filter\(row => row\.status === 'failed'\)/)
})

test('single create tracks automatic ports and visibly warns for unverified fallback', () => {
  assert.match(source, /addPortAuto\.value = true/)
  assert.match(source, /res\?\.verified !== false/)
  assert.match(source, /服务器未验证：/)
  assert.match(source, /autoPort: addPortAuto\.value/)
  assert.match(source, /@change="onSingleAddPortChange"/)
})
