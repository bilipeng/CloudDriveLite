<template>
  <div class="chunk-uploader">
    <el-button 
      type="primary" 
      @click="pickerVisible = true"
      class="chunk-upload-btn"
    >
      <el-icon><UploadFilled /></el-icon>
      <span>大文件上传</span>
    </el-button>

    <el-dialog 
      v-model="pickerVisible" 
      title="文件上传" 
      width="720px"
      class="upload-dialog"
      :close-on-click-modal="false"
    >
      <!-- 上传区域（模态框内） -->
      <div 
        class="upload-area"
        :class="{ 'is-dragover': isDragover }"
        @drop="handleDrop"
        @dragover="handleDragover"
        @dragleave="handleDragleave"
      >
        <div class="upload-content">
          <div class="upload-icon-wrapper">
            <el-icon size="64" class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-icon-bg"></div>
          </div>
          <p class="upload-text">点击或拖拽文件到此处</p>
          <p class="upload-hint">支持大文件上传、断点续传和批量上传</p>
          <el-button 
            type="primary" 
            size="default" 
            class="select-file-btn"
            @click="triggerFileInput"
          >
            <el-icon><FolderOpened /></el-icon>
            选择文件
          </el-button>
        </div>
        <input
          ref="fileInput"
          type="file"
          multiple
          :accept="accept"
          @change="handleFileSelect"
          class="file-input"
        />
      </div>

      <!-- 上传文件列表（模态框内） -->
      <div v-if="fileList.length > 0" class="file-list">
        <div
          v-for="file in fileList"
          :key="file.id"
          class="file-item"
          :class="{ 
            'file-success': file.status === 'success', 
            'file-error': file.status === 'error' 
          }"
        >
          <!-- 文件信息 -->
          <div class="file-info">
            <div class="file-name">{{ file.name }}</div>
            <div class="file-meta">
              <span class="file-size">{{ formatFileSize(file.size) }}</span>
              <span class="file-status" :class="`status-${file.status}`">
                {{ getStatusText(file.status) }}
              </span>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="file-progress">
            <el-progress 
              :percentage="Math.round(file.progress * 100)" 
              :status="getProgressStatus(file.status)"
              :show-text="file.status !== 'uploading'"
            />
            <div v-if="file.status === 'uploading'" class="progress-detail">
              分块 {{ file.chunkProgress.current }}/{{ file.chunkProgress.total }}
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="file-actions">
            <template v-if="file.status === 'paused'">
              <el-button size="small" @click="resumeFile(file)" type="primary">
                继续
              </el-button>
            </template>
            <template v-else-if="file.status === 'uploading'">
              <el-button size="small" @click="pauseFile(file)" :loading="file.pausing">
                暂停
              </el-button>
            </template>
            <template v-else-if="file.status === 'success'">
              <el-button size="small" type="success" disabled>
                完成
              </el-button>
            </template>
            <template v-else-if="file.status === 'error'">
              <el-button size="small" @click="retryFile(file)" type="warning">
                重试
              </el-button>
            </template>
            
            <el-button 
              size="small" 
              @click="removeFile(file)" 
              :disabled="file.status === 'uploading' && !file.pausing"
              type="danger"
            >
              移除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 全局控制（模态框内） -->
      <div v-if="fileList.length > 0" class="global-controls">
        <div class="controls-left">
          <el-button @click="pauseAll" :disabled="!hasUploadingFiles" size="small">
          全部暂停
        </el-button>
          <el-button @click="resumeAll" :disabled="!hasPausedFiles" size="small">
          全部继续
        </el-button>
          <el-button @click="clearAll" type="danger" size="small">
          清空列表
        </el-button>
        </div>
        <div class="controls-right">
          <span class="upload-summary">
            共 {{ fileList.length }} 个文件
            <template v-if="hasUploadingFiles || hasPausedFiles">
              · {{ fileList.filter(f => f.status === 'uploading' || f.status === 'paused').length }} 个上传中
            </template>
            <template v-if="fileList.filter(f => f.status === 'success').length > 0">
              · {{ fileList.filter(f => f.status === 'success').length }} 个已完成
            </template>
          </span>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCloseDialog">关闭</el-button>
          <el-button 
            v-if="hasPendingFiles" 
            type="primary" 
            @click="startUpload"
          >
            开始上传 ({{ fileList.filter(f => f.status === 'pending').length }})
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, FolderOpened } from '@element-plus/icons-vue'
import Uploader from 'simple-uploader.js'

export default {
  name: 'ChunkUploader',
  props: {
    folderId: {
      type: Number,
      default: 0
    },
    accept: {
      type: String,
      default: '*'
    },
    multiple: {
      type: Boolean,
      default: true
    },
    chunkSize: {
      type: Number,
      default: 2 * 1024 * 1024 // 2MB
    },
    simultaneousUploads: {
      type: Number,
      default: 3
    }
  },
  emits: ['uploaded', 'error'],
  setup(props, { emit }) {
    const fileInput = ref(null)
    const isDragover = ref(false)
    const uploader = ref(null)
    const fileList = ref([])
    const pickerVisible = ref(false)

    // 计算属性
    const hasPendingFiles = computed(() => 
      fileList.value.some(file => file.status === 'pending')
    )

    const hasUploadingFiles = computed(() => 
      fileList.value.some(file => file.status === 'uploading')
    )

    const hasPausedFiles = computed(() => 
      fileList.value.some(file => file.status === 'paused')
    )

    // 初始化上传器
    const initUploader = () => {
      // 如果已存在上传器，先清理
      if (uploader.value) {
        try {
          uploader.value.cancel()
          // simple-uploader.js 可能没有 destroy 方法，使用 cancel 和移除事件监听
          if (typeof uploader.value.off === 'function') {
            uploader.value.off()
          }
        } catch (e) {
          console.warn('清理上传器时出错:', e)
        }
      }
      
      uploader.value = new Uploader({
        target: '/api/files/upload',
        chunkSize: props.chunkSize,
        simultaneousUploads: props.simultaneousUploads,
        testChunks: true,
        fileParameterName: 'file',
        query: () => ({
          folderId: props.folderId || 0
        }),
        headers: {
          'X-Requested-With': 'XMLHttpRequest'
        },
        withCredentials: true,
        forceChunkSize: true,
        progressCallbacksInterval: 100,
        maxChunkRetries: 3,
        chunkRetryInterval: 1000
      })

      // 绑定事件
      uploader.value.on('fileAdded', handleFileAdded)
      uploader.value.on('fileProgress', handleFileProgress)
      uploader.value.on('fileSuccess', handleFileSuccess)
      uploader.value.on('fileError', handleFileError)
      uploader.value.on('fileRetry', handleFileRetry)
      uploader.value.on('complete', handleComplete)
      // 绑定暂停/恢复事件（如果不支持，依赖 handleFileProgress 中的状态同步）
      uploader.value.on('filePaused', handleFilePaused)
      uploader.value.on('fileResumed', handleFileResumed)
    }

    // 事件处理函数
    const handleFileAdded = (file) => {
      const uploadFile = {
        id: file.uniqueIdentifier,
        name: file.name,
        size: file.size,
        progress: 0,
        status: 'pending',
        pausing: false,
        file: file,
        chunkProgress: {
          current: 0,
          // simple-uploader 的 file.chunks 为数组，直接渲染会输出大量对象信息，这里用数值总数
          total: Math.max(1, Math.ceil(file.size / (typeof file.chunkSize === 'number' && file.chunkSize > 0 ? file.chunkSize : props.chunkSize)))
        }
      }
      fileList.value.push(uploadFile)
      ElMessage.success(`已添加文件: ${file.name}`)
    }

    const handleFileProgress = (file) => {
      const uploadFile = fileList.value.find(f => f.id === file.uniqueIdentifier)
      if (uploadFile) {
        uploadFile.progress = file.progress()
        // 用整体进度推算已完成分片数，避免将分片对象数组渲染到界面
        uploadFile.chunkProgress.current = Math.min(
          uploadFile.chunkProgress.total,
          Math.max(0, Math.round(uploadFile.progress * uploadFile.chunkProgress.total))
        )
        
        // 同步暂停状态（作为备用，主要依赖 filePaused/fileResumed 事件）
        if (file.paused !== undefined) {
          if (file.paused && uploadFile.status === 'uploading') {
            uploadFile.status = 'paused'
            uploadFile.pausing = false
          } else if (!file.paused && uploadFile.status === 'paused' && uploadFile.progress < 1) {
            uploadFile.status = 'uploading'
          }
        }
      }
    }

    const handleFileSuccess = (file, response) => {
      const uploadFile = fileList.value.find(f => f.id === file.uniqueIdentifier)
      if (uploadFile) {
        uploadFile.status = 'success'
        uploadFile.progress = 1
        uploadFile.chunkProgress.current = uploadFile.chunkProgress.total
        
        try {
          const result = JSON.parse(response)
          ElMessage.success(`文件上传成功: ${file.name}`)
          emit('uploaded', result.data)
        } catch (e) {
          ElMessage.success(`文件上传成功: ${file.name}`)
        }
      }
    }

    const extractErrorMessage = (message, chunk) => {
      if (typeof message === 'string') return message
      if (message && typeof message === 'object') {
        if (message.message) return String(message.message)
        if (message.responseText) return String(message.responseText)
        if (message.response) return String(message.response)
      }
      const status = chunk?.xhr?.status
      const statusText = chunk?.xhr?.statusText
      if (status) return `HTTP ${status}${statusText ? ' ' + statusText : ''}`
      try {
        return JSON.stringify(message)
      } catch {
        return String(message)
      }
    }

    const handleFileError = (file, message, chunk) => {
      const uploadFile = fileList.value.find(f => f.id === file.uniqueIdentifier)
      if (uploadFile) {
        uploadFile.status = 'error'
        const reason = extractErrorMessage(message, chunk)
        ElMessage.error(`文件上传失败: ${file.name} - ${reason}`)
        emit('error', new Error(reason))
      }
    }

    const handleFileRetry = (file) => {
      const uploadFile = fileList.value.find(f => f.id === file.uniqueIdentifier)
      if (uploadFile) {
        uploadFile.status = 'uploading'
        ElMessage.info(`重试上传: ${file.name}`)
      }
    }

    const handleFilePaused = (file) => {
      const uploadFile = fileList.value.find(f => f.id === file.uniqueIdentifier)
      if (uploadFile) {
        uploadFile.status = 'paused'
        uploadFile.pausing = false
      }
    }

    const handleFileResumed = (file) => {
      const uploadFile = fileList.value.find(f => f.id === file.uniqueIdentifier)
      if (uploadFile) {
        uploadFile.status = 'uploading'
      }
    }

    const handleComplete = () => {
      ElMessage.success('所有文件上传完成')
    }

    // 用户交互方法
    const triggerFileInput = () => {
      fileInput.value?.click()
    }

    const handleFileSelect = (event) => {
      const input = event.target
      if (input.files && input.files.length > 0) {
        uploader.value?.addFiles(input.files)
        input.value = '' // 重置input
        // 自动开始上传新添加的文件
        setTimeout(() => {
          startUpload()
        }, 100)
      }
    }

    const handleDrop = (event) => {
      event.preventDefault()
      isDragover.value = false
      
      if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
        uploader.value?.addFiles(event.dataTransfer.files)
        // 自动开始上传拖拽的文件
        setTimeout(() => {
          startUpload()
        }, 100)
      }
    }

    const handleDragover = (event) => {
      event.preventDefault()
      isDragover.value = true
    }

    const handleDragleave = (event) => {
      event.preventDefault()
      isDragover.value = false
    }

    // 文件操作
    const startUpload = () => {
      if (uploader.value) {
        uploader.value.upload()
        fileList.value.forEach(file => {
          if (file.status === 'pending') {
            file.status = 'uploading'
          }
        })
      }
    }

    const pauseFile = (uploadFile) => {
      uploadFile.pausing = true
      if (uploadFile.file && typeof uploadFile.file.pause === 'function') {
        uploadFile.file.pause()
      } else {
        // 如果文件对象没有 pause 方法，尝试通过 uploader 暂停
        uploader.value?.pause()
      }
      // 延迟重置 pausing 状态，确保暂停操作完成
      setTimeout(() => {
        uploadFile.pausing = false
        // 如果文件确实已暂停，更新状态
        if (uploadFile.file && uploadFile.file.paused) {
          uploadFile.status = 'paused'
        }
      }, 300)
    }

    const resumeFile = (uploadFile) => {
      if (uploadFile.file && typeof uploadFile.file.resume === 'function') {
        uploadFile.file.resume()
      } else {
        // 如果文件对象没有 resume 方法，尝试通过 uploader 恢复
        uploader.value?.resume()
      }
      // 状态会在 handleFileResumed 事件中更新，这里先设置
      uploadFile.status = 'uploading'
    }

    const removeFile = async (uploadFile) => {
      try {
        await ElMessageBox.confirm(
          `确定要移除文件 "${uploadFile.name}" 吗？`,
          '确认移除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        uploader.value?.removeFile(uploadFile.file)
        fileList.value = fileList.value.filter(f => f.id !== uploadFile.id)
        ElMessage.success('文件已移除')
      } catch {
        // 用户取消
      }
    }

    const retryFile = (uploadFile) => {
      uploadFile.file.retry()
      uploadFile.status = 'uploading'
    }

    // 批量操作
    const pauseAll = () => {
      uploader.value?.pause()
      fileList.value.forEach(file => {
        if (file.status === 'uploading') {
          file.status = 'paused'
        }
      })
    }

    const resumeAll = () => {
      uploader.value?.resume()
      fileList.value.forEach(file => {
        if (file.status === 'paused') {
          file.status = 'uploading'
        }
      })
    }

    const clearAll = async () => {
      if (fileList.value.length === 0) return
      
      try {
        await ElMessageBox.confirm(
          '确定要清空所有文件吗？',
          '确认清空',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        uploader.value?.cancel()
        fileList.value = []
        ElMessage.success('已清空文件列表')
      } catch {
        // 用户取消
      }
    }

    // 关闭对话框时的处理
    const handleCloseDialog = () => {
      // 如果有正在上传的文件，提示用户
      const uploadingFiles = fileList.value.filter(f => f.status === 'uploading' || f.status === 'paused')
      if (uploadingFiles.length > 0) {
        ElMessageBox.confirm(
          `还有 ${uploadingFiles.length} 个文件正在上传中，关闭对话框后上传将继续在后台进行。确定要关闭吗？`,
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
        ).then(() => {
          pickerVisible.value = false
        }).catch(() => {})
      } else {
        pickerVisible.value = false
      }
    }

    // 工具函数
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const getStatusText = (status) => {
      const statusMap = {
        pending: '等待上传',
        uploading: '上传中',
        paused: '已暂停',
        success: '上传成功',
        error: '上传失败'
      }
      return statusMap[status] || status
    }

    const getProgressStatus = (status) => {
      const statusMap = {
        success: 'success',
        error: 'exception',
        uploading: undefined,
        paused: 'warning',
        pending: undefined
      }
      return statusMap[status]
    }

    // 监听 folderId 变化
    watch(() => props.folderId, (newFolderId, oldFolderId) => {
      if (newFolderId !== oldFolderId && uploader.value) {
        console.log('folderId 已更新:', oldFolderId, '->', newFolderId)
        // query 已经是函数形式，会自动获取最新的 folderId
        // 但如果上传器已经初始化，我们需要确保它使用新的 folderId
        // simple-uploader.js 的 query 函数会在每次请求时调用，所以这里不需要额外操作
      }
    })

    // 生命周期
    onMounted(() => {
      initUploader()
    })

    onUnmounted(() => {
      if (uploader.value) {
        try {
          // 取消所有上传
        uploader.value.cancel()
          // 移除所有事件监听
          if (typeof uploader.value.off === 'function') {
            uploader.value.off()
          }
          // 清空文件列表
          fileList.value = []
        } catch (e) {
          console.warn('组件卸载时清理上传器出错:', e)
        } finally {
          uploader.value = null
        }
      }
    })

    return {
      fileInput,
      isDragover,
      fileList,
      pickerVisible,
      hasPendingFiles,
      hasUploadingFiles,
      hasPausedFiles,
      triggerFileInput,
      handleFileSelect,
      handleDrop,
      handleDragover,
      handleDragleave,
      startUpload,
      handleCloseDialog,
      pauseFile,
      resumeFile,
      removeFile,
      retryFile,
      pauseAll,
      resumeAll,
      clearAll,
      formatFileSize,
      getStatusText,
      getProgressStatus
    }
  }
}
</script>

<style scoped>
.chunk-uploader {
  width: 100%;
}

.chunk-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chunk-upload-btn:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4);
}

.chunk-upload-btn:active {
  transform: translateY(0);
}

.upload-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.upload-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.upload-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.upload-area {
  border: 2px dashed #cbd5e1;
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  cursor: pointer;
}

.upload-area::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.05) 0%, transparent 70%);
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1) rotate(0deg); opacity: 0.5; }
  50% { transform: scale(1.1) rotate(180deg); opacity: 0.8; }
}

.upload-area:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);
}

.upload-area.is-dragover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-width: 3px;
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.25);
  transform: scale(1.02);
}

.upload-content {
  pointer-events: none;
  position: relative;
  z-index: 1;
}

.upload-icon-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 24px;
}

.upload-icon {
  position: relative;
  z-index: 2;
  color: #3b82f6;
  filter: drop-shadow(0 4px 12px rgba(59, 130, 246, 0.3));
}

.upload-icon-bg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  border-radius: 50%;
  animation: ripple 2s ease-in-out infinite;
}

@keyframes ripple {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.3; }
}

.upload-text {
  margin: 24px 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.upload-hint {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
}

.select-file-btn {
  pointer-events: auto;
  margin-top: 8px;
  border-radius: 10px;
  padding: 10px 24px;
  font-weight: 500;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}

.select-file-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.file-input {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  opacity: 0;
  cursor: pointer;
}

.file-list {
  margin-top: 20px;
}

.file-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.file-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
  border-color: #cbd5e1;
}

.file-item.file-success {
  border-color: #10b981;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.15);
}

.file-item.file-error {
  border-color: #ef4444;
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.15);
}

.file-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.file-name {
  font-weight: 500;
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.file-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  margin-left: 12px;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.file-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f4f4f5;
  color: #909399;
}

.file-status.status-uploading {
  background: #ecf5ff;
  color: #409eff;
}

.file-status.status-success {
  background: #f0f9eb;
  color: #67c23a;
}

.file-status.status-error {
  background: #fef0f0;
  color: #f56c6c;
}

.file-status.status-paused {
  background: #fdf6ec;
  color: #e6a23c;
}

.file-progress {
  margin-bottom: 12px;
}

.progress-detail {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-top: 4px;
}

.file-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.global-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
  gap: 16px;
}

.controls-left {
  display: flex;
  gap: 8px;
}

.controls-right {
  flex: 1;
  text-align: right;
}

.upload-summary {
  color: #606266;
  font-size: 14px;
}
</style>