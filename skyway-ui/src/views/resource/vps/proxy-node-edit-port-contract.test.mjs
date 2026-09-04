import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

const editSurfaces = [
  {
    name: 'VPS detail proxy node panel',
    path: resolve(__dirname, 'components/ProxyNodePanel.vue')
  },
  {
    name: 'proxy node list page',
    path: resolve(__dirname, 'proxyNode/index.vue')
  }
]

function readSurface(surface) {
  return readFileSync(surface.path, 'utf8')
}

for (const surface of editSurfaces) {
  test(`${surface.name} edit dialog supports port editing`, () => {
    const source = readSurface(surface)
    const editDialog = source.match(/<el-dialog title="编辑节点"[\s\S]*?<\/el-dialog>/)?.[0] || ''

    assert.match(editDialog, /<el-form-item label="端口"[\s\S]*v-model="editNodeForm\.port"/)
    assert.match(source, /const editNodeForm = reactive\(\{[\s\S]*port:/)
    assert.match(source, /editNodeForm\.port\s*=\s*row\.port/)
    assert.match(source, /port:\s*editNodeForm\.port/)
    assert.match(source, /function syncUpdatedNodeName\(row, updatedNode\)/)
    assert.match(source, /syncUpdatedNodeName\(row, res\?\.data\)/)
    assert.match(source, /updatedNode\.port != null[\s\S]*row\.port = updatedNode\.port/)
    assert.match(source, /hasOwnProperty\.call\(updatedNode, 'url'\)[\s\S]*row\.url = updatedNode\.url/)
  })
}
