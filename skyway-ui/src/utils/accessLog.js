export const ACCESS_LOG_MAX_ENTRIES = 1000

export function buildAccessLogWsUrl({ scope, instanceId, nodeId, token, locationLike, baseApi = '' }) {
  if (scope !== 'vps' && scope !== 'node') throw new Error('Invalid access log scope')
  const locationValue = locationLike || globalThis.location
  if (!locationValue) throw new Error('Browser location is unavailable')
  const protocol = locationValue.protocol === 'https:' ? 'wss:' : 'ws:'
  const params = new URLSearchParams({ scope, token: token || '' })
  if (scope === 'vps') {
    if (!instanceId) throw new Error('instanceId is required')
    params.set('instanceId', String(instanceId))
  } else {
    if (!nodeId) throw new Error('nodeId is required')
    params.set('nodeId', String(nodeId))
  }
  return `${protocol}//${locationValue.host}${baseApi}/ws/access-log?${params.toString()}`
}

export function appendAccessLogEntries(current, incoming, limit = ACCESS_LOG_MAX_ENTRIES) {
  const next = [...(current || []), ...(incoming || [])]
  return next.length > limit ? next.slice(next.length - limit) : next
}
