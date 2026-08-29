export function parseSocks5RelayText(value) {
  const text = (value || '').trim()
  if (!text) return { ok: false, message: '请输入 SOCKS5 中转配置' }
  const parts = text.split(':')
  if (parts.length !== 4 || parts.some(part => !part.trim())) {
    return { ok: false, message: '格式应为 host:port:username:password' }
  }
  const port = Number(parts[1].trim())
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    return { ok: false, message: 'SOCKS5 端口范围为 1-65535' }
  }
  return {
    ok: true,
    text,
    host: parts[0].trim(),
    port: String(port),
    username: parts[2].trim(),
    password: parts[3].trim()
  }
}

export function parseSocks5RelayLines(value) {
  return String(value || '')
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean)
    .map(text => ({ text, ...parseSocks5RelayText(text) }))
}

/** Returns one calendar month later in the datetime format used by node forms. */
export function getDefaultNodeExpireTime(now = new Date()) {
  const target = new Date(now instanceof Date ? now.getTime() : now)
  if (Number.isNaN(target.getTime())) return ''
  const originalDay = target.getDate()
  target.setDate(1)
  target.setMonth(target.getMonth() + 1)
  const lastDayOfTargetMonth = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()
  target.setDate(Math.min(originalDay, lastDayOfTargetMonth))
  const pad = value => String(value).padStart(2, '0')
  return `${target.getFullYear()}-${pad(target.getMonth() + 1)}-${pad(target.getDate())} ${pad(target.getHours())}:${pad(target.getMinutes())}:${pad(target.getSeconds())}`
}
