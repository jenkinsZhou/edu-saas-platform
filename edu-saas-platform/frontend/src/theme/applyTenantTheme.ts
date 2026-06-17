import { ref } from 'vue'
import { DEFAULT_BRAND_ICON } from './brandIcons'

export interface TenantTheme {
  primaryColor: string
  accentColor: string
  surfaceColor: string
  sidebarColor: string
  sidebarTextColor: string
  brandIcon?: string
}

/** 当前品牌图标 key，跨组件响应式共享（侧边栏 Logo、登录页等订阅）。 */
export const brandIcon = ref<string>(DEFAULT_BRAND_ICON)

export function applyTenantTheme(theme: TenantTheme) {
  const root = document.documentElement
  root.style.setProperty('--edu-primary', theme.primaryColor)
  root.style.setProperty('--edu-accent', theme.accentColor)
  root.style.setProperty('--edu-surface', theme.surfaceColor)
  root.style.setProperty('--edu-sidebar', theme.sidebarColor)
  root.style.setProperty('--edu-sidebar-text', theme.sidebarTextColor)
  root.style.setProperty('--el-color-primary', theme.primaryColor)
  if (theme.brandIcon) {
    brandIcon.value = theme.brandIcon
  }
}
