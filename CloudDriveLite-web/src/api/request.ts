import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosResponse, AxiosError } from 'axios'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: 'http://localhost:8080', // 后端服务地址
  timeout: 10000,
  withCredentials: true, // 支持跨域携带cookie
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error: AxiosError) => {
    // 处理HTTP错误
    if (error.response) {
      const { status, data } = error.response
      const message = (data as any)?.message || ''
      switch (status) {
        case 401:
          // 清除本地存储
          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('userNumber')
          localStorage.removeItem('userName')
          // 显示提示并跳转
          ElMessage.warning(message || '登录已过期，请重新登录')
          if (router.currentRoute.value.path !== '/login') {
            router.replace('/login')
          }
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          // 检查响应消息是否表示未登录
          const errorMessage = (data as any)?.message || '请求失败'
          if (errorMessage.includes('未登录') || 
              errorMessage.includes('登录已过期') || 
              errorMessage.includes('请先登录') ||
              errorMessage.includes('未授权')) {
            // 清除本地存储
            localStorage.removeItem('token')
            localStorage.removeItem('userId')
            localStorage.removeItem('userNumber')
            localStorage.removeItem('userName')
            ElMessage.warning(errorMessage)
            if (router.currentRoute.value.path !== '/login') {
              router.replace('/login')
            }
          } else {
            ElMessage.error(errorMessage)
          }
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }
    return Promise.reject(error)
  }
)

export default request
