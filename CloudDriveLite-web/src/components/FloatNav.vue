<template>
    <div class="float-nav">
      <div class="left">
        <div class="logo"><img src="@/assets/logo.png"/>CloudDriveLite</div>
        <Breadcrumb :folder-chain="folderChain" />
      </div>
  
      <div class="center">
        <el-input
          v-model="keyword"
          placeholder="搜索文件"
          prefix-icon="Search"
          size="small"
          style="width: 260px"
          @keyup.enter="onSearch"
        />
      </div>
  
      <div class="right">
        <el-dropdown trigger="click">
          <span class="user">
            <el-avatar size="small" icon="User" />
            <span style="margin-left: 8px">{{ userName || '用户' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, computed } from 'vue'
  import { useRouter } from 'vue-router'
  import { ArrowDown, Search } from '@element-plus/icons-vue'

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
  .float-nav{
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 64px;
    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
    color: #334155;
    display: flex;
    align-items: center;
    padding: 0 32px;
    z-index: 1000;
    box-shadow: 0 4px 20px rgba(0,0,0,.08);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  }
  .left{ 
    display: flex; 
    align-items: center; 
    margin-right: auto;
  }
  
  .logo{ 
    font-size: 22px; 
    font-weight: 700; 
    margin-right: 32px;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .logo img {
    width: 100px;
    
    border-radius: 6px;
  }
  
  .center{ 
    margin: 0 32px;
  }
  
  .right{ 
    margin-left: auto;
  }
  
  .user{ 
    cursor: pointer; 
    display: flex; 
    align-items: center; 
    color: #475569;
    padding: 8px 12px;
    border-radius: 8px;
    transition: all 0.2s ease;
  }
  
  .user:hover {
    background: rgba(59, 130, 246, 0.1);
    color: #3b82f6;
  }
  
  
  .el-input__wrapper {
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,.06);
    border: 1px solid #e2e8f0;
    transition: all 0.2s ease;
  }
  
  .el-input__wrapper:hover {
    border-color: #cbd5e1;
    box-shadow: 0 4px 12px rgba(0,0,0,.08);
  }
  
  .el-input__wrapper.is-focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  </style>