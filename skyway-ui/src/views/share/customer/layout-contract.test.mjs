import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

function readScreen(name) {
  return readFileSync(resolve(__dirname, 'screens', name), 'utf8')
}

function readShareIndex() {
  return readFileSync(resolve(__dirname, 'index.vue'), 'utf8')
}

function readComponent(name) {
  return readFileSync(resolve(__dirname, 'components', name), 'utf8')
}

function readStyle() {
  return readFileSync(resolve(__dirname, 'style.scss'), 'utf8')
}

function collectShareFiles(dir = __dirname) {
  return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const path = resolve(dir, entry.name)
    if (entry.isDirectory()) return collectShareFiles(path)
    return /\.(vue|scss)$/.test(entry.name) ? [path] : []
  })
}

function cssBlock(source, selector) {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = source.match(new RegExp(`${escaped}\\s*\\{([\\s\\S]*?)\\n\\}`, 'm'))
  assert.ok(match, `missing CSS block for ${selector}`)
  return match[1]
}

function pxValue(block, property) {
  const escaped = property.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = block.match(new RegExp(`${escaped}:\\s*(\\d+)px`))
  assert.ok(match, `missing px value for ${property}`)
  return Number(match[1])
}

test('guide directory exposes a return-home navigation action', () => {
  const source = readScreen('GuideDirectoryScreen.vue')
  const index = readShareIndex()
  assert.match(source, /@click="\$emit\('navigate', 'overview'\)"/)
  assert.match(source, /defineEmits\(\[[^\]]*'navigate'/s)
  assert.match(index, /<GuideDirectoryScreen[\s\S]*@navigate="showView"/)
})

test('overview keeps subscriptions and quick guide aligned with downloads below', () => {
  const source = readScreen('OverviewScreen.vue')
  assert.match(source, /class="overview-side-stack"/)
  assert.doesNotMatch(source, /class="overview-bottom-grid"/)
  assert.doesNotMatch(source, /overview-faq-panel/)
  assert.doesNotMatch(source, /常见问题 \/ 温馨提示/)
  const sideStackIndex = source.indexOf('class="overview-side-stack"')
  const downloadPanelIndex = source.indexOf('download-overview-panel')
  assert.ok(
    sideStackIndex >= 0 && downloadPanelIndex >= 0 && sideStackIndex < downloadPanelIndex,
    'side stack should render before the full-width download panel'
  )
  assert.match(source, /class="overview-side-stack"[\s\S]*class="[^"]*quick-guide-panel[^"]*"/)
})

test('overview visual density matches compact prototype proportions', () => {
  const style = readStyle()
  assert.ok(pxValue(cssBlock(style, '.stat-card'), 'height') <= 116)
  assert.ok(pxValue(cssBlock(style, '.stat-card'), 'padding') <= 18)
  assert.ok(pxValue(cssBlock(style, '.share-tr'), 'min-height') <= 54)
  assert.ok(pxValue(cssBlock(style, '.share-th'), 'min-height') <= 42)
  assert.ok(pxValue(cssBlock(style, '.table-tools button,\n.table-tools label,\n.table-actions button'), 'height') <= 34)
  assert.ok(pxValue(cssBlock(style, '.status-img'), 'height') <= 24)
  assert.ok(pxValue(cssBlock(style, '.quick-item'), 'height') <= 52)
  assert.ok(pxValue(cssBlock(style, '.mini-client'), 'min-height') <= 96)
  assert.match(cssBlock(style, '.quick-guide-panel'), /flex:\s*0 0 auto/)
  assert.match(cssBlock(style, '.overview-side-stack'), /padding:\s*0/)
  assert.ok(pxValue(cssBlock(style, '.quick-group'), 'padding') <= 2)
  assert.match(cssBlock(style, '.quick-group'), /border:\s*0/)
  assert.match(cssBlock(style, '.quick-group'), /background:\s*transparent/)
  assert.ok(pxValue(cssBlock(style, '.quick-group + .quick-group'), 'margin-top') >= 10)
  assert.ok(pxValue(cssBlock(style, '.quick-group + .quick-group'), 'margin-top') <= 12)
  assert.ok(pxValue(cssBlock(style, '.quick-item'), 'height') >= 48)
  assert.ok(pxValue(cssBlock(style, '.quick-item + .quick-item'), 'margin-top') >= 8)
})

test('overview subscription table removes protocol and node role badges', () => {
  const source = readScreen('OverviewScreen.vue')
  const style = readStyle()

  assert.doesNotMatch(source, /<span>协议<\/span>/)
  assert.doesNotMatch(source, /protocol-chip/)
  assert.doesNotMatch(source, /node-tag-img/)
  assert.doesNotMatch(source, /getNodeTag/)
  assert.doesNotMatch(source, /tagPrimaryNode|tagBackupNode|tagSubscribeNode/)
  assert.doesNotMatch(style, /\.protocol-chip/)
  assert.doesNotMatch(style, /\.node-tag-img/)
  assert.match(cssBlock(style, '.share-tr'), /grid-template-columns:\s*minmax\(0,\s*2\.6fr\) minmax\(150px,\s*1\.5fr\) 1fr 0\.8fr 1\.05fr/)
})

test('overview operation column puts copy subscription before detail', () => {
  const source = readScreen('OverviewScreen.vue')
  const index = readShareIndex()
  const style = readStyle()
  const overviewBinding = index.match(/<OverviewScreen[\s\S]*?\/>/)?.[0] || ''
  const copyButton = source.match(/<button type="button" class="asset-action copy-subscription"[\s\S]*?<\/button>/)?.[0] || ''
  const detailButton = source.match(/<button type="button" class="asset-action detail"[\s\S]*?<\/button>/)?.[0] || ''

  assert.match(source, /defineEmits\(\[[^\]]*'copy-subscription'/s)
  assert.match(overviewBinding, /@copy-subscription="copyNodeClash"/)
  assert.doesNotMatch(overviewBinding, /@copy-vless="copyNodeVless"/)
  assert.match(
    source,
    /class="table-actions"[\s\S]*aria-label="复制订阅"[\s\S]*\$emit\('copy-subscription', node\)[\s\S]*aria-label="查看详情"[\s\S]*\$emit\('detail', node\)/
  )
  assert.doesNotMatch(source, /aria-label="复制 VLESS"/)
  assert.doesNotMatch(copyButton, /<img/)
  assert.doesNotMatch(detailButton, /<img/)
  assert.match(cssBlock(style, '.table-actions .asset-action'), /width:\s*70px/)
  assert.match(cssBlock(style, '.table-actions .asset-action'), /height:\s*31px/)
})

test('overview subscription list supports customer status filtering and keyword search together', () => {
  const source = readScreen('OverviewScreen.vue')
  const index = readShareIndex()
  const style = readStyle()
  const overviewBinding = index.match(/<OverviewScreen[\s\S]*?\/>/)?.[0] || ''

  assert.match(overviewBinding, /v-model:status-filter="statusFilter"/)
  assert.match(index, /const statusFilter = ref\('all'\)/)
  assert.match(index, /filterShareNodes\(normalizedNodes\.value,\s*\{[\s\S]*keyword:\s*keyword\.value,[\s\S]*status:\s*statusFilter\.value/)
  assert.match(source, /<select :value="statusFilter" @change="\$emit\('update:statusFilter', \$event\.target\.value\)">/)
  assert.match(source, /<option value="all">全部状态<\/option>/)
  assert.match(source, /<option value="active">正常<\/option>/)
  assert.match(source, /<option value="expired">已过期<\/option>/)
  assert.match(source, /<option value="disabled">停用<\/option>/)
  assert.match(source, /defineEmits\(\[[^\]]*'update:statusFilter'/s)
  assert.match(cssBlock(style, '.table-tools .status-filter'), /width:\s*128px/)
  assert.match(cssBlock(style, '.status-chip.danger'), /#d93025/)
})

test('overview quick guide group headings are text-only', () => {
  const source = readScreen('OverviewScreen.vue')

  assert.match(source, /<div class="quick-group desktop">\s*<h3>桌面端导入<\/h3>/)
  assert.match(source, /<div class="quick-group mobile">\s*<h3>手机端导入<\/h3>/)
  assert.doesNotMatch(source, /<h3><span class="title-icon (desktop|mobile)"><\/span>(桌面端|手机端)导入<\/h3>/)
})

test('large summary icons use one unified component style', () => {
  const overview = readScreen('OverviewScreen.vue')
  const desktop = readScreen('DesktopGuideScreen.vue')
  const detail = readScreen('NodeDetailScreen.vue')
  const style = readStyle()

  assert.equal(overview.match(/class="summary-icon/g)?.length, 4)
  assert.doesNotMatch(overview, /stat-icon-img|stat-icon generated/)
  assert.doesNotMatch(overview, /iconActiveSubscription|iconPlatform/)

  assert.equal(desktop.match(/class="summary-icon/g)?.length, 3)
  assert.doesNotMatch(desktop, /desktopIconRecommend|desktopIconProtocol|desktopIconPlatform/)

  assert.equal(detail.match(/class="summary-icon/g)?.length, 4)
  assert.doesNotMatch(detail, /detail-summary-icon|statusNormal/)

  assert.ok(pxValue(cssBlock(style, '.summary-icon'), 'width') === 56)
  assert.ok(pxValue(cssBlock(style, '.summary-icon'), 'height') === 56)
  assert.match(cssBlock(style, '.desktop-feature-bar .summary-icon,\n.node-detail-summary .summary-icon'), /width:\s*56px/)
  assert.match(cssBlock(style, '.desktop-feature-bar .summary-icon,\n.node-detail-summary .summary-icon'), /height:\s*56px/)
})

test('mobile guide removes QR code and points scanning to node detail', () => {
  const source = readScreen('MobileGuideScreen.vue')
  const index = readShareIndex()
  const style = readStyle()
  const actionBlocks = Array.from(source.matchAll(/<div class="mobile-actions[^"]*">([\s\S]*?)<\/div>/g), match => match[1])

  assert.match(source, /节点详情页/)
  assert.doesNotMatch(source, /fake-qr|qr-card|ios-import-layout|qrUrl|defineProps/)
  assert.doesNotMatch(source, /<img[^>]+qrUrl/)
  assert.doesNotMatch(index, /<MobileGuideScreen[\s\S]*:qr-url=/)
  assert.doesNotMatch(index, /primaryQrUrl|refreshPrimaryQrCode/)
  assert.equal(actionBlocks.length, 2)
  for (const block of actionBlocks) {
    assert.doesNotMatch(block, /<button/)
  }
  assert.doesNotMatch(source, /\$emit\('copy-vless'|\$emit\('download-qr'|\$emit\('open-guide'/)
  assert.doesNotMatch(style, /\.fake-qr|\.qr-card|\.ios-import-layout/)
  assert.match(cssBlock(style, '.mobile-actions.tip-only'), /grid-template-columns:\s*1fr/)
})

test('mobile FAQ keeps only the question grid without a contact button', () => {
  const source = readScreen('MobileGuideScreen.vue')
  const style = readStyle()

  assert.doesNotMatch(source, /contact-wide|联系管理员/)
  assert.doesNotMatch(style, /\.contact-wide/)
})

test('mobile guide bottom panels align with method columns and keep download cards structured', () => {
  const style = readStyle()
  const bottomGrid = cssBlock(style, '.mobile-bottom-grid')
  const downloadPanel = cssBlock(style, '.mobile-download-panel,\n.mobile-faq-panel')
  const downloadButton = cssBlock(style, '.mobile-download-cards button')

  assert.match(bottomGrid, /grid-template-columns:\s*1fr 1fr/)
  assert.match(bottomGrid, /align-items:\s*stretch/)
  assert.equal(pxValue(bottomGrid, 'gap'), pxValue(cssBlock(style, '.desktop-method-grid,\n.mobile-method-grid'), 'gap'))
  assert.match(downloadPanel, /align-self:\s*stretch/)
  assert.match(downloadButton, /grid-template-columns:\s*72px minmax\(0,\s*1fr\)/)
  assert.match(downloadButton, /grid-template-rows:\s*auto auto minmax\(0,\s*1fr\) auto/)
  assert.match(cssBlock(style, '.mobile-download-cards .client-icon'), /grid-row:\s*1 \/ 4/)
  assert.match(cssBlock(style, '.mobile-download-cards strong,\n.mobile-download-cards b,\n.mobile-download-cards small'), /grid-column:\s*2/)
  assert.match(cssBlock(style, '.mobile-download-cards span'), /grid-column:\s*1 \/ -1/)
})

test('mobile client icon component renders real app artwork for v2rayNG and Shadowrocket', () => {
  const source = readComponent('ClientIcon.vue')
  const mobileScreen = readScreen('MobileGuideScreen.vue')

  assert.match(source, /<img[^>]+type === 'v2rayng'[^>]+:src="iconV2rayng"/)
  assert.match(source, /<img[^>]+type === 'shadowrocket'[^>]+:src="iconShadowrocket"/)
  assert.match(source, /import iconV2rayng from '@\/assets\/share\/customer\/icon-v2rayng\.png'/)
  assert.match(source, /import iconShadowrocket from '@\/assets\/share\/customer\/icon-shadowrocket\.png'/)
  assert.doesNotMatch(source, /type === 'v2rayng'[\s\S]{0,90}client-icon-letter/)
  assert.doesNotMatch(source, /type === 'shadowrocket'[\s\S]{0,90}mark-rocket/)
  assert.match(mobileScreen, /<ClientIcon type="v2rayng" size="xl" \/><strong>Android 下载<\/strong>/)
  assert.match(mobileScreen, /<ClientIcon type="shadowrocket" size="xl" \/><strong>iOS 下载<\/strong>/)
})

test('customer share UI does not rely on font-dependent decorative glyphs for icons', () => {
  const unstableGlyphs = /[▱▯▤▦▣♧♟⊞◆☼⟳⌄⌕⇩⌘↗←‹›∞☆⚖✓◷ⓘ♬]/u
  for (const file of collectShareFiles()) {
    const source = readFileSync(file, 'utf8')
    assert.doesNotMatch(source, unstableGlyphs, `${file} still contains font-dependent icon glyphs`)
  }
})

test('overview side stack has no gray container background', () => {
  const style = readStyle()
  const sideStack = cssBlock(style, '.overview-side-stack')
  const background = sideStack.match(/background:\s*([^;]+)/)?.[1]?.trim()
  assert.equal(background, 'transparent', 'side stack should explicitly stay transparent')
})

test('overview quick guide is slightly raised to align with the shortened subscription list', () => {
  const style = readStyle()
  assert.match(cssBlock(style, '.overview-main-grid'), /align-items:\s*start/)
  assert.match(cssBlock(style, '.subscription-list-panel'), /height:\s*376px/)
  assert.match(cssBlock(style, '.quick-guide-panel'), /min-height:\s*376px/)
  assert.match(cssBlock(style, '.subscription-list-panel'), /align-self:\s*start/)
  assert.match(cssBlock(style, '.overview-side-stack'), /align-self:\s*start/)
  assert.doesNotMatch(cssBlock(style, '.overview-side-stack'), /min-height:\s*100%/)
  assert.doesNotMatch(cssBlock(style, '.quick-guide-panel'), /height:\s*100%/)
})

test('overview subscription list shows all rows without pagination and scrolls inside the table', () => {
  const source = readScreen('OverviewScreen.vue')
  const style = readStyle()
  const tableBlock = cssBlock(style, '.share-table')
  const panelBlock = cssBlock(style, '.subscription-list-panel')

  assert.doesNotMatch(source, /class="table-footer"/)
  assert.doesNotMatch(source, /上一页|下一页|条\/页/)
  assert.match(source, /<div v-for="node in nodes"/)
  assert.match(panelBlock, /display:\s*flex/)
  assert.match(panelBlock, /flex-direction:\s*column/)
  assert.match(tableBlock, /flex:\s*1 1 auto/)
  assert.match(tableBlock, /min-height:\s*0/)
  assert.match(tableBlock, /overflow-y:\s*auto/)
  assert.match(cssBlock(style, '.quick-guide-panel'), /flex:\s*0 0 auto/)
})

test('overview download section uses compact spacing and text-only lane headings', () => {
  const source = readScreen('OverviewScreen.vue')
  const style = readStyle()
  const downloadPanel = cssBlock(style, '.download-overview-panel')

  assert.doesNotMatch(downloadPanel, /margin-top:/)
  assert.match(source, /<h3>桌面端下载<\/h3>/)
  assert.match(source, /<h3>手机端下载<\/h3>/)
  assert.doesNotMatch(source, /<span class="title-icon (desktop|mobile)"><\/span>(桌面端|手机端)下载/)
})

test('overview quick guide tutorial buttons center their labels', () => {
  const style = readStyle()
  const tutorialButton = cssBlock(style, '.quick-item > span:last-child')

  assert.match(tutorialButton, /display:\s*inline-flex/)
  assert.match(tutorialButton, /align-items:\s*center/)
  assert.match(tutorialButton, /justify-content:\s*center/)
  assert.match(tutorialButton, /line-height:\s*1/)
})

test('hero artwork is treated as ambient decoration without hard rectangles', () => {
  const style = readStyle()
  const overview = readScreen('OverviewScreen.vue')
  assert.doesNotMatch(cssBlock(style, '.customer-share-root'), /66\.2%/)
  assert.doesNotMatch(cssBlock(style, '.customer-share-root'), /linear-gradient\(112deg/)
  assert.match(cssBlock(style, '.share-hero-art'), /mix-blend-mode:\s*multiply/)
  assert.match(cssBlock(style, '.share-hero-art'), /mask-image:\s*radial-gradient/)
  assert.match(cssBlock(style, '.unlock-art'), /mask-image:\s*radial-gradient/)
  assert.match(cssBlock(style, '.unlock-art'), /opacity:\s*0\.[23]/)
  assert.match(overview, /hero-desktop\.png/)
  assert.match(cssBlock(style, '.overview-hero'), /min-height:\s*132px/)
  const overviewHeroArt = cssBlock(style, '.overview-hero .share-hero-art')
  assert.match(overviewHeroArt, /height:\s*132px/)
  assert.match(overviewHeroArt, /object-fit:\s*contain/)
  assert.match(overviewHeroArt, /object-position:\s*center/)
  assert.match(overviewHeroArt, /opacity:\s*0\.[34]/)
  const heroArt = cssBlock(style, '.tutorial-hero .share-hero-art')
  assert.match(heroArt, /width:\s*(6[4-9]\d|[7-9]\d\d)px/)
  assert.match(heroArt, /opacity:\s*0\.[34]/)
  assert.doesNotMatch(heroArt, /opacity:\s*0\.[89]/)
  assert.match(cssBlock(style, '.download-hero .share-hero-art,\n.guide-hero .share-hero-art'), /opacity:\s*0\.[34]/)
  assert.match(cssBlock(style, '.node-detail-hero .share-hero-art'), /opacity:\s*0\.[34]/)
})

test('desktop tutorial lower cards use compact content-height layouts', () => {
  const style = readStyle()
  assert.match(cssBlock(style, '.desktop-bottom-grid'), /align-items:\s*start/)
  assert.doesNotMatch(cssBlock(style, '.desktop-faq,\n.desktop-download'), /min-height/)
})

test('desktop method actions and download cards avoid cramped text columns', () => {
  const style = readStyle()
  const desktop = readScreen('DesktopGuideScreen.vue')
  assert.match(desktop, /class="desktop-download-copy"/)
  assert.match(desktop, /class="desktop-download-cta"/)
  assert.match(cssBlock(style, '.method-actions,\n.mobile-actions'), /grid-template-columns:\s*minmax\(154px,\s*auto\) minmax\(150px,\s*auto\) minmax\(0,\s*1fr\)/)
  assert.match(cssBlock(style, '.desktop-download-cards button'), /grid-template-columns:\s*52px minmax\(0,\s*1fr\)/)
  assert.match(cssBlock(style, '.desktop-download-copy'), /min-width:\s*0/)
  assert.match(cssBlock(style, '.desktop-download-copy small'), /line-height:\s*1\.45/)
  assert.match(cssBlock(style, '.desktop-download-cta'), /grid-column:\s*1 \/ -1/)
  assert.doesNotMatch(style, /\.desktop-download-cards\s+span\s*\{/)
})

test('download center uses a simplified single-column content layout', () => {
  const source = readScreen('DownloadCenterScreen.vue')
  const style = readStyle()
  assert.doesNotMatch(source, /class="download-sidebar"/)
  assert.doesNotMatch(source, /class="share-panel side-explain"/)
  assert.doesNotMatch(source, /class="safe-tip"/)
  assert.doesNotMatch(style, /\.download-sidebar/)
  assert.doesNotMatch(style, /\.side-explain/)
  assert.doesNotMatch(style, /\.safe-tip/)
  assert.match(source, /class="download-note-strip"/)
  assert.match(cssBlock(style, '.download-grid'), /grid-template-columns:\s*1fr/)
  assert.ok(pxValue(cssBlock(style, '.download-card-grid'), 'gap') <= 16)
  assert.ok(pxValue(cssBlock(style, '.download-client-card'), 'min-height') <= 152)
})

test('node detail is a full screen view based on desktop import content, not a dialog', () => {
  const index = readShareIndex()
  const detail = readScreen('NodeDetailScreen.vue')
  const desktop = readScreen('DesktopGuideScreen.vue')

  assert.match(index, /import NodeDetailScreen from '\.\/screens\/NodeDetailScreen\.vue'/)
  assert.match(index, /activeView === 'nodeDetail'/)
  assert.doesNotMatch(index, /NodeDetailDialog/)
  assert.doesNotMatch(index, /detailVisible/)
  assert.match(index, /function openNodeDetail\(node\)[\s\S]*activeView\.value = 'nodeDetail'/)

  assert.match(detail, /class="share-screen node-detail-screen"/)
  assert.match(detail, /crumb="我的订阅 \/ 节点详情"/)
  assert.match(detail, /hero-node-detail\.png/)
  assert.match(detail, /desktopV2raySteps/)
  assert.match(detail, /desktopClashSteps/)
  assert.match(detail, /<StepList :steps="desktopV2raySteps"/)
  assert.match(detail, /<StepList :steps="desktopClashSteps" purple/)
  assert.match(detail, /class="share-panel node-qr-panel polished-node-card"/)
  assert.match(detail, /class="share-panel node-detail-tips polished-node-card"/)

  assert.match(desktop, /<section class="share-screen desktop-screen">/)
  assert.doesNotMatch(desktop, /node-detail-screen/)
})

test('node detail uses polished action and bottom card styling', () => {
  const style = readStyle()
  assert.match(cssBlock(style, '.node-detail-hero .share-hero-art'), /object-fit:\s*cover/)
  assert.ok(pxValue(cssBlock(style, '.node-detail-hero .share-hero-art'), 'height') <= 170)
  assert.match(cssBlock(style, '.node-detail-hero .share-hero-actions'), /right:\s*0/)
  assert.match(cssBlock(style, '.node-method-card .method-actions'), /grid-template-columns:\s*minmax\(140px,\s*auto\) minmax\(180px,\s*auto\) minmax\(0,\s*1fr\)/)
  assert.match(cssBlock(style, '.node-qr-panel'), /background:\s*linear-gradient/)
  assert.match(cssBlock(style, '.node-detail-tips button'), /grid-template-columns:\s*32px minmax\(140px,\s*0\.72fr\) minmax\(0,\s*1fr\) 20px/)
})
