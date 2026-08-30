import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const vpsList = readFileSync(resolve(__dirname, 'index.vue'), 'utf8')
const vpsDetailNodes = readFileSync(resolve(__dirname, 'components/ProxyNodePanel.vue'), 'utf8')
const proxyList = readFileSync(resolve(__dirname, 'proxyNode/index.vue'), 'utf8')

test('VPS cumulative traffic is left aligned and server sortable', () => {
  assert.match(vpsList, /label="累计流量" align="left" header-align="left" prop="totalTrafficBytes"[\s\S]*?sortable="custom"/)
  assert.match(vpsList, /@sort-change="handleInstanceSortChange"/)
  assert.match(vpsList, /:default-sort="\{ prop: 'totalTrafficBytes', order: 'descending' \}"/)
  assert.match(vpsList, /totalTrafficBytes: 'total_traffic_bytes'/)
  assert.match(vpsList, /orderByColumn: 'total_traffic_bytes',[\s\S]*?isAsc: 'descending'/)
  assert.match(vpsList, /queryParams\.value\.orderByColumn/)
})

for (const [name, source] of [['VPS detail nodes', vpsDetailNodes], ['proxy nodes', proxyList]]) {
  test(`${name} supports paginated server sorting including cumulative traffic`, () => {
    assert.match(source, /@sort-change="handleSortChange"/)
    assert.doesNotMatch(source, /:default-sort="\{ prop: 'totalTrafficBytes', order: 'descending' \}"/)
    assert.match(source, /prop="nodeName"[\s\S]*?sortable="custom"/)
    assert.match(source, /prop="port"[\s\S]*?sortable="custom"/)
    assert.match(source, /prop="expireTime"[\s\S]*?sortable="custom"/)
    assert.match(source, /prop="totalTrafficBytes"[\s\S]*?sortable="custom"/)
    assert.match(source, /totalTrafficBytes: 'total_traffic_bytes'/)
    assert.match(source, /orderByColumn: 'create_time',[\s\S]*?isAsc: 'descending'/)
  })
}
