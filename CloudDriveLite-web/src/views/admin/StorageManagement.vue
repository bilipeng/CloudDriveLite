<template>
  <div class="storage-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>存储管理</h2>
      <el-button type="primary" :icon="Refresh" @click="loadData" :loading="loading">刷新</el-button>
    </div>

    <!-- 系统存储总览 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :xs="24" :md="8">
        <el-card class="overview-card">
          <div class="overview-item">
            <div class="overview-label">总存储空间</div>
            <div class="overview-value">{{ formatStorage(storageStats.totalStorage) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="overview-card">
          <div class="overview-item">
            <div class="overview-label">已使用</div>
            <div class="overview-value primary">{{ formatStorage(storageStats.totalStorage) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="overview-card">
          <div class="overview-item">
            <div class="overview-label">文件类型数</div>
            <div class="overview-value success">{{ Object.keys(storageStats.typeStatistics).length }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 存储使用率 -->
    <el-card class="usage-card">
      <template #header>
        <div class="card-header">
          <span>系统存储使用率</span>
        </div>
      </template>
      <div class="usage-content">
        <el-progress
          :percentage="storageUsagePercent"
          :color="getProgressColor(storageUsagePercent)"
          :stroke-width="20"
          text-inside
        />
        <div class="usage-stats">
          <div class="usage-stat-item">
            <span class="stat-label">总存储：</span>
            <span class="stat-value">{{ formatStorage(storageStats.totalStorage) }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 用户存储排行 -->
    <el-card class="ranking-card">
      <template #header>
        <div class="card-header">
          <span>用户存储使用排行</span>
          <el-input
            v-model="rankingLimit"
            placeholder="显示数量"
            style="width: 120px"
            type="number"
            :min="5"
            :max="50"
          >
            <template #append>
              <el-button @click="updateRanking">更新</el-button>
            </template>
          </el-input>
        </div>
      </template>
      <el-table :data="storageRanking" stripe border style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userNumber" label="用户号" width="140" />
        <el-table-column label="已用存储" width="140">
          <template #default="{ row }">
            <span class="storage-amount">{{ formatStorage(row.usedStorage) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最大存储" width="140">
          <template #default="{ row }">
            <span class="storage-amount">{{ formatStorage(row.maxStorage) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用率" width="200">
          <template #default="{ row }">
            <div class="usage-cell">
              <el-progress
                :percentage="Math.round(row.usagePercent)"
                :color="getStorageColor(row.usagePercent)"
                :stroke-width="8"
                :show-text="false"
              />
              <span class="usage-text">{{ Math.round(row.usagePercent) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEditStorage(row)">
              设置存储
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 存储分布统计 -->
    <el-row :gutter="20" class="distribution-row">
      <el-col :xs="24" :md="12">
        <el-card class="distribution-card">
          <template #header>
            <div class="card-header">
              <span>存储类型分布</span>
            </div>
          </template>
          <div class="type-distribution">
            <div v-if="typeStatsList.length === 0" class="no-data">暂无数据</div>
            <div v-for="(item, index) in typeStatsList" :key="index" class="type-item">
              <div class="type-info">
                <div class="type-name">{{ item.type }}</div>
                <div class="type-size">{{ formatStorage(item.size) }} ({{ item.percent.toFixed(1) }}%)</div>
              </div>
              <el-progress :percentage="item.percent" :color="getTypeColor(index)" :stroke-width="12" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="distribution-card">
          <template #header>
            <div class="card-header">
              <span>用户存储分布</span>
            </div>
          </template>
          <div class="user-distribution">
            <div class="dist-item">
              <div class="dist-label">使用超过 80%</div>
              <div class="dist-value warning">{{ storageStats.usersOver80 }} 用户</div>
            </div>
            <div class="dist-item">
              <div class="dist-label">使用 50-80%</div>
              <div class="dist-value">{{ storageStats.users50to80 }} 用户</div>
            </div>
            <div class="dist-item">
              <div class="dist-label">使用 20-50%</div>
              <div class="dist-value success">{{ storageStats.users20to50 }} 用户</div>
            </div>
            <div class="dist-item">
              <div class="dist-label">使用低于 20%</div>
              <div class="dist-value">{{ storageStats.usersUnder20 }} 用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { 
  getStorageStatistics, 
  getStorageRanking,
  type StorageStatistics,
  type StorageRankingItem 
} from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const rankingLimit = ref(10)

// 存储统计数据
const storageStats = ref<StorageStatistics>({
  totalStorage: 0,
  typeStatistics: {},
  usersOver80: 0,
  users50to80: 0,
  users20to50: 0,
  usersUnder20: 0
})

// 存储排行数据
const storageRanking = ref<StorageRankingItem[]>([])

// 格式化存储大小
function formatStorage(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 计算存储使用率（这里需要从所有用户的总存储空间计算，暂时用0）
const storageUsagePercent = computed(() => {
  // 实际应该计算：总已用 / 总分配
  // 这里暂时返回0，因为需要所有用户的总分配空间
  return 0
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    // 加载存储统计
    const stats = await getStorageStatistics()
    storageStats.value = stats

    // 加载存储排行
    const ranking = await getStorageRanking(rankingLimit.value)
    storageRanking.value = ranking
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

function updateRanking() {
  loadData()
}

function handleEditStorage(row: StorageRankingItem) {
  // 跳转到用户管理页面，并定位到该用户
  router.push({ path: '/admin/users', query: { userId: row.userId } })
}

function getProgressColor(percentage: number) {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#67c23a'
}

function getStorageColor(percent: number) {
  if (percent >= 90) return '#f56c6c'
  if (percent >= 70) return '#e6a23c'
  return '#67c23a'
}

// 获取文件类型统计（按类型分组）
const typeStatsList = computed(() => {
  const stats = storageStats.value.typeStatistics
  const total = storageStats.value.totalStorage
  return Object.entries(stats).map(([type, size]) => ({
    type: getTypeName(type),
    size,
    percent: total > 0 ? (size / total * 100) : 0
  })).sort((a, b) => b.size - a.size)
})

function getTypeName(type: string): string {
  if (type.startsWith('image/')) return '图片文件'
  if (type.startsWith('video/')) return '视频文件'
  if (type.startsWith('application/pdf') || type.includes('document') || type.includes('text')) return '文档文件'
  return '其他文件'
}

function getTypeColor(index: number): string {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#909399']
  return colors[index % colors.length]
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.storage-management {
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

.overview-row {
  margin-bottom: 20px;
}

.overview-card {
  border-radius: 8px;
  text-align: center;
}

.overview-item {
  padding: 20px 0;
}

.overview-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.overview-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}

.overview-value.primary {
  color: #409eff;
}

.overview-value.success {
  color: #67c23a;
}

.usage-card,
.ranking-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

.usage-content {
  padding: 20px 0;
}

.usage-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.usage-stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-right: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.stat-value.success {
  color: #67c23a;
}

.storage-amount {
  font-weight: 600;
  color: #303133;
}

.usage-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.usage-text {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  min-width: 45px;
}

.distribution-row {
  margin-bottom: 20px;
}

.distribution-card {
  border-radius: 8px;
}

.type-distribution {
  padding: 20px 0;
}

.type-item {
  margin-bottom: 24px;
}

.type-item:last-child {
  margin-bottom: 0;
}

.type-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.type-name {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.type-size {
  font-size: 14px;
  color: #909399;
}

.no-data {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.user-distribution {
  padding: 20px 0;
}

.dist-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.dist-item:last-child {
  margin-bottom: 0;
}

.dist-label {
  font-size: 14px;
  color: #606266;
}

.dist-value {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.dist-value.success {
  color: #67c23a;
}

.dist-value.warning {
  color: #e6a23c;
}

@media (max-width: 768px) {
  .storage-management {
    padding: 16px;
  }

  .usage-stats {
    flex-direction: column;
    gap: 12px;
  }
}
</style>

