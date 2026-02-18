import request from '@/utils/request'

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

// ========== 仪表盘 ==========
export function getDashboardSummary() {
  return request({
    url: '/resource/vps/dashboard/summary',
    method: 'get'
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

/** 节点流量统计（总量 + 近期明细） */
export function getProxyNodeTraffic(nodeId) {
  return request({
    url: '/resource/vps/proxyNode/' + nodeId + '/traffic',
    method: 'get'
  })
}

export function addProxyNode(data) {
  return request({
    url: '/resource/vps/proxyNode',
    method: 'post',
    data: data
  })
}

export function updateProxyNode(data) {
  return request({
    url: '/resource/vps/proxyNode',
    method: 'put',
    data: data
  })
}

export function delProxyNode(ids) {
  return request({
    url: '/resource/vps/proxyNode/' + ids,
    method: 'delete'
  })
}

/** 推荐可用端口（从 10000 起连续、未占用的端口，用于添加节点时预填） */
export function getRecommendPort(instanceId) {
  return request({
    url: '/resource/vps/instance/' + instanceId + '/recommendPort',
    method: 'get'
  })
}

/** 在指定实例上添加代理节点（HTTP 同步，用于客户详情等无 WebSocket 场景） */
export function addProxyNodeOnInstance(instanceId, data) {
  return request({
    url: '/resource/vps/instance/' + instanceId + '/proxyNode',
    method: 'post',
    data: data
  })
}
