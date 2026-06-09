import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import App from './App.vue'
import { router } from './router'
import { apiGet, isAuthenticated } from './api/http'
import { applyTenantTheme, type TenantTheme } from './theme/applyTenantTheme'

applyTenantTheme({
  primaryColor: '#2563eb',
  accentColor: '#16a34a',
  surfaceColor: '#ffffff',
  sidebarColor: '#111827',
  sidebarTextColor: '#e5e7eb'
})

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus)
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
