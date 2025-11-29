<template>
  <el-container class="admin-layout">
    <!-- 顶部导航栏 -->
    <el-header class="admin-header">
      <div class="header-left">
        <div class="logo" @click="goToHome">
          <img src="@/assets/logo.png" alt="Logo" />
          <span class="logo-text">CloudDriveLite 管理后台</span>
        </div>
        <el-button 
          :icon="Back" 
          text
          class="back-home-btn"
          @click="goToHome"
        >
          返回主页
        </el-button>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="username">{{ currentUser }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="settings">系统设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '240px'" class="admin-aside">
        <div class="collapse-btn" @click="toggleCollapse">
          <el-icon><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="admin-menu"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><Odometer /></el-icon>
            <template #title>系统概览</template>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/storage">
            <el-icon><FolderOpened /></el-icon>
            <template #title>存储管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Document /></el-icon>
            <template #title>登录日志</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  UserFilled,
  ArrowDown,
  Expand,
  Fold,
  Odometer,
  User,
  FolderOpened,
  Document,
  Back
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const currentUser = ref('管理员')

const activeMenu = computed(() => route.path)

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中')
      break
    case 'logout':
      ElMessage.success('已退出登录')
      router.push('/login')
      break
  }
}

function goToHome() {
  router.push('/files')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.8;
}

.back-home-btn {
  margin-left: 16px;
  color: #606266;
  transition: all 0.3s ease;
}

.back-home-btn:hover {
  color: #3b82f6;
  transform: translateX(-2px);
}

.logo img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.admin-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  position: relative;
  transition: width 0.3s;
}

.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-bottom: 1px solid #e4e7ed;
  color: #606266;
  transition: all 0.3s;
}

.collapse-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.admin-menu {
  border: none;
  height: calc(100vh - 48px);
  overflow-y: auto;
}

.admin-menu:not(.el-menu--collapse) {
  width: 240px;
}

.admin-main {
  background: #f5f7fa;
  padding: 0;
  overflow-y: auto;
}

/* 滚动条样式 */
.admin-menu::-webkit-scrollbar,
.admin-main::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.admin-menu::-webkit-scrollbar-track,
.admin-main::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.admin-menu::-webkit-scrollbar-thumb,
.admin-main::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.admin-menu::-webkit-scrollbar-thumb:hover,
.admin-main::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式 */
@media (max-width: 768px) {
  .admin-aside {
    position: fixed;
    left: 0;
    top: 60px;
    height: calc(100vh - 60px);
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s;
  }

  .admin-aside.mobile-open {
    transform: translateX(0);
  }

  .admin-main {
    margin-left: 0;
  }
}
</style>

