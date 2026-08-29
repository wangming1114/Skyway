import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(__dirname, 'index.vue'), 'utf8')

test('VPS create, edit and clone share the improved responsive form layout', () => {
  assert.match(source, /class="vps-instance-dialog"/)
  assert.match(source, /label-position="top" class="vps-instance-form"/)
  assert.match(source, /width: min\(760px, calc\(100vw - 32px\)\)/)
  assert.match(source, /max-height: calc\(100vh - 180px\)/)
  assert.match(source, /overflow-y: auto/)
  assert.match(source, /@media \(max-width: 992px\)/)
})

test('connection test is presented as a compact helper panel', () => {
  assert.match(source, /class="vps-connection-test"/)
  assert.match(source, /自动识别服务器配置/)
  assert.match(source, /自动回写 CPU、内存、磁盘及系统版本/)
  assert.match(source, />测试连接<\/el-button>/)
  assert.match(source, /background: rgba\(64, 158, 255, 0\.08\)/)
})

test('dialog removes duplicate Element Plus padding and keeps a compact header', () => {
  assert.match(source, /:global\(\.vps-instance-dialog\) \{[\s\S]*?padding: 0;[\s\S]*?background: var\(--el-bg-color-overlay\)/)
  assert.match(source, /\.el-dialog__header\) \{[\s\S]*?padding: 14px 20px 12px/)
  assert.match(source, /\.el-dialog__title\) \{[\s\S]*?font-size: 18px/)
})

test('dialog uses structured sections and a consistent action footer', () => {
  assert.match(source, /vps-form-section-title[\s\S]*基本信息/)
  assert.match(source, /vps-form-section-title[\s\S]*连接信息/)
  assert.match(source, /vps-form-section-title[\s\S]*规格与备注/)
  assert.match(source, /class="vps-instance-footer"[\s\S]*取 消[\s\S]*确 定/)
})
