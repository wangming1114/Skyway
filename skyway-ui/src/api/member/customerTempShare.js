import request from '@/utils/request'

export function listCustomerTempShare(customerId) {
  return request({
    url: '/member/customer/' + customerId + '/temp-shares',
    method: 'get'
  })
}

export function addCustomerTempShare(customerId, data) {
  return request({
    url: '/member/customer/' + customerId + '/temp-shares',
    method: 'post',
    data
  })
}

export function revokeCustomerTempShare(id) {
  return request({
    url: '/member/customer/temp-shares/' + id,
    method: 'delete'
  })
}

export function unlockCustomerTempShare(token, accessPassword) {
  return request({
    url: '/share/customer/' + token + '/unlock',
    method: 'post',
    headers: { isToken: false },
    data: { accessPassword }
  })
}
