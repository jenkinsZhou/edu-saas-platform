export interface TenantTheme {
  primaryColor: string
  accentColor: string
  surfaceColor: string
  sidebarColor: string
  sidebarTextColor: string
}

export function applyTenantTheme(theme: TenantTheme) {
  const root = document.documentElement
  root.style.setProperty('--edu-primary', theme.primaryColor)
  root.style.setProperty('--edu-accent', theme.accentColor)
  root.style.setProperty('--edu-surface', theme.surfaceColor)
  root.style.setProperty('--edu-sidebar', theme.sidebarColor)
  root.style.setProperty('--edu-sidebar-text', theme.sidebarTextColor)
  root.style.setProperty('--el-color-primary', theme.primaryColor)
}
