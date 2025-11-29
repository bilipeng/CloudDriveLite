// 管理员 API 服务

export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
  timestamp?: string
}

// 用户信息类型
export type UserInfo = {
  id: number
  username: string
  userNumber: string
  email: string
  phoneNumber: string
  role: 'USER' | 'ADMIN'
  status: number // 1-正常, 0-禁用
  maxStorage: number
  usedStorage: number
  usagePercent: number
  createdAt: string
}

// 分页响应
export type PageResp<T> = {
  items: T[]
  page: number
  size: number
  total: number
}

// 系统概览数据
export type SystemOverview = {
  totalUsers: number
  activeUsers: number
  adminCount: number
  totalFiles: number
  totalFolders: number
  totalStorage: number
  todayLogins: number
  todayUploads: number
}

// 存储排行项
export type StorageRankingItem = {
  userId: number
  username: string
  userNumber: string
  usedStorage: number
  maxStorage: number
  usagePercent: number
}

// 存储统计
export type StorageStatistics = {
  totalStorage: number
  typeStatistics: Record<string, number>
  usersOver80: number
  users50to80: number
  users20to50: number
  usersUnder20: number
}

// 登录日志
export type LoginLog = {
  id: number
  userId: number
  userNumber: string
  username: string
  loginTime: string
  ipAddress: string
  userAgent: string
  loginStatus: 'SUCCESS' | 'FAILED'
  failureReason: string | null
}

// 登录统计
export type LoginStatistics = {
  totalSuccess: number
  totalFailed: number
  todaySuccess: number
  todayFailed: number
  activeUsers: number
  totalLogins: number
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

// ==================== 用户管理 ====================

/**
 * 获取用户列表
 */
export async function listUsers(params: {
  keyword?: string
  status?: number
  role?: string
  page?: number
  size?: number
}): Promise<PageResp<UserInfo>> {
  const q = new URLSearchParams()
  if (params.keyword) q.set('keyword', params.keyword)
  if (params.status != null) q.set('status', String(params.status))
  if (params.role) q.set('role', params.role)
  if (params.page) q.set('page', String(params.page))
  if (params.size) q.set('size', String(params.size))
  
  return request<PageResp<UserInfo>>(`/api/admin/users?${q.toString()}`)
}

/**
 * 更新用户存储空间
 */
export async function updateUserStorage(userId: number, maxStorage: number): Promise<{
  userId: number
  maxStorage: number
  usedStorage: number
}> {
  return request(`/api/admin/users/${userId}/storage?maxStorage=${maxStorage}`, {
    method: 'PUT',
  })
}

/**
 * 更新用户状态
 */
export async function updateUserStatus(userId: number, status: number): Promise<{
  userId: number
  status: number
}> {
  return request(`/api/admin/users/${userId}/status?status=${status}`, {
    method: 'PUT',
  })
}

/**
 * 更新用户角色
 */
export async function updateUserRole(userId: number, role: 'USER' | 'ADMIN'): Promise<{
  userId: number
  role: string
}> {
  return request(`/api/admin/users/${userId}/role?role=${role}`, {
    method: 'PUT',
  })
}

// ==================== 系统监控 ====================

/**
 * 获取系统概览
 */
export async function getSystemOverview(): Promise<SystemOverview> {
  return request<SystemOverview>('/api/admin/system/overview')
}

/**
 * 获取存储使用排行
 */
export async function getStorageRanking(limit: number = 10): Promise<StorageRankingItem[]> {
  return request<StorageRankingItem[]>(`/api/admin/system/storage/ranking?limit=${limit}`)
}

/**
 * 获取存储统计详情
 */
export async function getStorageStatistics(): Promise<StorageStatistics> {
  return request<StorageStatistics>('/api/admin/system/storage/statistics')
}

// ==================== 登录日志 ====================

/**
 * 获取登录日志列表
 */
export async function getLoginLogs(params: {
  userId?: number
  startDate?: string
  endDate?: string
  loginStatus?: 'SUCCESS' | 'FAILED'
  page?: number
  size?: number
}): Promise<PageResp<LoginLog>> {
  const q = new URLSearchParams()
  if (params.userId) q.set('userId', String(params.userId))
  if (params.startDate) q.set('startDate', params.startDate)
  if (params.endDate) q.set('endDate', params.endDate)
  if (params.loginStatus) q.set('loginStatus', params.loginStatus)
  if (params.page) q.set('page', String(params.page))
  if (params.size) q.set('size', String(params.size))
  
  return request<PageResp<LoginLog>>(`/api/admin/logs/login?${q.toString()}`)
}

/**
 * 获取登录统计
 */
export async function getLoginStatistics(params?: {
  startDate?: string
  endDate?: string
}): Promise<LoginStatistics> {
  const q = new URLSearchParams()
  if (params?.startDate) q.set('startDate', params.startDate)
  if (params?.endDate) q.set('endDate', params.endDate)
  
  const url = params ? `/api/admin/logs/login/statistics?${q.toString()}` : '/api/admin/logs/login/statistics'
  return request<LoginStatistics>(url)
}

// ==================== 管理员 Session 管理 ====================

/**
 * 激活管理员 Session
 * 从客户端 Session 验证用户是否为管理员，如果是，则激活管理员 Session
 */
export async function activateAdminSession(): Promise<{
  activated: boolean
  message: string
}> {
  return request<{
    activated: boolean
    message: string
  }>('/api/admin/activate', {
    method: 'POST',
  })
}

