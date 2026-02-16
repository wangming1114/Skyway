import request from '@/utils/request'

/** 发送邮箱验证码 */
export function sendEmailCode(email) {
  return request({
    url: '/c-api/auth/sendEmailCode',
    method: 'post',
    data: { email }
  })
}

/** C 端注册 */
export function register(data) {
  return request({
    url: '/c-api/auth/register',
    method: 'post',
    data
  })
}

/** C 端登录（account 为邮箱或用户名） */
export function login(data) {
  return request({
    url: '/c-api/auth/login',
    method: 'post',
    data: { account: data.account, password: data.password }
  })
}

/** 发送找回密码验证码 */
export function sendResetCode(email) {
  return request({
    url: '/c-api/auth/sendResetCode',
    method: 'post',
    data: { email }
  })
}

/** 找回密码（邮箱 + 验证码 + 新密码） */
export function resetPassword(data) {
  return request({
    url: '/c-api/auth/resetPassword',
    method: 'post',
    data: { email: data.email, code: data.code, password: data.password }
  })
}
