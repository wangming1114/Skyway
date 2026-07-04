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
