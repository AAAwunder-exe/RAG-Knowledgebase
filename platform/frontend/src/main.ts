import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 样式仍整体引入：保证 ElMessage/ElMessageBox 等 JS 调用组件也有样式
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/index.css'

const app = createApp(App)

// Element Plus 组件改为 vite 侧按需自动导入（unplugin-vue-components），
// 不再 app.use(ElementPlus) 全量注册，显著减小生产包体积。

// 图标仍需全局注册：菜单 item.icon 以名称字符串动态 :is 渲染，无法静态分析
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)

app.mount('#app')
