import { createRouter, createWebHistory } from 'vue-router'
const FileManager = () => import('../views/FileManager.vue')
const OverallLayout = () => import('../layout/NavbarLayout.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/index' },
    { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('../views/Register.vue') },

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
  const publicPaths = ['/login', '/register']
  const isPublic = publicPaths.includes(to.path)

  if (!isAuthed && !isPublic) {
    next('/login')
  } else if (isAuthed && to.path === '/login') {
    next('/files')
  } else {
    next()
  }
})

export default router
