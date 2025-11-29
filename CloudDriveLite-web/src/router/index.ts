import { createRouter, createWebHistory } from 'vue-router'
const FileManager = () => import('../views/FileManager.vue')
const OverallLayout = () => import('../layout/NavbarLayout.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/index' },
    { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('../views/Register.vue') },
    { path: '/forgot-password', name: 'forgot-password', component: () => import('../views/ForgotPassword.vue') },

    {
      path: '/',
      component: OverallLayout,   // 外套
      children: [                // 内页
        {
          path: '/index',
          name: 'index',
          component: FileManager
        },
        {
          path: '/files',
          name: 'files',
          component: FileManager,
        },
        {
          path: '/profile',
          name: 'profile',
          component: () => import('../views/Profile.vue')
        }
      ]
    },

    // 管理员路由
    {
      path: '/admin',
      component: () => import('../layout/AdminLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/admin/dashboard'
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('../views/admin/Dashboard.vue')
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/UserManagement.vue')
        },
        {
          path: 'storage',
          name: 'admin-storage',
          component: () => import('../views/admin/StorageManagement.vue')
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('../views/admin/LoginLogs.vue')
        }
      ]
    },
    
    // 其他页面可按需添加
  ],

})

// 简单的登录守卫：没有 token 去登录
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isAuthed = Boolean(token)
  const publicPaths = ['/login', '/register', '/forgot-password']
  const isPublic = publicPaths.includes(to.path)
  const isAdminPath = to.path.startsWith('/admin')

  // 如果访问管理员路径但未登录，重定向到登录页
  if (isAdminPath && !isAuthed) {
    next('/login')
    return
  }

  if (!isAuthed && !isPublic) {
    next('/login')
  } else if (isAuthed && to.path === '/login') {
    next('/files')
  } else {
    next()
  }
})

export default router
