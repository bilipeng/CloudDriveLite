<template>
  <el-upload
    action="/api/files/upload"           
    :headers="{ 'X-Requested-With': 'XMLHttpRequest' }"
    :with-credentials="true"             
    :show-file-list="false"              
    :data="uploadData"
    :on-success="handleSuccess"
    :on-error="handleError"
    class="modern-upload"
  >
    <el-button type="primary" class="upload-btn">
      <el-icon><Upload /></el-icon>
      <span>上传文件</span>
    </el-button>
  </el-upload>
</template>
  
<script setup lang="ts">
  import { ElMessage } from 'element-plus'
  import { Upload } from '@element-plus/icons-vue'
  import { computed } from 'vue'
  
  // 接收父组件传入的 folderId
  const props = defineProps<{
    folderId?: number
  }>()
  
  // 计算上传数据，包含 folderId
  const uploadData = computed(() => ({
    folderId: props.folderId || 0
  }))
  
  // 1. 声明事件
  const emit = defineEmits<{
    uploaded: []
  }>()
  
  // 成功回调
  const handleSuccess = () => {
    console.log('上传成功，folderId:', props.folderId)
    ElMessage.success('上传成功')
    emit('uploaded')   // ← 通知父组件
  }
  
  // 失败回调
  const handleError = (err: any) => {
    const msg = err?.response?.data?.message || '上传失败'
    ElMessage.error(msg)
  }
  </script>

<style scoped>
.modern-upload {
  display: inline-block;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.upload-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.upload-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
}

.upload-btn .el-icon {
  font-size: 16px;
}
</style>