import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { PNG } from 'pngjs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const assetRoot = resolve(__dirname, '../../../assets/share/customer')

const transparentAssets = [
  'hero-overview.png',
  'desktop-icon-recommend.png',
  'desktop-icon-protocol.png',
  'desktop-icon-platform.png',
  'desktop-icon-help.png',
  'desktop-icon-contact.png',
  'icon-back-arrow.png',
  'icon-copy.png',
  'icon-download-arrow.png',
  'icon-download-tray.png',
  'icon-external-link.png',
  'tag-windows.png',
  'tag-macos.png',
  'step-blue-1.png',
  'step-blue-2.png',
  'step-purple-3.png',
  'step-purple-4.png',
  'icon-active-subscription.png',
  'icon-v2rayn.png',
  'icon-clash-verge.png',
  'icon-question.png',
  'icon-platform.png',
  'icon-download.png',
  'icon-switch.png',
  'status-normal.png'
]

const splitSpriteAssets = [
  'button-view-detail.png',
  'button-view-guide.png',
  'button-download-now.png',
  'button-go-download.png',
  'button-more.png',
  'tag-star.png',
  'tag-primary-node.png',
  'tag-subscribe-node.png',
  'tag-backup-node.png',
  'tag-clash-subscription.png'
]

const realClientIconAssets = [
  'icon-v2rayng.png',
  'icon-shadowrocket.png'
]

const desktopClientIconAssets = [
  'icon-v2rayn.png',
  'icon-clash-verge.png'
]

const transparentPlateFreeClientAssets = [
  ...realClientIconAssets
]

function readPng(name) {
  const file = resolve(assetRoot, name)
  assert.ok(existsSync(file), `${name} should exist`)
  return PNG.sync.read(readFileSync(file))
}

function hasTransparentPixels(png) {
  for (let i = 3; i < png.data.length; i += 4) {
    if (png.data[i] < 250) return true
  }
  return false
}

function alphaAt(png, x, y) {
  return png.data[(y * png.width + x) * 4 + 3]
}

test('processed share customer assets have transparency', () => {
  for (const name of transparentAssets) {
    const png = readPng(name)
    assert.ok(hasTransparentPixels(png), `${name} should include an alpha mask`)
  }
})

test('compound sprite is split into individual transparent assets', () => {
  for (const name of splitSpriteAssets) {
    const png = readPng(name)
    assert.ok(png.width > 10, `${name} should have a usable width`)
    assert.ok(png.height > 10, `${name} should have a usable height`)
    assert.ok(hasTransparentPixels(png), `${name} should not keep the checkerboard background`)
  }
})

test('mobile client icons use real app artwork assets', () => {
  for (const name of realClientIconAssets) {
    const png = readPng(name)
    assert.ok(png.width >= 100, `${name} should be large enough for high-density displays`)
    assert.ok(png.height >= 100, `${name} should be large enough for high-density displays`)
  }
})

test('mobile client icons do not keep app-icon border plates', () => {
  for (const name of transparentPlateFreeClientAssets) {
    const png = readPng(name)
    assert.ok(alphaAt(png, Math.round(png.width * 0.12), Math.round(png.height * 0.5)) < 20, `${name} should remove the left border plate`)
    assert.ok(alphaAt(png, Math.round(png.width * 0.88), Math.round(png.height * 0.5)) < 20, `${name} should remove the right border plate`)
  }
})

test('desktop client icons use official app artwork assets', () => {
  const [v2rayn, clash] = desktopClientIconAssets.map(readPng)
  assert.equal(v2rayn.width, 256)
  assert.equal(v2rayn.height, 256)
  assert.equal(clash.width, 512)
  assert.equal(clash.height, 512)
  assert.ok(hasTransparentPixels(v2rayn), 'v2rayN official icon should keep transparent corners')
  assert.ok(hasTransparentPixels(clash), 'Clash Verge Rev official icon should keep transparent corners')
})
