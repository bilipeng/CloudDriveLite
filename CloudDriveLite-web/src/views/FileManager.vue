<template>
  <el-container style="height: 100vh">

    <el-main style="padding: 20px;">
      <el-card shadow="never">
        <!-- 工具栏 -->
        <div style="margin-bottom: 12px; display: flex; gap: 8px; align-items: center; justify-content: space-between;">
          <div style="display: flex; gap: 8px; align-items: center;">
            <Back :disabled="folderChain.length <= 1" @back="goBack" />

            <Upfilebutton :folderId="folderId" @uploaded="load" />

            <el-button type="primary" @click="showCreateFolderDialog = true">新建文件夹</el-button>
          </div>
          <div style="flex: 1; margin: 0 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
            <span style="color: var(--el-text-color-secondary)">当前路径：{{ currentPath }}</span>
          </div>
          <div>
            <el-button @click="load()" :loading="loading">刷新</el-button>
          </div>
        </div>
        <div class="table-title">
          <span>文件名</span>
          <span>修改时间</span>
          <span>大小</span>
        </div>
        <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
          <el-table-column type="selection" width="48" />
          <el-table-column label="文件名" min-width="250">
            <template #default="{ row }">
              <el-icon style="margin-right: 6px">
                <Folder v-if="row.folder" />
                <Picture v-else-if="row.fileType && row.fileType.startsWith('image/')" />
                <VideoPlay v-else-if="row.fileType && row.fileType.startsWith('video/')" />
                <Document v-else />
              </el-icon>
              <span 
                class="file-name" 
                :class="{ 'folder-name': row.folder }"
                @dblclick="handleItemDoubleClick(row)"
              >
                {{ row.name }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="修改时间" width="200" />
          <el-table-column prop="sizeText" label="大小" width="140" />
          <el-table-column label="操作" width="230">
            <template #default="{ row }">
              <el-button size="small" @click="handleRename(row)">重命名</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              <el-button size="small" @click="handleDownload(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <span>共 {{ total }} 项</span>
          <el-pagination layout="prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="onPageChange" small />
        </div>
      </el-card>
    </el-main>
    
    <!-- 创建文件夹对话框 -->
    <el-dialog v-model="showCreateFolderDialog" title="新建文件夹" width="400px">
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
    <el-dialog v-model="showRenameDialog" title="重命名" width="400px">
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
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Cloudy, Folder, Refresh, Setting, Document, Picture, VideoPlay } from '@element-plus/icons-vue'
import Back from '@/components/Back.vue'
import { listFiles, createFolder, renameItem, deleteItem, getBreadcrumb, type FileItem as ApiFileItem } from '@/api/files'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import Upfilebutton from '@/components/Upfilebutton.vue'

type Row = {
  id: number
  name: string
  folder: boolean
  fileType: string
  sizeText: string
  updatedAt: string
}

const tableData = ref<Row[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const folderId = ref<number>(0)
const pathStack = ref<number[]>([0])

type Crumb = { id: number; name: string }
const folderChain = ref<Crumb[]>([{ id: 0, name: '根目录' }])
const currentPath = ref<string>('根目录')

// 加载状态
const loading = ref(false)

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
    const [data, breadcrumb] = await Promise.all([
      listFiles({ folderId: folderId.value, page: page.value, size: size.value }),
      getBreadcrumb(folderId.value)
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
    folderChain.value = breadcrumb
    currentPath.value = breadcrumb.map(c => c.name).join(' / ')
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 处理项目双击
function handleItemDoubleClick(row: Row) {
  if (row.folder) {
    pathStack.value.push(row.id)
    folderId.value = row.id
    page.value = 1
    load()
  } else {
    // 文件后续处理：预览/下载
  }
}

// 返回上级文件夹
function goBack() {
  if (pathStack.value.length <= 1) return
  pathStack.value.pop()
  folderId.value = pathStack.value[pathStack.value.length - 1]
  page.value = 1
  load()
}

// 分页变化
function onPageChange(p: number) {
  page.value = p
  load()
}

// 加载文件列表
onMounted(() => {
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


// 下载
function handleDownload(row: Row) {
  window.open(`/api/files/${row.id}/download`, '_blank')
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.title { font-size: 14px; }
.nav-actions { display: flex; align-items: center; gap: 8px; }
.table-title { display: none; }
.file-name { 
  cursor: pointer; 
  color: var(--el-text-color-primary);
  transition: color 0.2s ease;
}

.file-name:hover {
  color: #3b82f6;
}

.folder-name {
  font-weight: 500;
  color: #1e293b;
}

.folder-name:hover {
  color: #3b82f6;
}
.table-footer { display: flex; justify-content: space-between; padding: 12px 0 0; }
</style>


