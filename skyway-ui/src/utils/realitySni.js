export const DEFAULT_REALITY_SNI = 'www.bing.com'

const domainPattern = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$/i

export function isValidRealitySni(value) {
  if (typeof value !== 'string') return false
  const sni = value.trim()
  return sni.length <= 253 && domainPattern.test(sni) && !/^[0-9.]+$/.test(sni)
}

export function readRealitySni(node) {
  try {
    const config = typeof node?.configJson === 'string' ? JSON.parse(node.configJson) : node?.configJson
    if (typeof config?.sni === 'string' && config.sni.trim()) return config.sni.trim()
  } catch {}
  try {
    return new URL(node?.url).searchParams.get('sni')?.trim() || ''
  } catch {
    return ''
  }
}
