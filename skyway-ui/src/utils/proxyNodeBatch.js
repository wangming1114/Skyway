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
