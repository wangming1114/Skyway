import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(__dirname, 'components/ProxyNodePanel.vue'), 'utf8')

test('batch create is exposed in both VPS and customer detail panels', () => {
  assert.match(source, /@click="handleAdd"[\s\S]*?>新增节点<\/el-button>/)
  assert.match(source, /@click="handleBatchAdd"[\s\S]*?>批量新增<\/el-button>/)
  assert.match(source, /v-if="!instanceId" label="服务器" required/)
  assert.match(source, /batchForm\.instanceId/)
  assert.match(source, /props\.fixedCustomer \? props\.customerId : undefined/)
})

test('batch create serially reuses the existing single-node HTTP API', () => {
  const executor = source.match(/async function executeBatchRows\(targetRows\)[\s\S]*?\n}\n\nfunction submitBatchAdd/)?.[0] || ''
  assert.match(executor, /for \(const row of targetRows\)/)
  assert.match(source, /async function resolveAutoBatchPort[\s\S]*?excludePorts[\s\S]*?await getRecommendPort\(batchInstanceId\.value,/)
  assert.match(executor, /await addProxyNodeOnInstance\(batchInstanceId\.value, payload\)/)
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

test('single and batch create default to one month expiry without changing edit echo', () => {
  assert.match(source, /addForm\.expireTime = getDefaultNodeExpireTime\(\)/)
  assert.match(source, /addFormPermanent\.value = false/)
  assert.match(source, /batchForm\.expireTime = getDefaultNodeExpireTime\(\)/)
  assert.match(source, /batchPermanent\.value = false/)
  assert.match(source, /editNodeForm\.expireTime = row\.expireTime \|\| null/)
  assert.match(source, /editNodePermanent\.value = !row\.expireTime/)
})
