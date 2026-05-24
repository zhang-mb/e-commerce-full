import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动带 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理后端返回
request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      if (data.code === 401) {
        // 未登录或token过期
        ElMessageBox.confirm(
          '登录已过期，请重新登录',
          '提示',
          {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          localStorage.removeItem('token')
          window.location.href = '/login'
        })
      } else {
        ElMessage.error(data.msg || '请求失败')
      }
      return Promise.reject(data)
    }
    return data
  },
  err => {
    ElMessage.error('网络异常或服务器错误')
    return Promise.reject(err)
  }
)

export default request