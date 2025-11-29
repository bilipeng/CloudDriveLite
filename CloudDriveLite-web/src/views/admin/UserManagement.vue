<template>
  <div class="user-management">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Refresh" @click="loadUsers">刷新</el-button>
      </div>
    </div>

    <!-- 搜索和筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="搜索">
          <el-input
            v-model="searchKeyword"
            placeholder="用户名/用户号"
            clearable
            style="width: 200px"
            :prefix-icon="Search"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filterRole" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表表格 -->
    <el-card class="table-card">
      <el-table
        :data="userList"
        stripe
        border
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userNumber" label="用户号" width="140" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="phoneNumber" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="存储使用" width="200">
          <template #default="{ row }">
            <div class="storage-info">
              <div class="storage-text">
                {{ formatStorage(row.usedStorage) }} / {{ formatStorage(row.maxStorage) }}
              </div>
              <el-progress
                :percentage="Math.round(row.usagePercent)"
                :color="getStorageColor(row.usagePercent)"
                :stroke-width="6"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEditStorage(row)">
              设置存储
            </el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-if="row.role !== 'ADMIN'"
              size="small"
              type="danger"
              link
              @click="handleSetAdmin(row)"
            >
              设为管理员
            </el-button>
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

    <!-- 设置存储空间对话框 -->
    <el-dialog v-model="storageDialogVisible" title="设置存储空间" width="500px">
      <el-form :model="storageForm" label-width="120px">
        <el-form-item label="用户名">
          <el-input v-model="storageForm.username" disabled />
        </el-form-item>
        <el-form-item label="当前已用">
          <el-input :value="storageForm.usedStorage" disabled />
        </el-form-item>
        <el-form-item label="最大存储空间 (GB)" required>
          <el-input-number
            v-model="storageForm.maxStorage"
            :min="Math.ceil(storageForm.usedStorageBytes / 1073741824)"
            :step="1"
            :precision="0"
            style="width: 100%"
          />
          <div class="form-hint">最小值为：{{ Math.ceil(storageForm.usedStorageBytes / 1073741824) }} GB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="storageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveStorage">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  listUsers, 
  updateUserStorage, 
  updateUserStatus, 
  updateUserRole,
  type UserInfo 
} from '@/api/admin'

const loading = ref(false)
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const userList = ref<UserInfo[]>([])

const storageDialogVisible = ref(false)
const storageForm = ref({
  id: 0,
  username: '',
  usedStorage: '',
  usedStorageBytes: 0,
  maxStorage: 0
})

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

// 加载用户列表
async function loadUsers() {
  loading.value = true
  try {
    const result = await listUsers({
      keyword: searchKeyword.value || undefined,
      role: filterRole.value || undefined,
      status: filterStatus.value ?? undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    userList.value = result.items
    total.value = result.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadUsers()
}

function handleReset() {
  searchKeyword.value = ''
  filterRole.value = ''
  filterStatus.value = null
  currentPage.value = 1
  loadUsers()
}

function handleEditStorage(row: UserInfo) {
  storageForm.value = {
    id: row.id,
    username: row.username,
    usedStorage: formatStorage(row.usedStorage),
    usedStorageBytes: row.usedStorage,
    maxStorage: Math.ceil(row.maxStorage / 1073741824) // 转换为GB
  }
  storageDialogVisible.value = true
}

async function handleSaveStorage() {
  try {
    // 转换为字节
    const maxStorageBytes = storageForm.value.maxStorage * 1073741824
    await updateUserStorage(storageForm.value.id, maxStorageBytes)
    ElMessage.success('存储空间设置成功')
    storageDialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '设置存储空间失败')
  }
}

async function handleToggleStatus(row: UserInfo) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

async function handleSetAdmin(row: UserInfo) {
  try {
    await ElMessageBox.confirm(`确定要将用户 "${row.username}" 设置为管理员吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateUserRole(row.id, 'ADMIN')
    ElMessage.success('设置成功')
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '设置失败')
    }
  }
}

function handleSizeChange(val: number) {
  pageSize.value = val
  currentPage.value = 1
  loadUsers()
}

function handlePageChange(val: number) {
  currentPage.value = val
  loadUsers()
}

function getStorageColor(percent: number) {
  if (percent >= 90) return '#f56c6c'
  if (percent >= 70) return '#e6a23c'
  return '#67c23a'
}

// 监听分页变化
watch([currentPage, pageSize], () => {
  loadUsers()
})

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
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

.table-card {
  border-radius: 8px;
}

.storage-info {
  width: 100%;
}

.storage-text {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .user-management {
    padding: 16px;
  }

  .filter-form {
    display: flex;
    flex-direction: column;
  }
}
</style>

