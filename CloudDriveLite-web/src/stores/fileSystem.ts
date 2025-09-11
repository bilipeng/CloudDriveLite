import { ref, computed } from 'vue'

type FileItem = { 
  id: string
  name: string
  updatedAt: string
  size: string
  type: 'folder' | 'file'
  fileType?: 'image' | 'video' | 'document' | 'other'
}

// 全局文件系统状态
export const useFileSystemStore = () => {
  // 当前路径栈，用于记录导航历史
  const currentPath = ref<string[]>(['root'])
  const currentFolderId = ref('root')

  // 获取文件夹链条用于面包屑导航
  const folderChain = computed(() => {
    const chain = []
    
    // 从当前路径构建链条
    for (let i = 0; i < currentPath.value.length; i++) {
      const folderId = currentPath.value[i]
      if (folderId === 'root') {
        chain.push({ id: 'root', name: '首页' })
      } else {
        // 从父级文件夹中找到当前文件夹的名称
        const parentId = i > 0 ? currentPath.value[i - 1] : 'root'
        const parentFiles = mockFileSystem[parentId] || []
        const folder = parentFiles.find(f => f.id === folderId)
        if (folder) {
          chain.push({ id: folderId, name: folder.name })
        }
      }
    }
    
    // 如果当前不在根目录，只返回路径中除了根目录的部分
    return chain.slice(1)
  })

  // 进入文件夹
  const enterFolder = (folderId: string) => {
    currentPath.value.push(folderId)
    currentFolderId.value = folderId
  }

  // 返回上级文件夹
  const goBack = () => {
    if (currentPath.value.length > 1) {
      currentPath.value.pop()
      currentFolderId.value = currentPath.value[currentPath.value.length - 1]
    }
  }

  // 跳转到指定路径
  const navigateToPath = (pathIndex: number) => {
    // pathIndex 是面包屑中的索引，需要加1因为面包屑不包含根目录
    const actualIndex = pathIndex + 1
    if (actualIndex >= 0 && actualIndex < currentPath.value.length) {
      currentPath.value = currentPath.value.slice(0, actualIndex + 1)
      currentFolderId.value = currentPath.value[actualIndex]
    }
  }

  return {
    currentPath,
    currentFolderId,
    folderChain,
    enterFolder,
    goBack,
    navigateToPath
  }
}

// 模拟的文件夹层级数据
export const mockFileSystem: Record<string, FileItem[]> = {
  'root': [
    { id: 'folder1', name: '本地存储演示', updatedAt: '2024-01-15', size: '0 B', type: 'folder' as const },
    { id: 'folder2', name: 'test', updatedAt: '2024-01-14', size: '0 B', type: 'folder' as const },
    { id: 'folder3', name: 'test2', updatedAt: '2024-01-13', size: '0 B', type: 'folder' as const },
    { id: 'file1', name: 'readme.txt', updatedAt: '2024-01-12', size: '1.2 KB', type: 'file' as const, fileType: 'document' as const },
  ],
  'folder1': [
    { id: 'folder1-1', name: '图片文件夹', updatedAt: '2024-01-15', size: '0 B', type: 'folder' as const },
    { id: 'folder1-2', name: '文档文件夹', updatedAt: '2024-01-14', size: '0 B', type: 'folder' as const },
    { id: 'file1-1', name: '项目说明.docx', updatedAt: '2024-01-13', size: '15.6 KB', type: 'file' as const, fileType: 'document' as const },
    { id: 'file1-2', name: '会议记录.pdf', updatedAt: '2024-01-12', size: '2.3 MB', type: 'file' as const, fileType: 'document' as const },
  ],
  'folder2': [
    { id: 'file2-1', name: '测试图片.jpg', updatedAt: '2024-01-14', size: '856 KB', type: 'file' as const, fileType: 'image' as const },
    { id: 'file2-2', name: '演示视频.mp4', updatedAt: '2024-01-13', size: '45.2 MB', type: 'file' as const, fileType: 'video' as const },
    { id: 'file2-3', name: '配置文件.json', updatedAt: '2024-01-12', size: '0.8 KB', type: 'file' as const, fileType: 'document' as const },
  ],
  'folder3': [
    { id: 'folder3-1', name: '子文件夹A', updatedAt: '2024-01-13', size: '0 B', type: 'folder' as const },
    { id: 'folder3-2', name: '子文件夹B', updatedAt: '2024-01-12', size: '0 B', type: 'folder' as const },
    { id: 'file3-1', name: '数据文件.csv', updatedAt: '2024-01-11', size: '3.4 KB', type: 'file' as const, fileType: 'document' as const },
  ],
  'folder1-1': [
    { id: 'file1-1-1', name: '风景照1.jpg', updatedAt: '2024-01-15', size: '2.1 MB', type: 'file' as const, fileType: 'image' as const },
    { id: 'file1-1-2', name: '风景照2.png', updatedAt: '2024-01-14', size: '1.8 MB', type: 'file' as const, fileType: 'image' as const },
    { id: 'file1-1-3', name: '头像.jpg', updatedAt: '2024-01-13', size: '156 KB', type: 'file' as const, fileType: 'image' as const },
  ],
  'folder1-2': [
    { id: 'file1-2-1', name: '技术文档.md', updatedAt: '2024-01-14', size: '5.2 KB', type: 'file' as const, fileType: 'document' as const },
    { id: 'file1-2-2', name: 'API接口文档.pdf', updatedAt: '2024-01-13', size: '1.2 MB', type: 'file' as const, fileType: 'document' as const },
  ],
  'folder3-1': [
    { id: 'file3-1-1', name: '备份文件.zip', updatedAt: '2024-01-13', size: '12.5 MB', type: 'file' as const, fileType: 'other' as const },
    { id: 'file3-1-2', name: '日志文件.log', updatedAt: '2024-01-12', size: '0.5 KB', type: 'file' as const, fileType: 'document' as const },
  ],
  'folder3-2': [
    { id: 'file3-2-1', name: '临时文件.tmp', updatedAt: '2024-01-12', size: '0 B', type: 'file' as const, fileType: 'other' as const },
  ]
}
