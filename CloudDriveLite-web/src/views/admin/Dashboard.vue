<template>
  <div class="admin-dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-breadcrumb separator="/" class="page-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/files' }">
            <el-icon><HomeFilled /></el-icon>
            首页
          </el-breadcrumb-item>
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>系统概览</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <h2>系统概览</h2>
        <el-button type="primary" :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <!-- 数据卡片区域 -->
    <el-row :gutter="20" class="stats-cards">
      <!-- 用户统计 -->
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-change positive">+12 本月</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-icon">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-change positive">82% 活跃率</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon admin-icon">
              <el-icon><Avatar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.adminCount }}</div>
              <div class="stat-label">管理员</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon file-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalFiles.toLocaleString() }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-change positive">+234 今日</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 存储统计 -->
    <el-row :gutter="20" class="storage-row">
      <el-col :xs="24" :md="12">
        <el-card class="storage-card">
          <template #header>
            <div class="card-header">
              <span>存储使用情况</span>
            </div>
          </template>
          <div class="storage-info">
            <div class="storage-total">
              <div class="storage-label">总存储空间</div>
              <div class="storage-value">{{ formatStorage(overview.totalStorage) }}</div>
            </div>
            <div class="storage-used">
              <div class="storage-label">今日上传</div>
              <div class="storage-value primary">{{ overview.todayUploads }}</div>
            </div>
            <div class="storage-remaining">
              <div class="storage-label">总文件夹</div>
              <div class="storage-value success">{{ overview.totalFolders }}</div>
            </div>
          </div>
          <div class="storage-percent">系统存储统计</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card class="today-card">
          <template #header>
            <div class="card-header">
              <span>今日统计</span>
            </div>
          </template>
          <div class="today-stats">
            <div class="today-item">
              <el-icon class="today-icon"><User /></el-icon>
              <div class="today-info">
                <div class="today-value">{{ overview.todayLogins }}</div>
                <div class="today-label">登录次数</div>
              </div>
            </div>
            <div class="today-item">
              <el-icon class="today-icon"><Upload /></el-icon>
              <div class="today-info">
                <div class="today-value">{{ overview.todayUploads }}</div>
                <div class="today-label">上传文件</div>
              </div>
            </div>
            <div class="today-item">
              <el-icon class="today-icon"><Document /></el-icon>
              <div class="today-info">
                <div class="today-value">{{ overview.totalFiles }}</div>
                <div class="today-label">总文件数</div>
              </div>
            </div>
            <div class="today-item">
              <el-icon class="today-icon"><FolderAdd /></el-icon>
              <div class="today-info">
                <div class="today-value">{{ overview.totalFolders }}</div>
                <div class="today-label">总文件夹</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近登录日志 -->
    <el-card class="recent-logs-card">
      <template #header>
        <div class="card-header">
          <span>最近登录记录</span>
          <el-button type="text" @click="viewAllLogs">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentLogs" style="width: 100%" stripe v-loading="loading">
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userNumber" label="用户号" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Refresh, User, UserFilled, Avatar, Document, 
  Upload, Download, FolderAdd, HomeFilled
} from '@element-plus/icons-vue'
import { 
  getSystemOverview, 
  getLoginLogs,
  type SystemOverview,
  type LoginLog 
} from '@/api/admin'

const router = useRouter()

// 系统概览数据
const overview = ref<SystemOverview>({
  totalUsers: 0,
  activeUsers: 0,
  adminCount: 0,
  totalFiles: 0,
  totalFolders: 0,
  totalStorage: 0,
  todayLogins: 0,
  todayUploads: 0
})

// 最近登录日志
const recentLogs = ref<Array<{
  time: string
  username: string
  userNumber: string
  ip: string
  status: string
}>>([])

// 加载数据
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    // 加载系统概览
    const overviewData = await getSystemOverview()
    overview.value = overviewData

    // 加载最近登录日志
    const logsData = await getLoginLogs({ page: 1, size: 5 })
    recentLogs.value = logsData.items.map(log => ({
      time: formatDateTime(log.loginTime),
      username: log.username,
      userNumber: log.userNumber,
      ip: log.ipAddress,
      status: log.loginStatus === 'SUCCESS' ? '成功' : '失败'
    }))
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  loadData()
}

function viewAllLogs() {
  router.push('/admin/logs')
}

function getProgressColor(percentage: number) {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#67c23a'
}

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


onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
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
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  margin-right: 16px;
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.active-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.admin-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.file-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-footer {
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.stat-change {
  font-size: 12px;
  font-weight: 500;
}

.stat-change.positive {
  color: #67c23a;
}

.storage-row {
  margin-bottom: 20px;
}

.storage-card,
.today-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

.storage-info {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24px;
}

.storage-total,
.storage-used,
.storage-remaining {
  text-align: center;
}

.storage-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.storage-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.storage-value.primary {
  color: #409eff;
}

.storage-value.success {
  color: #67c23a;
}

.storage-progress {
  margin-bottom: 12px;
}

.storage-percent {
  text-align: center;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.today-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.today-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.today-icon {
  font-size: 32px;
  color: #409eff;
  margin-right: 16px;
}

.today-info {
  flex: 1;
}

.today-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.today-label {
  font-size: 14px;
  color: #909399;
}

.recent-logs-card {
  border-radius: 8px;
}

@media (max-width: 768px) {
  .admin-dashboard {
    padding: 16px;
  }

  .today-stats {
    grid-template-columns: 1fr;
  }

  .storage-info {
    flex-direction: column;
    gap: 16px;
  }
}
</style>

