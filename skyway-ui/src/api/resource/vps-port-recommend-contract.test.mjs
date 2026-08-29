import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(__dirname, 'vps.js'), 'utf8')

test('recommend port API remains GET-compatible and accepts optional exclusions', () => {
  const method = source.match(/export function getRecommendPort[\s\S]*?\n}\n/)?.[0] || ''
  assert.match(method, /getRecommendPort\(instanceId, params\)/)
  assert.match(method, /method: 'get'/)
  assert.match(method, /params/)
})
