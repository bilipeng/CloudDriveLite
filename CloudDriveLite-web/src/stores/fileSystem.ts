import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export type BreadcrumbItem = { id: number; name: string }

// 文件系统状态管理 Store
export const useFileSystemStore = defineStore('fileSystem', () => {
  // 当前文件夹 ID
  const currentFolderId = ref<number>(0)
  
  // 路径栈，用于记录导航历史
  const pathStack = ref<number[]>([0])
  
  // 面包屑导航数据（从 API 获取）
  const folderChain = ref<BreadcrumbItem[]>([{ id: 0, name: '根目录' }])

  // 设置当前文件夹 ID
  const setCurrentFolderId = (folderId: number) => {
    currentFolderId.value = folderId
  }

  // 设置面包屑导航数据
  const setFolderChain = (chain: BreadcrumbItem[]) => {
    folderChain.value = chain
  }

  // 进入文件夹
  const enterFolder = (folderId: number) => {
    pathStack.value.push(folderId)
    currentFolderId.value = folderId
  }

  // 返回上级文件夹
  const goBack = () => {
    if (pathStack.value.length > 1) {
      pathStack.value.pop()
      currentFolderId.value = pathStack.value[pathStack.value.length - 1]
      return currentFolderId.value
    }
    return null
  }

  // 跳转到指定路径（通过面包屑索引）
  const navigateToPath = (pathIndex: number) => {
    if (pathIndex >= 0 && pathIndex < folderChain.value.length) {
      const targetFolderId = folderChain.value[pathIndex].id
      // 截断路径栈到目标位置
      const targetIndex = pathStack.value.indexOf(targetFolderId)
      if (targetIndex >= 0) {
        pathStack.value = pathStack.value.slice(0, targetIndex + 1)
        currentFolderId.value = targetFolderId
        return targetFolderId
      } else {
        // 如果目标不在路径栈中，重置路径栈
        pathStack.value = [0]
        for (let i = 0; i <= pathIndex; i++) {
          pathStack.value.push(folderChain.value[i].id)
        }
        currentFolderId.value = targetFolderId
        return targetFolderId
      }
    }
    return null
  }

  // 重置到根目录
  const resetToRoot = () => {
    currentFolderId.value = 0
    pathStack.value = [0]
    folderChain.value = [{ id: 0, name: '根目录' }]
  }

  return {
    currentFolderId,
    pathStack,
    folderChain,
    setCurrentFolderId,
    setFolderChain,
    enterFolder,
    goBack,
    navigateToPath,
    resetToRoot
  }
})
