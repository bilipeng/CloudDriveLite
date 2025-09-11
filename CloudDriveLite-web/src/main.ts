import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

/* 1. 引入 Element Plus 与样式 */
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

/* 2. 引入所有图标 */
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

/* 3. 注册所有图标组件 */
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

app.use(createPinia())
app.use(router)

app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
