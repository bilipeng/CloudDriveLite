// 用户相关 API 服务

export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
  timestamp?: string
}

// 用户信息
export type UserInfo = {
  id: number
  username: string
  userNumber: string
  phoneNumber: string
  email: string
  role: 'USER' | 'ADMIN'
  status: number
  createdAt: string
}

// 用户存储信息
export type UserStorageInfo = {
  userId: number
  username: string
  userNumber: string
  maxStorage: number
  usedStorage: number
  usagePercent: number
}

import { handleUnauthorized, isUnauthorizedResponse } from '@/utils/auth'

// 通用 API 请求函数
async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })
  
  if (!res.ok) {
    // 处理 HTTP 401 未授权
    if (res.status === 401) {
      handleUnauthorized('登录已过期，请重新登录')
      throw new Error('登录已过期，请重新登录')
    }
    throw new Error(`请求失败: ${res.status} ${res.statusText}`)
  }
  
  const json: ApiResponse<T> = await res.json()
  if (!json.success) {
    const message = json.message || '请求失败'
    // 检查是否未登录
    if (isUnauthorizedResponse(message)) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data
}

/**
 * 获取当前用户信息
 */
export async function getCurrentUserInfo(): Promise<UserInfo> {
  return request<UserInfo>('/api/user/info')
}

/**
 * 更新当前用户信息
 */
export async function updateUserInfo(params: {
  username?: string
  phoneNumber?: string
  email?: string
}): Promise<{
  username: string
  phoneNumber: string
  email: string
}> {
  const formData = new FormData()
  if (params.username) formData.append('username', params.username)
  if (params.phoneNumber) formData.append('phoneNumber', params.phoneNumber)
  if (params.email !== undefined) formData.append('email', params.email || '')
  
  const res = await fetch('/api/user/info', {
    method: 'PUT',
    body: formData,
    credentials: 'include'
  })
  const json: ApiResponse<{
    username: string
    phoneNumber: string
    email: string
  }> = await res.json()
  if (!json.success) {
    const message = json.message || '更新失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data
}

/**
 * 修改密码
 */
export async function changePassword(params: {
  oldPassword: string
  newPassword: string
}): Promise<void> {
  const formData = new FormData()
  formData.append('oldPassword', params.oldPassword)
  formData.append('newPassword', params.newPassword)
  
  const res = await fetch('/api/user/password', {
    method: 'PUT',
    body: formData,
    credentials: 'include'
  })
  const json: ApiResponse<void> = await res.json()
  if (!json.success) {
    const message = json.message || '修改密码失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
}

/**
 * 获取当前用户的存储信息
 */
export async function getCurrentUserStorage(): Promise<UserStorageInfo> {
  return request<UserStorageInfo>('/api/user/storage')
}

