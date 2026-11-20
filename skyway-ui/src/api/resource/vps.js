import request from '@/utils/request'
import useUserStore from '@/store/modules/user'
import { operationId, WS_TLS } from '@/utils/wsNode'

const pendingPrefix = () => 'skyway.ws.operations.' + useUserStore().id + '.'
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

function rememberOperation(id, scope) {
  const key = pendingPrefix() + id
  if (scope) localStorage.setItem(key, JSON.stringify(scope))
  else localStorage.removeItem(key)
}

export function pendingWsOperations(instanceId, customerId) {
  const prefix = pendingPrefix()
  const ids = []
  for (const key of Object.keys(localStorage)) {
    if (!key.startsWith(prefix)) continue
    try {
      const scope = JSON.parse(localStorage.getItem(key))
      if ((!instanceId || Number(scope.instanceId) === Number(instanceId)) &&
          (!customerId || Number(scope.customerId) === Number(customerId))) ids.push(key.slice(prefix.length))
    } catch { localStorage.removeItem(key) }
  }
  return ids
}

function networkFailure(error) {
  return !!error?.isAxiosError && (!error.response || error.response.status >= 500)
}

export async function waitWsOperation(result, onStage) {
  const id = result.data?.operationId
  if (!id) return result
  let state = result.data
  let previousStage
  let wait = 1500
  while (true) {
    if (state.stage && state.stage !== previousStage) {
      previousStage = state.stage
      onStage?.(state.stage)
    }
    if (state.state === 'DONE' || state.state === 'FAILED') {
      rememberOperation(id, null)
      if (state.state === 'FAILED') throw new Error(state.stage)
      return { ...result, data: state.node }
    }
    await delay(wait)
    try {
      const response = await request({ url: '/resource/vps/proxyNode/wsOperations/' + id, method: 'get' })
      if (!response.data) throw new Error('未找到该操作，请重新提交')
      state = response.data
      wait = 1500
    } catch (error) {
      if (!networkFailure(error)) { rememberOperation(id, null); throw error }
      wait = Math.min(wait * 2, 15000)
    }
  }
}

async function submitWsOperation(options, scope, onStage) {
  const id = options.data.operationId
  rememberOperation(id, scope)
  for (let attempt = 0; ; attempt++) {
    let response
    try {
      response = await request({ ...options, headers: { repeatSubmit: false } })
    } catch (error) {
      if (!networkFailure(error)) { rememberOperation(id, null); throw error }
      if (attempt >= 2) return waitWsOperation({ data: { operationId: id } }, onStage)
      await delay(1500)
      continue
    }
    return waitWsOperation(response, onStage)
  }
}

// ========== 分类（树） ==========
export function listCategoryTree() {
  return request({
    url: '/resource/vps/category/list',
    method: 'get'
  })
}

export function getCategory(id) {
  return request({
    url: '/resource/vps/category/' + id,
    method: 'get'
  })
}

export function addCategory(data) {
  return request({
    url: '/resource/vps/category',
    method: 'post',
    data: data
  })
}

export function updateCategory(data) {
  return request({
    url: '/resource/vps/category',
    method: 'put',
    data: data
  })
}

export function delCategory(id) {
  return request({
    url: '/resource/vps/category/' + id,
    method: 'delete'
  })
}

// ========== 节点（下拉） ==========
export function listNode() {
  return request({
    url: '/resource/vps/node/list',
    method: 'get'
  })
}

// ========== VPS 实例 ==========
export function listInstance(query) {
  return request({
    url: '/resource/vps/instance/list',
    method: 'get',
    params: query
  })
}

export function getInstance(id) {
  return request({
    url: '/resource/vps/instance/' + id,
    method: 'get'
  })
}

/** VPS 实例实时网速（按 sing-box 端口汇总，单位由后端返回 MB/s） */
export function getInstanceSpeed(id) {
  return request({
    url: '/resource/vps/instance/' + id + '/speed',
    method: 'get'
  })
}

/** VPS 实例实时网速内存快照（一次返回全部实例，不触发 SSH） */
export function getInstanceSpeedSnapshot() {
  return request({
    url: '/resource/vps/instance/speed/snapshot',
    method: 'get'
  })
}

/** 连接测试并拉取 CPU/内存/磁盘（用于新增/编辑时的连接测试按钮） */
export function testConnection(data) {
  return request({
    url: '/resource/vps/instance/testConnection',
    method: 'post',
    data: data
  })
}

export function addInstance(data) {
  return request({
    url: '/resource/vps/instance',
    method: 'post',
    data: data
  })
}

export function updateInstance(data) {
  return request({
    url: '/resource/vps/instance',
    method: 'put',
    data: data
  })
}

export function delInstance(id) {
  return request({
    url: '/resource/vps/instance/' + id,
    method: 'delete'
  })
}

export function forceDelInstance(id) {
  return request({
    url: '/resource/vps/instance/' + id + '/force',
    method: 'delete'
  })
}

// ========== 仪表盘 ==========
export function getDashboardSummary() {
  return request({
    url: '/resource/vps/dashboard/summary',
    method: 'get'
  })
}

export function getDashboardVpsTrafficTrend(days) {
  return request({
    url: '/resource/vps/dashboard/vpsTrafficTrend',
    method: 'get',
    params: { days }
  })
}

export function getDashboardVpsTrafficRank(params) {
  return request({
    url: '/resource/vps/dashboard/vpsTrafficRank',
    method: 'get',
    params
  })
}

export function getDashboardCustomerTrafficRank(params) {
  return request({
    url: '/resource/vps/dashboard/customerTrafficRank',
    method: 'get',
    params
  })
}

// ========== 代理节点 ==========
export function listProxyNode(query) {
  return request({
    url: '/resource/vps/proxyNode/list',
    method: 'get',
    params: query
  })
}

export function getProxyNode(id) {
  return request({
    url: '/resource/vps/proxyNode/' + id,
    method: 'get'
  })
}

export function getProxyDomainWhitelistPresets() {
  return request({
    url: '/resource/vps/proxyNode/domainWhitelist/presets',
    method: 'get'
  })
}

export function getProxyDomainPolicyPresets() {
  return request({
    url: '/resource/vps/proxyNode/domainPolicy/presets',
    method: 'get'
  })
}

export function updateProxyDomainWhitelist(nodeId, data) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/domainWhitelist',
    method: 'put',
    data
  })
}

export function batchUpdateProxyDomainWhitelist(data) {
  return request({
    url: '/resource/vps/proxyNode/domainWhitelist/batch',
    method: 'put',
    data
  })
}

export function updateProxyDomainPolicy(nodeId, data) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/domainPolicy',
    method: 'put',
    data
  })
}

export function batchUpdateProxyDomainPolicy(data) {
  return request({
    url: '/resource/vps/proxyNode/domainPolicy/batch',
    method: 'put',
    data
  })
}

/** 节点流量统计（总量 + 近期明细） */
export function getProxyNodeTraffic(nodeId) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/traffic',
    method: 'get'
  })
}

/** 指定实例下的节点端口限速规则 */
export function listProxyNodeRateLimit(query) {
  return request({
    url: '/resource/vps/proxyNode/rateLimit/list',
    method: 'get',
    params: query
  })
}

/** 单个节点端口限速规则 */
export function getProxyNodeRateLimit(nodeId) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/rateLimit',
    method: 'get'
  })
}

/** 设置或修改节点端口限速 */
export function setProxyNodeRateLimit(nodeId, data) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/rateLimit',
    method: 'post',
    data: data
  })
}

/** 移除节点端口限速 */
export function removeProxyNodeRateLimit(nodeId) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/rateLimit',
    method: 'delete'
  })
}

export function addProxyNode(data) {
  return request({
    url: '/resource/vps/proxyNode',
    method: 'post',
    data: data
  })
}

export function updateProxyNode(data, onStage, node) {
  if (node?.nodeType === WS_TLS && node.listenPort) data.operationId ||= operationId()
  const options = { url: '/resource/vps/proxyNode', method: 'put', data }
  if (data.operationId) return submitWsOperation(options, { instanceId: node?.instanceId ?? data.instanceId, customerId: node?.customerId ?? data.customerId }, onStage)
  return request(options).then(response => waitWsOperation(response, onStage))
}

export function delProxyNode(ids) {
  return request({
    url: '/resource/vps/proxyNode/' + ids,
    method: 'delete'
  })
}

export function forceDelProxyNode(ids) {
  return request({
    url: '/resource/vps/proxyNode/' + ids + '/force',
    method: 'delete'
  })
}

/** 推荐 VPS 实时可用端口；params 可传 excludePorts 排除本批已尝试端口 */
export function getRecommendPort(instanceId, params) {
  return request({
    url: '/resource/vps/instance/' + instanceId + '/recommendPort',
    method: 'get',
    params
  })
}

/** 检查实例 SSH 是否可连接（用于添加节点前先建立 SSH） */
export function checkInstanceSsh(instanceId) {
  return request({
    url: '/resource/vps/instance/' + instanceId + '/sshCheck',
    method: 'get'
  })
}

/** 在指定实例上添加代理节点（HTTP 同步，用于客户详情等无 WebSocket 场景） */
export function addProxyNodeOnInstance(instanceId, data, onStage) {
  const options = { url: '/resource/vps/instance/' + instanceId + '/proxyNode', method: 'post', data }
  if (data.nodeType !== WS_TLS) return request(options)
  data.operationId ||= operationId()
  return submitWsOperation(options, { instanceId, customerId: data.customerId }, onStage)
}
