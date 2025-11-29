export type FileItem = {
  id: number
  parentId: number
  fileName: string
  fileType: string
  folder: boolean
  fileSize: number
  fileSizeFormatted: string
  uploadedTime: string
  downloadUrl: string
  previewUrl: string
  isImage: boolean
}

import { handleUnauthorized, isUnauthorizedResponse } from '@/utils/auth'

export type PageResp<T> = {
  items: T[]
  page: number
  size: number
  total: number
}

export async function listFiles(params: { folderId?: number; page?: number; size?: number }) {
  const q = new URLSearchParams()
  if (params.folderId != null) q.set('folderId', String(params.folderId))
  if (params.page != null) q.set('page', String(params.page))
  if (params.size != null) q.set('size', String(params.size))
  const res = await fetch(`/api/files?${q.toString()}`, { credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '加载失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  
  // 转换后端格式到前端期望格式
  const data = json.data
  return {
    items: data.items || [],
    page: data.page || 1,
    size: data.size || 20,
    total: data.total || 0
  } as PageResp<FileItem>
}

export type BreadcrumbItem = { id: number; name: string }

// 创建文件夹
export async function createFolder(params: { folderName: string; parentId?: number }) {
  const form = new FormData()
  form.append('folderName', params.folderName)
  if (params.parentId != null) form.append('parentId', String(params.parentId))
  const res = await fetch('/api/files/folder', { method: 'POST', body: form, credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '创建文件夹失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data as FileItem
}

// 重命名
export async function renameItem(id: number, newName: string) {
  const form = new FormData()
  form.append('newName', newName)
  const res = await fetch(`/api/files/${id}/rename`, { method: 'PUT', body: form, credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '重命名失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data as FileItem
}

// 移动
export async function moveItem(id: number, targetParentId: number) {
  const form = new FormData()
  form.append('targetParentId', String(targetParentId))
  const res = await fetch(`/api/files/${id}/move`, { method: 'PUT', body: form, credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '移动失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data as FileItem
}

// 删除（支持递归）
export async function deleteItem(id: number, recursive: boolean = false) {
  const q = new URLSearchParams()
  if (recursive) q.set('recursive', 'true')
  const res = await fetch(`/api/files/${id}?${q.toString()}`, { method: 'DELETE', credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '删除失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data
}

// 面包屑
export async function getBreadcrumb(folderId: number = 0) {
  const q = new URLSearchParams()
  if (folderId !== 0) q.set('folderId', String(folderId))
  const res = await fetch(`/api/files/breadcrumb?${q.toString()}`, { credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '获取路径失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data as BreadcrumbItem[]
}

// 获取 RawBox 预览链接
export async function getRawBoxPreviewUrl(id: number) {
  const res = await fetch(`/api/files/${id}/rawbox-preview`, { credentials: 'include' })
  const json = await res.json()
  if (!json.success) {
    const message = json.message || '获取预览链接失败'
    if (isUnauthorizedResponse(message) || res.status === 401) {
      handleUnauthorized(message)
      throw new Error(message)
    }
    throw new Error(message)
  }
  return json.data as { previewUrl: string; fileName: string; fileType: string; rawboxFileName: string }
}
