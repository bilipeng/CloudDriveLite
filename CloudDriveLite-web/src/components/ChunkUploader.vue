<template>
  <div class="chunk-uploader">
    <el-button type="primary" @click="pickerVisible = true">上传文件</el-button>

    <el-dialog v-model="pickerVisible" title="选择文件上传" width="640px">
      <!-- 上传区域（模态框内） -->
      <div 
        class="upload-area"
        :class="{ 'is-dragover': isDragover }"
        @drop="handleDrop"
        @dragover="handleDragover"
        @dragleave="handleDragleave"
      >
        <div class="upload-content">
          <el-icon size="48" color="#409EFF"><UploadFilled /></el-icon>
          <p class="upload-text">点击或拖拽文件到此处添加到队列</p>
          <p class="upload-hint">支持大文件上传和断点续传</p>
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
        <el-button @click="pauseAll" :disabled="!hasUploadingFiles">
          全部暂停
        </el-button>
        <el-button @click="resumeAll" :disabled="!hasPausedFiles">
          全部继续
        </el-button>
        <el-button @click="clearAll" type="danger">
          清空列表
        </el-button>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="pickerVisible = false">关闭</el-button>
          <el-button type="primary" :disabled="!hasPendingFiles" @click="startUpload">开始上传</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
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
      uploader.value = new Uploader({
        target: '/api/files/upload',
        chunkSize: props.chunkSize,
        simultaneousUploads: props.simultaneousUploads,
        testChunks: true,
        fileParameterName: 'file',
        query: {
          folderId: props.folderId
        },
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
      }
    }

    const handleDrop = (event) => {
      event.preventDefault()
      isDragover.value = false
      
      if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
        uploader.value?.addFiles(event.dataTransfer.files)
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

    // 生命周期
    onMounted(() => {
      initUploader()
    })

    onUnmounted(() => {
      if (uploader.value) {
        uploader.value.cancel()
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

.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #fafafa;
  position: relative;
}

.upload-area:hover {
  border-color: #409eff;
}

.upload-area.is-dragover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.upload-content {
  pointer-events: none;
}

.upload-text {
  font-size: 16px;
  color: #606266;
  margin: 16px 0 8px;
}

.upload-hint {
  font-size: 14px;
  color: #909399;
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
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  transition: all 0.3s ease;
}

.file-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.file-item.file-success {
  border-color: #67c23a;
  background-color: #f0f9eb;
}

.file-item.file-error {
  border-color: #f56c6c;
  background-color: #fef0f0;
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
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
</style>