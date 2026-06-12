import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/theme.css'
import App from './App.vue'
import { router } from './router'
import { apiGet, isAuthenticated } from './api/http'
import { applyTenantTheme, type TenantTheme } from './theme/applyTenantTheme'

applyTenantTheme({
  primaryColor: '#1e40af',
  accentColor: '#d97706',
  surfaceColor: '#ffffff',
  sidebarColor: '#0f1b3d',
  sidebarTextColor: '#dbeafe'
})

createApp(App)
  .use(createPinia())
  .use(router)
  .use(Antd)
  .mount('#app')

if (isAuthenticated()) {
  void loadTenantTheme()
}

async function loadTenantTheme() {
  try {
    const theme = await apiGet<TenantTheme>('/tenant/theme')
    applyTenantTheme(theme)
  } catch (error) {
    console.warn('加载机构主题失败，使用默认主题', error)
  }
}
