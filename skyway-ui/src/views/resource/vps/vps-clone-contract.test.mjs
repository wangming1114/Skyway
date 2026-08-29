import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(__dirname, 'index.vue'), 'utf8')

test('VPS list exposes clone with add permission', () => {
  assert.match(source, /command="clone"[^>]*v-hasPermi="\['resource:vps:add'\]"[^>]*>克隆<\/el-dropdown-item>/)
  assert.match(source, /handleInstanceCommand\(command, row\)[\s\S]*command === 'clone'\) handleCloneInstance\(row\)/)
})

test('clone loads full details and reuses the existing create flow', () => {
  const cloneHandler = source.match(/function handleCloneInstance\(row\)[\s\S]*?\n}/)?.[0] || ''
  assert.match(cloneHandler, /getInstance\(row\.id\)/)
  assert.match(cloneHandler, /buildInstanceForm\(res\.data, true\)/)
  assert.match(cloneHandler, /instanceTitle\.value = '克隆VPS'/)
  assert.match(source, /id: clone \? undefined : source\.id/)
  assert.match(source, /name: clone \? `\$\{sourceName} - 副本`/)
  assert.match(source, /if \(payload\.id\)[\s\S]*?updateInstance\(payload\)[\s\S]*?else \{[\s\S]*?addInstance\(payload\)/)
})

test('clone form only maps editable fields instead of spreading detail-only data', () => {
  const mapper = source.match(/function buildInstanceForm\(data, clone = false\)[\s\S]*?\n}/)?.[0] || ''
  assert.doesNotMatch(mapper, /\.\.\.source/)
  assert.match(mapper, /categoryId: source\.categoryId/)
  assert.match(mapper, /sshPassword: source\.sshPassword/)
  assert.match(mapper, /trafficLimitGb:/)
  assert.match(mapper, /expireTime: source\.expireTime/)
})
