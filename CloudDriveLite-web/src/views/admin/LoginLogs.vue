<template>
  <div class="login-logs">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>登录日志</h2>
      <div class="header-actions">
        <el-button :icon="Download">导出</el-button>
        <el-button type="primary" :icon="Refresh" @click="handleSearch" :loading="loading">刷新</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="用户">
          <el-select
            v-model="filterUserId"
            placeholder="全部用户"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="登录状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon success-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalSuccess.toLocaleString() }}</div>
              <div class="stat-label">成功登录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon error-icon">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalFailed.toLocaleString() }}</div>
              <div class="stat-label">失败登录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.activeUsers.toLocaleString() }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon today-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.todaySuccess.toLocaleString() }}</div>
              <div class="stat-label">今日登录</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 登录日志表格 -->
    <el-card class="table-card">
      <el-table
        :data="logList"
        stripe
        border
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="loginTime" label="登录时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userNumber" label="用户号" width="140" />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="userAgent" label="浏览器" width="200" show-overflow-tooltip />
        <el-table-column prop="loginStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.loginStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.loginStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failureReason" label="失败原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.failureReason" class="failure-reason">{{ row.failureReason }}</span>
            <span v-else class="no-reason">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Search, Refresh, Download, CircleCheck, CircleClose, User, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { 
  getLoginLogs, 
  getLoginStatistics,
  listUsers,
  type LoginLog,
  type LoginStatistics,
  type UserInfo
} from '@/api/admin'

const loading = ref(false)
const filterUserId = ref<number | undefined>(undefined)
const filterStatus = ref<'SUCCESS' | 'FAILED' | ''>('')
const dateRange = ref<[Date, Date] | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 用户选项
const userOptions = ref<UserInfo[]>([])

// 日志数据
const logList = ref<LoginLog[]>([])

// 统计数据
const statistics = ref<LoginStatistics>({
  totalSuccess: 0,
  totalFailed: 0,
  todaySuccess: 0,
  todayFailed: 0,
  activeUsers: 0,
  totalLogins: 0
})

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

// 格式化日期为 ISO 字符串
function formatDateISO(date: Date): string {
  return date.toISOString()
}

// 加载用户列表（用于筛选）
async function loadUsers() {
  try {
    const result = await listUsers({ page: 1, size: 100 })
    userOptions.value = result.items
  } catch (error: any) {
    console.error('加载用户列表失败:', error)
  }
}

// 加载登录日志
async function loadLogs() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterUserId.value) params.userId = filterUserId.value
    if (filterStatus.value) params.loginStatus = filterStatus.value
    if (dateRange.value) {
      params.startDate = formatDateISO(dateRange.value[0])
      params.endDate = formatDateISO(dateRange.value[1])
    }
    
    const result = await getLoginLogs(params)
    logList.value = result.items
    total.value = result.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载登录日志失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
async function loadStatistics() {
  try {
    const params: any = {}
    if (dateRange.value) {
      params.startDate = formatDateISO(dateRange.value[0])
      params.endDate = formatDateISO(dateRange.value[1])
    }
    const stats = await getLoginStatistics(params)
    statistics.value = stats
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
  }
}

function handleSearch() {
  currentPage.value = 1
  loadLogs()
  loadStatistics()
}

function handleReset() {
  filterUserId.value = undefined
  filterStatus.value = ''
  dateRange.value = null
  currentPage.value = 1
  loadLogs()
  loadStatistics()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  currentPage.value = 1
  loadLogs()
}

function handlePageChange(val: number) {
  currentPage.value = val
  loadLogs()
}

// 监听分页变化
watch([currentPage, pageSize], () => {
  loadLogs()
})

onMounted(() => {
  loadUsers()
  loadLogs()
  loadStatistics()
})
</script>

<style scoped>
.login-logs {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.filter-form {
  margin: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  margin-right: 16px;
}

.success-icon {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.error-icon {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.user-icon {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.today-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.table-card {
  border-radius: 8px;
}

.failure-reason {
  color: #f56c6c;
  font-size: 12px;
}

.no-reason {
  color: #c0c4cc;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .login-logs {
    padding: 16px;
  }

  .filter-form {
    display: flex;
    flex-direction: column;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>

