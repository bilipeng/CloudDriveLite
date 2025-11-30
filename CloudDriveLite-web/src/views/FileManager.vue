<template>
  <div class="file-manager">
    <!-- 主内容区 -->
    <el-card class="main-card" shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button 
            :icon="ArrowLeft" 
            :disabled="folderChain.length <= 1" 
            @click="goBack"
            circle
            class="back-btn"
          />
          <el-divider direction="vertical" />
          <el-button-group class="action-button-group">
            <Upfilebutton :folderId="folderId" @uploaded="handleUploaded" />
            <ChunkUploader :folderId="folderId" />
            <el-button type="primary" :icon="FolderAdd" @click="showCreateFolderDialog = true" class="create-folder-btn">
              新建文件夹
            </el-button>
          </el-button-group>
        </div>
        <div class="toolbar-center">
          <el-breadcrumb separator="/" class="modern-breadcrumb">
          <el-breadcrumb-item 
            v-for="(crumb, index) in folderChain" 
            :key="crumb.id"
            :class="{ 'breadcrumb-active': index === folderChain.length - 1 }"
            @click="handleBreadcrumbClick(index)"
            style="cursor: pointer;"
          >
            <el-icon v-if="index === 0" style="margin-right: 4px;"><HomeFilled /></el-icon>
            {{ crumb.name }}
          </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="toolbar-right">
          <el-button 
            :icon="Refresh" 
            @click="load()" 
            :loading="loading" 
            circle
            class="action-btn"
          />
          <el-button 
            type="danger" 
            :disabled="selectedRows.length === 0" 
            @click="handleBatchDelete"
            :icon="Delete"
            class="action-btn"
          >
            <span v-if="selectedRows.length">删除 ({{ selectedRows.length }})</span>
            <span v-else>删除</span>
          </el-button>
        </div>
      </div>
      <!-- 文件列表 -->
      <el-table 
        ref="tableRef"
        :data="tableData" 
        stripe 
        class="file-table modern-table"
        v-loading="loading"
        @selection-change="onSelectionChange"
        @row-dblclick="(row: Row) => handleItemDoubleClick(row)"
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-item">
              <div class="file-icon-wrapper">
                <el-icon class="file-icon" :size="24" :color="getFileIconColor(row)">
                  <Folder v-if="row.folder" />
                  <Picture v-else-if="row.fileType && row.fileType.startsWith('image/')" />
                  <VideoPlay v-else-if="row.fileType && row.fileType.startsWith('video/')" />
                  <Document v-else />
                </el-icon>
              </div>
              <span 
                class="file-name" 
                :class="{ 'folder-name': row.folder }"
              >
                {{ row.name }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="修改时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            <span class="size-text">{{ row.folder ? '-' : row.sizeText }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                v-if="!row.folder"
                size="small" 
                type="primary" 
                text
                :icon="Download"
                @click="handleDownload(row)"
                class="action-btn-item"
              >
                下载
              </el-button>
              <el-button 
                v-if="!row.folder"
                size="small" 
                type="primary" 
                text
                :icon="View"
                @click="handlePreview(row)"
                class="action-btn-item"
              >
                预览
              </el-button>
              <el-button 
                size="small" 
                type="primary" 
                text
                :icon="Edit"
                @click="handleRename(row)"
                class="action-btn-item"
              >
                重命名
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                text
                :icon="Delete"
                @click="handleDelete(row)"
                class="action-btn-item"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <el-icon :size="80" color="#cbd5e1"><FolderOpened /></el-icon>
            <p class="empty-text">当前文件夹为空</p>
            <p class="empty-hint">点击上方按钮上传文件或创建文件夹</p>
          </div>
        </template>
      </el-table>
      
      <!-- 分页 -->
      <div class="table-footer">
        <div class="footer-info">
          <span>共 {{ total }} 项</span>
        </div>
        <el-pagination 
          v-model:current-page="page"
          :page-size="size" 
          :total="total" 
          layout="prev, pager, next, jumper"
          @current-change="onPageChange"
          class="pagination"
        />
      </div>
      </el-card>

    <!-- 创建文件夹对话框 -->
    <el-dialog 
      v-model="showCreateFolderDialog" 
      title="新建文件夹" 
      width="420px"
      class="modern-dialog"
    >
      <el-form :model="createFolderForm" :rules="createFolderRules" ref="createFolderFormRef">
        <el-form-item label="文件夹名称" prop="folderName">
          <el-input v-model="createFolderForm.folderName" placeholder="请输入文件夹名称" @keyup.enter="handleCreateFolder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateFolderDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateFolder" :loading="createFolderLoading">创建</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog 
      v-model="showRenameDialog" 
      title="重命名" 
      width="420px"
      class="modern-dialog"
    >
      <el-form :model="renameForm" :rules="renameRules" ref="renameFormRef">
        <el-form-item label="新名称" prop="newName">
          <el-input v-model="renameForm.newName" placeholder="请输入新名称" @keyup.enter="handleRenameConfirm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showRenameDialog = false">取消</el-button>
          <el-button type="primary" @click="handleRenameConfirm" :loading="renameLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { 
  Folder, Refresh, Document, Picture, VideoPlay, 
  FolderAdd, Delete, Download, View, Edit, FolderOpened,
  ArrowLeft, HomeFilled
} from '@element-plus/icons-vue'
import Back from '@/components/Back.vue'
import { listFiles, createFolder, renameItem, deleteItem, getBreadcrumb, getRawBoxPreviewUrl, type FileItem as ApiFileItem } from '@/api/files'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import Upfilebutton from '@/components/Upfilebutton.vue'
import ChunkUploader from '@/components/ChunkUploader.vue'
import { useFileSystemStore } from '@/stores/fileSystem'

type Row = {
  id: number
  name: string
  folder: boolean
  fileType: string
  sizeText: string
  updatedAt: string
}

// 使用文件系统 store
const fileSystemStore = useFileSystemStore()

const tableData = ref<Row[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const folderId = computed(() => fileSystemStore.currentFolderId)
const selectedRows = ref<Row[]>([])
const tableRef = ref()

// 从 store 获取面包屑导航数据
const folderChain = computed(() => fileSystemStore.folderChain)
const currentPath = computed(() => folderChain.value.map(c => c.name).join(' / '))

// 加载状态
const loading = ref(false)

// 格式化时间
function formatTime(timeStr: string): string {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取文件图标颜色
function getFileIconColor(row: Row): string {
  if (row.folder) return '#3b82f6'
  if (row.fileType?.startsWith('image/')) return '#10b981'
  if (row.fileType?.startsWith('video/')) return '#f59e0b'
  return '#6b7280'
}

// 表格行类名
function tableRowClassName({ row }: { row: Row }) {
  return row.folder ? 'folder-row' : 'file-row'
}

// 上传后刷新
function handleUploaded() {
  load()
}

// 创建文件夹
const showCreateFolderDialog = ref(false)
const createFolderLoading = ref(false)
const createFolderForm = ref({ folderName: '' })
const createFolderFormRef = ref<FormInstance>()
const createFolderRules = {
  folderName: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { min: 1, max: 255, message: '长度在 1 到 255 个字符', trigger: 'blur' },
    { pattern: /^[^/\\:*?"<>|]*$/, message: '不能包含 / \\ : * ? " < > |', trigger: 'blur' }
  ]
}

// 重命名
const showRenameDialog = ref(false)
const renameLoading = ref(false)
const renameForm = ref({ newName: '' })
const renameFormRef = ref<FormInstance>()
const currentRenameItem = ref<Row | null>(null)
const renameRules = {
  newName: [
    { required: true, message: '请输入新名称', trigger: 'blur' },
    { min: 1, max: 255, message: '长度在 1 到 255 个字符', trigger: 'blur' }
  ]
}

async function load() {
  try {
    loading.value = true
    const currentId = folderId.value
    const [data, breadcrumb] = await Promise.all([
      listFiles({ folderId: currentId, page: page.value, size: size.value }),
      getBreadcrumb(currentId)
    ])
    total.value = data.total
    tableData.value = data.items.map((it: ApiFileItem) => ({
      id: it.id,
      name: it.fileName,
      folder: Boolean(it.folder),
      fileType: it.fileType,
      sizeText: it.fileSizeFormatted,
      updatedAt: it.uploadedTime,
    }))
    // 同步面包屑数据到 store
    fileSystemStore.setFolderChain(breadcrumb)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 处理项目双击
function handleItemDoubleClick(row: Row) {
  if (row.folder) {
    fileSystemStore.enterFolder(row.id)
    page.value = 1
    load()
  } else {
    // 文件后续处理：预览/下载
  }
}

// 返回上级文件夹
function goBack() {
  const newFolderId = fileSystemStore.goBack()
  if (newFolderId !== null) {
    page.value = 1
    load()
  }
}

// 分页变化
function onPageChange(p: number) {
  page.value = p
  load()
}

// 面包屑导航点击处理
function handleBreadcrumbClick(index: number) {
  const targetFolderId = fileSystemStore.navigateToPath(index)
  if (targetFolderId !== null) {
    page.value = 1
    load()
  }
}

// 加载文件列表
onMounted(() => {
  // 初始化 store 到根目录
  fileSystemStore.resetToRoot()
  load()
})

// 创建文件夹
async function handleCreateFolder() {
  if (!createFolderFormRef.value) return
  try {
    await createFolderFormRef.value.validate()
    createFolderLoading.value = true
    await createFolder({ folderName: createFolderForm.value.folderName, parentId: folderId.value })
    ElMessage.success('文件夹创建成功')
    showCreateFolderDialog.value = false
    createFolderForm.value.folderName = ''
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '创建文件夹失败')
  } finally {
    createFolderLoading.value = false
  }
}

// 重命名
function handleRename(row: Row) {
  currentRenameItem.value = row
  renameForm.value.newName = row.name
  showRenameDialog.value = true
}

async function handleRenameConfirm() {
  if (!renameFormRef.value || !currentRenameItem.value) return
  try {
    await renameFormRef.value.validate()
    renameLoading.value = true
    await renameItem(currentRenameItem.value.id, renameForm.value.newName)
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '重命名失败')
  } finally {
    renameLoading.value = false
  }
}

// 删除
async function handleDelete(row: Row) {
  try {
    const message = row.folder
      ? `确定要删除文件夹 "${row.name}" 吗？此操作将删除文件夹及其所有内容，且不可恢复。`
      : `确定要删除文件 "${row.name}" 吗？此操作不可恢复。`
    await ElMessageBox.confirm(message, '确认删除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteItem(row.id, row.folder)
    ElMessage.success('删除成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// 多选改变
function onSelectionChange(rows: Row[]) {
  selectedRows.value = rows
}

// 批量删除
async function handleBatchDelete() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 项吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await Promise.all(
      selectedRows.value.map(item => deleteItem(item.id, item.folder))
    )
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    tableRef.value?.clearSelection?.()
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '批量删除失败')
  }
}

// 行操作汇总
function handleRowCommand(cmd: string, row: Row) {
  switch (cmd) {
    case 'rename':
      handleRename(row)
      break
    case 'delete':
      handleDelete(row)
      break
    case 'download':
      handleDownload(row)
      break
    case 'preview':
      handlePreview(row)
      break
  }
}


// 下载
function handleDownload(row: Row) {
  window.open(`/api/files/${row.id}/download`, '_blank')
}

// 预览
async function handlePreview(row: Row) {
  try {
    const previewData = await getRawBoxPreviewUrl(row.id)
    // 在新窗口中打开 RawBox 预览链接
    window.open(previewData.previewUrl, '_blank')
    ElMessage.success('正在打开预览...')
  } catch (e: any) {
    ElMessage.error(e.message || '预览失败')
  }
}
</script>

<style scoped>
.file-manager {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  min-height: calc(100vh - 64px);
}

/* 主卡片 */
.main-card {
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  transition: all 0.3s ease;
}

.main-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  margin-bottom: 20px;
  border-bottom: 2px solid #f1f5f9;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-center {
  flex: 1;
  margin: 0 32px;
}

.back-btn {
  transition: all 0.2s ease;
}

.back-btn:hover:not(:disabled) {
  transform: translateX(-2px);
  background: #f1f5f9;
}

.action-btn {
  transition: all 0.2s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.action-button-group {
  display: flex;
  gap: 8px;
}

.action-button-group :deep(.el-button) {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.create-folder-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.create-folder-btn:hover {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.modern-breadcrumb {
  font-size: 14px;
}

.breadcrumb-active {
  color: #3b82f6;
  font-weight: 600;
}

/* 文件表格 */
.file-table {
  margin-top: 8px;
}

.modern-table :deep(.el-table__row) {
  transition: all 0.2s ease;
  cursor: pointer;
}

.modern-table :deep(.el-table__row:hover) {
  background: #f8fafc !important;
  transform: translateX(4px);
}

.modern-table :deep(.folder-row) {
  background: #fafbfc;
}

.modern-table :deep(.folder-row:hover) {
  background: #f1f5f9 !important;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.file-item:hover .file-icon-wrapper {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  transform: scale(1.08) rotate(2deg);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.file-name {
  cursor: pointer;
  color: #1e293b;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 400;
}

.file-name:hover {
  color: #3b82f6;
  font-weight: 500;
}

.folder-name {
  font-weight: 600;
  color: #0f172a;
}

.time-text,
.size-text {
  color: #64748b;
  font-size: 13px;
  font-weight: 400;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.action-btn-item {
  transition: all 0.2s ease;
  border-radius: 6px;
}

.action-btn-item:hover {
  transform: translateY(-1px);
  background: rgba(59, 130, 246, 0.1);
}

/* 空状态 */
.empty-state {
  padding: 80px 0;
  text-align: center;
  color: #94a3b8;
}

.empty-state .el-icon {
  opacity: 0.6;
  margin-bottom: 16px;
}

.empty-text {
  margin-top: 20px;
  font-size: 16px;
  font-weight: 500;
  color: #64748b;
}

.empty-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #94a3b8;
}

/* 表格底部 */
.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.footer-info {
  color: #6b7280;
  font-size: 14px;
}

.pagination {
  justify-content: flex-end;
}

/* 现代化对话框 */
.modern-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.modern-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.modern-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.modern-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.modern-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid #f1f5f9;
}

/* 响应式 */
@media (max-width: 768px) {
  .file-manager {
    padding: 16px;
  }

  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .toolbar-center {
    margin: 0;
  }

  .storage-card {
    padding: 20px;
  }

  .storage-values {
    font-size: 20px;
  }

  .storage-used {
    font-size: 24px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
}
</style>


