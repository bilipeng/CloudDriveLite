<template>
    <el-upload
      action="/api/files/upload"           
      :headers="{ 'X-Requested-With': 'XMLHttpRequest' }"
      :with-credentials="true"             
      :show-file-list="false"              
      :data="uploadData"
      :on-success="handleSuccess"
      :on-error="handleError"
      
    >
      <el-button type="primary">
        上传文件
        <el-icon class="el-icon--right"><Upload /></el-icon>
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