import request from '@/utils/request'

// 客户列表
export function listCustomer(query) {
  return request({
    url: '/member/customer/list',
    method: 'get',
    params: query
  })
}

// 客户详情
export function getCustomer(customerId) {
  return request({
    url: '/member/customer/' + customerId,
    method: 'get'
  })
}

// 客户关联节点列表
export function getCustomerBindings(customerId) {
  return request({
    url: '/member/customer/' + customerId + '/bindings',
    method: 'get'
  })
}

// 新增客户
export function addCustomer(data) {
  return request({
    url: '/member/customer',
    method: 'post',
    data: data
  })
}

// 修改客户
export function updateCustomer(data) {
  return request({
    url: '/member/customer',
    method: 'put',
    data: data
  })
}

// 删除客户
export function delCustomer(ids) {
  return request({
    url: '/member/customer/' + ids,
    method: 'delete'
  })
}

// 重置密码
export function resetCustomerPwd(customerId, password) {
  return request({
    url: '/member/customer/resetPwd',
    method: 'put',
    data: { id: customerId, password }
  })
}

// 状态修改
export function changeCustomerStatus(customerId, status) {
  return request({
    url: '/member/customer/changeStatus',
    method: 'put',
    data: { id: customerId, status }
  })
}

