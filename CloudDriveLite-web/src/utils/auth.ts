// 认证相关工具函数

import router from '@/router'
import { ElMessage } from 'element-plus'

/**
 * 处理未登录情况，清除本地存储并跳转到登录页
 */
export function handleUnauthorized(message?: string) {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('userNumber')
  localStorage.removeItem('userName')
  
  // 显示提示信息
  if (message) {
    ElMessage.warning(message)
  } else {
    ElMessage.warning('登录已过期，请重新登录')
  }
  
  // 跳转到登录页（如果不在登录页）
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
}

/**
 * 检查响应是否表示未登录
 */
export function isUnauthorizedResponse(message: string | undefined): boolean {
  if (!message) return false
  const lowerMessage = message.toLowerCase()
  return lowerMessage.includes('未登录') || 
         lowerMessage.includes('登录已过期') || 
         lowerMessage.includes('请先登录') ||
         lowerMessage.includes('unauthorized') ||
         lowerMessage.includes('未授权')
}


