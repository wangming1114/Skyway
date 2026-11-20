export const WS_TLS = 'VLESS-WS-TLS'

export function operationId() {
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 15) | 64
  bytes[8] = (bytes[8] & 63) | 128
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
  return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-')
}

export const validSubdomain = value => /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/i.test(value)
export const cloudflarePorts = [443, 2053, 2083, 2087, 2096, 8443]

export function fixedSubscription(path, origin = globalThis.location?.origin, base = import.meta.env?.VITE_APP_BASE_API || '') {
  if (!origin || !/^\/share\/subscription\/[a-f0-9]{64}\/clash$/.test(path || '')) return ''
  const api = base.replace(/\/$/, '')
  const prefix = /^\/(dev|prod|stage|c)-api$/.test(api) ? '' : api
  return new URL(prefix + path, origin).href
}
