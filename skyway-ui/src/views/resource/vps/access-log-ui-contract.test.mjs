import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const vpsList = readFileSync(resolve(here, 'index.vue'), 'utf8')
const nodeList = readFileSync(resolve(here, 'proxyNode/index.vue'), 'utf8')
const nodePanel = readFileSync(resolve(here, 'components/ProxyNodePanel.vue'), 'utf8')
const dialog = readFileSync(resolve(here, 'components/AccessLogDialog.vue'), 'utf8')

for (const [name, source] of [['VPS list', vpsList], ['node list', nodeList], ['VPS node panel', nodePanel]]) {
  const firstTable = source.slice(0, source.indexOf('</el-table>'))
  assert.match(firstTable, /el-dropdown-item command="accessLog"[^>]*>访问日志</)
  assert.doesNotMatch(firstTable, /el-button[^>]*@click="open(?:Vps|Node)AccessLog/)
  console.log(`✔ ${name} keeps access logs in the More menu`)
}

assert.match(dialog, /row\.customerName/)
assert.match(dialog, /#\$\{row\.customerId\}/)
assert.match(dialog, /row\.nodeName/)
assert.match(dialog, /row\.inboundTag/)
console.log('✔ access-log node column prioritizes customer name and keeps node identifiers')
