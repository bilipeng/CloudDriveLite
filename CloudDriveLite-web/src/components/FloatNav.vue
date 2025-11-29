<template>
  <div class="float-nav">
    <div class="left">
      <div class="logo" @click="goToHome">
        <img src="@/assets/logo.png" alt="Logo" />
        <span class="logo-text">CloudDriveLite</span>
      </div>
      <el-breadcrumb separator="/" class="nav-breadcrumb">
        <el-breadcrumb-item v-for="c in folderChain" :key="c.id">
          {{ c.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="center">
      <el-input
        v-model="keyword"
        placeholder="搜索文件、文件夹..."
        :prefix-icon="Search"
        size="default"
        class="search-input"
        clearable
        @keyup.enter="onSearch"
      />
    </div>
    
    <div class="right">
      <el-dropdown trigger="click" placement="bottom-end">
        <span class="user">
          <el-avatar :size="32" class="user-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <span class="user-name">{{ userName || '用户' }}</span>
          <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu class="user-menu">
            <el-dropdown-item @click="goToProfile">
              <el-icon><UserFilled /></el-icon>
              <span>个人中心</span>
            </el-dropdown-item>
            <el-dropdown-item divided @click="logout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>
  
  <script setup lang="ts">
  import { ref, computed } from 'vue'
  import { useRouter } from 'vue-router'
  import { ArrowDown, Search, User, UserFilled, SwitchButton } from '@element-plus/icons-vue'

  import { ElMessage } from 'element-plus'
  import { authApi } from '@/api/auth'

  
  
  const router = useRouter()
  const activeIndex = ref(router.currentRoute.value.path)
  const keyword = ref('')
  
  // 获取用户信息
  const userName = ref(localStorage.getItem('userName') || '')
  
  // 文件夹链条数据，从父组件传入
  const props = defineProps<{
    folderChain?: { id: string | number; name: string }[]
  }>()
  
  const folderChain = computed(() => props.folderChain || [])
  
  function onSearch() {
    /* 触发父级事件或跳搜索页 */
    router.push({ name: 'files', query: { kw: keyword.value } })
  }
  
  function goToProfile() {
    router.push('/profile')
  }
  
  function goToHome() {
    router.push('/files')
  }
  
  async function logout() {
    try {
      // 调用后端登出API
      await authApi.logout()
      ElMessage.success('已安全退出')
    } catch (error) {
      console.error('登出失败:', error)
      // 即使后端登出失败，也要清除本地数据
    } finally {
      // 清除本地存储的用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('userNumber')
      router.replace('/login')
    }
  }
  </script>
  
<style scoped>
.float-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 70px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  color: #334155;
  display: flex;
  align-items: center;
  padding: 0 40px;
  z-index: 1000;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  transition: all 0.3s ease;
}

.left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.8;
}

.logo img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.logo-text {
  color: #303133;
}

.nav-breadcrumb {
  flex: 1;
  min-width: 0;
}

.center {
  margin: 0 24px;
  flex-shrink: 0;
}

.search-input {
  width: 320px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.right {
  margin-left: auto;
  flex-shrink: 0;
}

.user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.user:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.user-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
}

.user-name {
  font-weight: 500;
  font-size: 14px;
}

.dropdown-icon {
  transition: transform 0.3s ease;
  font-size: 12px;
}

.user:hover .dropdown-icon {
  transform: rotate(180deg);
}

.user-menu :deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  transition: all 0.2s ease;
}

.user-menu :deep(.el-dropdown-menu__item:hover) {
  background: #f1f5f9;
  color: #3b82f6;
}

@media (max-width: 1024px) {
  .float-nav {
    padding: 0 20px;
  }
  
  .search-input {
    width: 200px;
  }
  
  .logo-text {
    display: none;
  }
}
</style>