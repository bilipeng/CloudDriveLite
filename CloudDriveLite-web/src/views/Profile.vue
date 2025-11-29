<template>
  <div class="profile-page">
    <div class="page-header">
      <div class="header-left">
        <el-button 
          :icon="ArrowLeft" 
          circle
          class="back-btn"
          @click="goBack"
        />
        <el-breadcrumb separator="/" class="page-breadcrumb">
          <el-breadcrumb-item>
            <span @click="goBack" style="cursor: pointer; display: inline-flex; align-items: center; gap: 4px;">
              <el-icon><HomeFilled /></el-icon>
              首页
            </span>
          </el-breadcrumb-item>
          <el-breadcrumb-item>个人中心</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <h2>个人中心</h2>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：个人信息 -->
      <el-col :xs="24" :md="16">
        <el-card class="info-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
              <el-button type="primary" :icon="Edit" @click="handleEditInfo">编辑</el-button>
            </div>
          </template>
          
          <el-form :model="userInfo" label-width="120px" class="info-form">
            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" :disabled="!editing" />
            </el-form-item>
            <el-form-item label="用户号">
              <el-input v-model="userInfo.userNumber" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="userInfo.phoneNumber" :disabled="!editing" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="userInfo.email" :disabled="!editing" />
            </el-form-item>
            <el-form-item label="注册时间">
              <el-input :value="formatDateTime(userInfo.createdAt)" disabled />
            </el-form-item>
            <el-form-item v-if="editing">
              <el-button type="primary" @click="handleSaveInfo" :loading="saving">保存</el-button>
              <el-button @click="handleCancelEdit">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card class="password-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>
          
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input 
                v-model="passwordForm.oldPassword" 
                type="password" 
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password" 
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input 
                v-model="passwordForm.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="changingPassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：存储空间 -->
      <el-col :xs="24" :md="8">
        <el-card class="storage-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>存储空间</span>
              <el-button :icon="Refresh" circle @click="loadStorageInfo" :loading="loadingStorage" />
            </div>
          </template>
          
          <div class="storage-content">
            <div class="storage-icon-wrapper">
              <el-icon :size="64" color="#667eea"><Box /></el-icon>
            </div>
            <div class="storage-info">
              <div class="storage-label">已使用</div>
              <div class="storage-value primary">{{ formatStorage(storageInfo.usedStorage) }}</div>
            </div>
            <div class="storage-info">
              <div class="storage-label">总空间</div>
              <div class="storage-value">{{ formatStorage(storageInfo.maxStorage) }}</div>
            </div>
            <div class="storage-info">
              <div class="storage-label">剩余空间</div>
              <div class="storage-value success">{{ formatStorage(storageInfo.maxStorage - storageInfo.usedStorage) }}</div>
            </div>
            
            <el-progress 
              :percentage="Math.round(storageInfo.usagePercent)" 
              :color="getStorageColor(storageInfo.usagePercent)"
              :stroke-width="12"
              class="storage-progress"
            />
            
            <div class="storage-percent">
              使用率：{{ Math.round(storageInfo.usagePercent) }}%
            </div>
          </div>
        </el-card>

        <!-- 账户信息 -->
        <el-card class="account-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>账户信息</span>
            </div>
          </template>
          
          <div class="account-info">
            <div class="account-item">
              <div class="account-label">用户角色</div>
              <el-tag :type="roleTagType" size="large">
                {{ userInfo.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
            </div>
            <div class="account-item">
              <div class="account-label">账户状态</div>
              <el-tag :type="userInfo.status === 1 ? 'success' : 'info'" size="large">
                {{ userInfo.status === 1 ? '正常' : '已禁用' }}
              </el-tag>
            </div>
            <div v-if="userInfo.role === 'ADMIN'" class="account-item admin-action">
              <el-button 
                type="primary" 
                :icon="Setting" 
                @click="goToAdmin"
                style="width: 100%;"
              >
                进入管理后台
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onActivated, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Edit, Refresh, Box, ArrowLeft, HomeFilled, Setting } from '@element-plus/icons-vue'
import { 
  getCurrentUserInfo, 
  updateUserInfo, 
  changePassword,
  getCurrentUserStorage, 
  type UserInfo,
  type UserStorageInfo 
} from '@/api/user'
import { authApi } from '@/api/auth'
import { activateAdminSession } from '@/api/admin'

const router = useRouter()
const route = useRoute()

// 用户信息
const userInfo = ref<UserInfo>({
  id: 0,
  username: '',
  userNumber: '',
  phoneNumber: '',
  email: '',
  role: 'USER',
  status: 1,
  createdAt: ''
})

// 计算属性：用于 ElTag 的 type
const roleTagType = computed(() => {
  return userInfo.value.role === 'ADMIN' ? 'danger' : undefined
})

// 编辑状态
const editing = ref(false)
const saving = ref(false)
const originalUserInfo = ref({})

// 密码表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordFormRef = ref<FormInstance>()
const changingPassword = ref(false)

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 存储信息
const storageInfo = ref<UserStorageInfo>({
  userId: 0,
  username: '',
  userNumber: '',
  maxStorage: 0,
  usedStorage: 0,
  usagePercent: 0
})

const loadingStorage = ref(false)

// 格式化存储大小
function formatStorage(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化日期时间
function formatDateTime(dateTime: string): string {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取存储进度条颜色
function getStorageColor(percent: number): string {
  if (percent >= 90) return '#f56c6c'
  if (percent >= 70) return '#e6a23c'
  return '#67c23a'
}

// 加载用户信息
async function loadUserInfo() {
  try {
    const userData = await getCurrentUserInfo()
    userInfo.value = userData
    // 更新 localStorage 中的用户名
    if (userData.username) {
      localStorage.setItem('userName', userData.username)
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    // 不显示错误消息，因为可能是未登录，会被自动跳转
    // 如果加载失败，使用 localStorage 中的基本信息作为降级方案
    const userName = localStorage.getItem('userName') || ''
    const userNumber = localStorage.getItem('userNumber') || ''
    if (userName || userNumber) {
      userInfo.value = {
        ...userInfo.value,
        username: userName,
        userNumber: userNumber
      }
    }
  }
}

// 加载存储信息
async function loadStorageInfo() {
  loadingStorage.value = true
  try {
    const info = await getCurrentUserStorage()
    storageInfo.value = info
  } catch (error: any) {
    ElMessage.error(error.message || '加载存储信息失败')
  } finally {
    loadingStorage.value = false
  }
}

// 编辑信息
function handleEditInfo() {
  editing.value = true
  originalUserInfo.value = { ...userInfo.value }
}

// 取消编辑
function handleCancelEdit() {
  editing.value = false
  userInfo.value = { ...originalUserInfo.value } as any
}

// 保存信息
async function handleSaveInfo() {
  saving.value = true
  try {
    await updateUserInfo({
      username: userInfo.value.username,
      phoneNumber: userInfo.value.phoneNumber,
      email: userInfo.value.email
    })
    ElMessage.success('保存成功')
    editing.value = false
    // 重新加载用户信息
    await loadUserInfo()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    changingPassword.value = true
    
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    passwordFormRef.value.resetFields()
    
    // 延迟跳转到登录页
    setTimeout(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('userNumber')
      localStorage.removeItem('userName')
      router.push('/login')
    }, 1500)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '密码修改失败')
    }
  } finally {
    changingPassword.value = false
  }
}

// 返回上一页
function goBack() {
  console.log('点击返回按钮')
  router.push('/files').then(() => {
    console.log('导航成功')
  }).catch((err) => {
    // 忽略导航重复的错误
    if (err.name !== 'NavigationDuplicated') {
      console.error('导航失败:', err)
      // 如果导航失败，尝试使用 replace
      router.replace('/files').catch(() => {
        // 最后的降级方案：使用 window.location
        window.location.href = '/files'
      })
    }
  })
}

// 跳转到管理后台
async function goToAdmin() {
  try {
    // 先激活管理员 Session
    await activateAdminSession()
    // 激活成功后跳转
    router.push('/admin/dashboard').catch((err) => {
      if (err.name !== 'NavigationDuplicated') {
        console.error('导航失败:', err)
        router.replace('/admin/dashboard').catch(() => {
          window.location.href = '/admin/dashboard'
        })
      }
    })
  } catch (error: any) {
    // 如果激活失败，可能是未登录或不是管理员
    ElMessage.error(error.message || '无法进入管理后台')
    console.error('激活管理员 Session 失败:', error)
  }
}

// 加载状态标志，防止重复加载
const isLoading = ref(false)

// 加载所有数据
async function loadAllData() {
  // 防止重复加载
  if (isLoading.value) {
    console.log('数据正在加载中，跳过重复请求')
    return
  }
  
  console.log('开始加载个人中心数据...')
  isLoading.value = true
  try {
    await Promise.all([
      loadUserInfo(),
      loadStorageInfo()
    ])
    console.log('个人中心数据加载完成')
  } catch (error) {
    console.error('加载数据时出错:', error)
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时加载数据
onMounted(async () => {
  console.log('Profile 组件已挂载，路径:', route.path, '完整路径:', route.fullPath)
  await nextTick()
  await loadAllData()
})

// 监听路由变化，确保每次进入页面都加载数据
watch(
  () => route.fullPath,
  async (newPath, oldPath) => {
    console.log('路由变化:', oldPath, '->', newPath)
    if (newPath === '/profile' || newPath.startsWith('/profile')) {
      try {
        await nextTick()
        await loadAllData()
      } catch (error) {
        console.error('路由监听中加载数据失败:', error)
      }
    }
  },
  { immediate: true }
)

// 如果使用了 keep-alive，使用 onActivated
onActivated(async () => {
  console.log('Profile 组件已激活')
  await nextTick()
  await loadAllData()
})
</script>

<style scoped>
.profile-page {
  padding: 32px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  min-height: calc(100vh - 70px);
}

.page-header {
  margin-bottom: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  transition: all 0.2s ease;
}

.back-btn:hover {
  transform: translateX(-2px);
  background: #f1f5f9;
}

.page-breadcrumb {
  font-size: 14px;
}

.page-breadcrumb :deep(.el-breadcrumb__item) {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.info-card,
.password-card,
.storage-card,
.account-card {
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s ease;
  background: #ffffff;
}

.info-card:hover,
.password-card:hover,
.storage-card:hover,
.account-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
  padding-bottom: 4px;
}

.info-form {
  max-width: 600px;
}

/* 存储空间卡片 */
.storage-content {
  text-align: center;
}

.storage-icon-wrapper {
  margin-bottom: 28px;
  display: flex;
  justify-content: center;
}

.storage-icon-wrapper .el-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.storage-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.2s ease;
}

.storage-info:hover {
  background: #f8fafc;
  margin: 0 -12px;
  padding: 16px 12px;
  border-radius: 8px;
}

.storage-info:last-of-type {
  border-bottom: none;
  margin-bottom: 24px;
}

.storage-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.storage-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.storage-value.primary {
  color: #3b82f6;
}

.storage-value.success {
  color: #10b981;
}

.storage-progress {
  margin: 24px 0;
}

.storage-progress :deep(.el-progress-bar__outer) {
  border-radius: 10px;
  overflow: hidden;
}

.storage-percent {
  font-size: 15px;
  color: #475569;
  font-weight: 600;
  margin-top: 8px;
}

/* 账户信息 */
.account-info {
  padding: 8px 0;
}

.account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.2s ease;
}

.account-item:hover {
  background: #f8fafc;
  margin: 0 -12px;
  padding: 18px 12px;
  border-radius: 8px;
}

.account-item:last-child {
  border-bottom: none;
}

.account-item.admin-action {
  margin-top: 16px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
  border-bottom: none;
  justify-content: center;
}

.account-item.admin-action:hover {
  background: transparent;
  margin: 16px -12px 0;
  padding-top: 24px;
}

.account-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }
}
</style>

