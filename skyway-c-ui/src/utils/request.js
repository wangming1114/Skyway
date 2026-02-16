import axios from 'axios'
import toast from '@/utils/toast'
import { getToken, removeToken } from '@/utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
}, err => Promise.reject(err))

service.interceptors.response.use(res => {
  const data = res.data
  const code = data.code ?? 200
  if (code === 401) {
    removeToken()
    if (!window.location.hash.includes('login')) {
      window.location.href = '/#/login'
    }
    return Promise.reject(new Error('登录已过期'))
  }
  if (code !== 200 && code !== 0) {
    toast.error(data.msg || '请求失败')
    return Promise.reject(new Error(data.msg || '请求失败'))
  }
  return data
}, err => {
  const status = err.response?.status
  if (status === 401 || status === 403) {
    removeToken()
    if (!window.location.hash.includes('login')) {
      toast.warning('登录已过期，请重新登录')
      window.location.href = '/#/login'
    }
    return Promise.reject(err)
  }
  const msg = err.response?.data?.msg || err.message || '网络异常'
  toast.error(msg)
  return Promise.reject(err)
})

export default service
