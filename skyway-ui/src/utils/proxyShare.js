export const DEFAULT_SUBCONVERTER_URL = 'https://api.wcc.best/sub'
export const DEFAULT_CLASH_CONFIG_URL = 'https://raw.githubusercontent.com/ACL4SSR/ACL4SSR/master/Clash/config/ACL4SSR_Online.ini'
export const DEFAULT_QR_SERVICE_URL = 'https://api.qrserver.com/v1/create-qr-code/'

export function parseVlessUrl(vlessUrl) {
  const raw = String(vlessUrl || '').trim()
  if (!raw) throw new Error('VLESS 链接为空')
  const parsed = new URL(raw)
  if (parsed.protocol !== 'vless:') throw new Error('仅支持 vless:// 链接')
  const params = {}
  parsed.searchParams.forEach((value, key) => {
    params[key] = value
  })
  return {
    raw,
    uuid: decodeURIComponent(parsed.username || ''),
    host: parsed.hostname,
    port: parsed.port,
    name: parsed.hash ? decodeURIComponent(parsed.hash.slice(1)) : '',
    params
  }
}

export function buildClashSubscribeUrl(vlessUrl, options = {}) {
  const baseUrl = options.baseUrl || DEFAULT_SUBCONVERTER_URL
  const configUrl = options.configUrl || DEFAULT_CLASH_CONFIG_URL
  return `${baseUrl}?target=clash&url=${encodeURIComponent(String(vlessUrl || '').trim())}&insert=false&config=${encodeURIComponent(configUrl)}`
}

export function buildQrImageUrl(text, size = 220, options = {}) {
  const qrSize = Number.isFinite(Number(size)) ? Number(size) : 220
  const baseUrl = options.baseUrl || DEFAULT_QR_SERVICE_URL
  return `${baseUrl}?size=${qrSize}x${qrSize}&data=${encodeURIComponent(String(text || '').trim())}`
}

export function safeProxyShareFilename(name, ext) {
  const normalizedExt = String(ext || '').replace(/^\.+/, '') || 'txt'
  const safeName = String(name || '')
    .trim()
    .replace(/[\\/:*?"<>|#]+/g, '-')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
  return `${safeName || 'proxy-share'}.${normalizedExt}`
}

export function normalizeShareNode(node = {}, now = new Date()) {
  const parsedUrl = tryParseShareUrl(node.url)
  const genericUrl = tryParseGenericUrl(node.url)
  const expireDate = node.expireTime ? new Date(node.expireTime) : null
  const remainingDays = expireDate && !Number.isNaN(expireDate.getTime())
    ? Math.ceil((expireDate.getTime() - now.getTime()) / 86400000)
    : null
  const status = String(node.status ?? '0')
  const isExpired = remainingDays !== null && remainingDays < 0
  const isDisabled = status !== '0'
  const address = node.address || parsedUrl?.host || genericUrl?.host || ''
  const port = node.port || parsedUrl?.port || genericUrl?.port || ''
  const protocol = node.nodeType || inferProtocol(node, parsedUrl)
  const rawUrl = String(node.url || '').trim()
  const isVless = rawUrl.toLowerCase().startsWith('vless://')
  const isClash = /clash/i.test(protocol) || /^https?:\/\//i.test(rawUrl)

  return {
    ...node,
    id: node.id ?? `${address}:${port}:${node.nodeName || parsedUrl?.name || ''}`,
    name: node.nodeName || parsedUrl?.name || '代理节点',
    protocol,
    endpoint: address && port ? `${address}:${port}` : (address || '-'),
    expireDate,
    remainingDays,
    statusText: isExpired ? '已过期' : (isDisabled ? '停用' : '正常'),
    isActive: !isDisabled && !isExpired,
    isDisabled,
    isExpired,
    isExpiringSoon: remainingDays !== null && remainingDays >= 0 && remainingDays <= 7,
    vlessUrl: isVless ? rawUrl : '',
    clashUrl: isVless ? buildClashSubscribeUrl(rawUrl) : (isClash ? rawUrl : '')
  }
}

export function filterShareNodes(nodes = [], options = {}) {
  const status = options.status || 'all'
  const keyword = String(options.keyword || '').trim().toLowerCase()

  return nodes.filter(node => {
    const statusMatched = status === 'all'
      || (status === 'active' && node.isActive)
      || (status === 'expiring' && node.isExpiringSoon)
      || (status === 'expired' && node.isExpired)
      || (status === 'disabled' && node.isDisabled)

    if (!statusMatched) return false
    if (!keyword) return true

    return [
      node.name,
      node.endpoint,
      node.protocol,
      node.statusText,
      node.remainingLabel
    ].filter(Boolean).join(' ').toLowerCase().includes(keyword)
  })
}

export function buildShareNodeSummary(nodes = []) {
  return nodes.reduce((summary, node) => {
    if (node.isActive) summary.activeCount += 1
    if (node.isExpiringSoon) summary.expiringSoonCount += 1
    return summary
  }, { activeCount: 0, expiringSoonCount: 0 })
}

function tryParseShareUrl(url) {
  if (!url) return null
  try {
    return parseVlessUrl(url)
  } catch {
    return null
  }
}

function tryParseGenericUrl(url) {
  if (!url) return null
  try {
    return new URL(String(url).trim())
  } catch {
    return null
  }
}

function inferProtocol(node, parsedUrl) {
  if (parsedUrl) {
    const security = parsedUrl.params?.security
    const network = parsedUrl.params?.type
    if (security === 'reality') return 'VLESS-REALITY'
    if (network === 'grpc') return 'VLESS-GRPC'
    return 'VLESS'
  }
  return node.url ? '订阅' : '-'
}
