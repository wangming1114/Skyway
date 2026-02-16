import request from '@/utils/request'

/** 获取当前用户信息 */
export function getUserInfo() {
  return request({
    url: '/c-api/user/info',
    method: 'get'
  })
}

/** 更新个人资料 */
export function updateProfile(data) {
  return request({
    url: '/c-api/user/profile',
    method: 'put',
    data
  })
}

/** 修改密码 */
export function changePwd(data) {
  return request({
    url: '/c-api/user/changePwd',
    method: 'post',
    data
  })
}
