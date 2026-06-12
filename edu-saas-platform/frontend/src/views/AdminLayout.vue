<template>
  <a-layout class="admin-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      theme="dark"
      width="240"
    >
      <div class="logo">
        <div class="logo-icon">
          <BookOutlined />
        </div>
        <transition name="fade">
          <div v-if="!collapsed" class="logo-copy">
            <span class="logo-text">EduSphere</span>
            <span class="logo-subtitle">校区运营中台</span>
          </div>
        </transition>
      </div>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        :items="menuItems"
        @click="handleMenuClick"
      />
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="layout-header">
        <div class="header-left">
          <MenuUnfoldOutlined
            v-if="collapsed"
            class="trigger"
            aria-label="展开侧边栏"
            @click="() => (collapsed = !collapsed)"
          />
          <MenuFoldOutlined
            v-else
            class="trigger"
            aria-label="收起侧边栏"
            @click="() => (collapsed = !collapsed)"
          />
          <div class="header-copy">
            <span class="header-title">教育 SaaS 管理平台</span>
            <span class="header-subtitle">课程、订单、权限与机构主题集中管理</span>
          </div>
        </div>

        <div class="header-right">
          <a-space :size="16">
            <a-badge :count="5">
              <BellOutlined class="header-icon" />
            </a-badge>

            <a-dropdown>
              <div class="user-info">
                <a-avatar :size="34" class="user-avatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <span class="user-name">{{ userLabel }}</span>
              </div>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="profile">
                    <UserOutlined />
                    个人中心
                  </a-menu-item>
                  <a-menu-item key="settings">
                    <SettingOutlined />
                    系统设置
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="handleLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </a-layout-header>

      <a-layout-content class="layout-content">
        <div class="content-wrapper">
          <RouterView />
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  BookOutlined,
  ShoppingOutlined,
  SafetyOutlined,
  BgColorsOutlined,
  UserOutlined,
  BellOutlined,
  SettingOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { apiGet, getLoginUser, logout } from '../api/http'

interface NavMenu {
  id: number
  parentId: number
  name: string
  path: string
  permissionCode: string
  sortNo: number
}

interface NavUser {
  accountId: number
  userId: number
  tenantId: number
  roles: string[]
  permissions: string[]
  campusIds: number[]
}

interface NavPayload {
  user: NavUser
  menus: NavMenu[]
  permissionIds: number[]
}

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const selectedKeys = ref<string[]>(['/'])
const navPayload = ref<NavPayload>()
const loginUser = getLoginUser()

const fallbackMenus: NavMenu[] = [
  { id: 1, parentId: 0, name: '运营总览', path: '/', permissionCode: 'dashboard:view', sortNo: 10 },
  { id: 2, parentId: 0, name: '课程中心', path: '/courses', permissionCode: 'course:product:view', sortNo: 20 },
  { id: 3, parentId: 0, name: '订单中心', path: '/orders', permissionCode: 'order:order:view', sortNo: 30 },
  { id: 4, parentId: 0, name: '账号权限', path: '/security', permissionCode: 'system:role:view', sortNo: 40 },
  { id: 5, parentId: 0, name: '机构主题', path: '/tenant-theme', permissionCode: 'tenant:theme:view', sortNo: 50 }
]

const menus = ref<NavMenu[]>(fallbackMenus)

const menuIcons: Record<string, any> = {
  '/': DashboardOutlined,
  '/courses': BookOutlined,
  '/orders': ShoppingOutlined,
  '/security': SafetyOutlined,
  '/tenant-theme': BgColorsOutlined
}

const menuItems = computed(() => {
  return menus.value
    .filter(menu => menu.path)
    .sort((a, b) => a.sortNo - b.sortNo)
    .map(menu => ({
      key: menu.path,
      icon: () => {
        const Icon = menuIcons[menu.path]
        return Icon ? h(Icon) : null
      },
      label: menu.name
    }))
})

const userLabel = computed(() => {
  const user = navPayload.value?.user
  if (!user) {
    return loginUser?.organizationName ?? '演示机构'
  }
  return `${loginUser?.organizationName ?? '机构'}`
})

watch(() => route.path, (newPath) => {
  selectedKeys.value = [newPath]
}, { immediate: true })

onMounted(async () => {
  try {
    const data = await apiGet<NavPayload>('/system/nav')
    navPayload.value = data
    if (data.menus && data.menus.length > 0) {
      menus.value = data.menus
    }
  } catch (error) {
    console.warn('加载菜单失败，使用默认菜单', error)
  }
})

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

async function handleLogout() {
  try {
    await logout()
    message.success('退出成功')
    await router.replace('/login')
  } catch (error) {
    message.error('退出失败')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-layout :deep(.ant-layout-sider) {
  background:
    linear-gradient(180deg, rgba(59, 130, 246, 0.16), rgba(15, 27, 61, 0) 260px),
    var(--edu-sidebar);
  box-shadow: 8px 0 28px rgba(15, 23, 42, 0.14);
  z-index: 20;
}

.admin-layout :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
}

.admin-layout :deep(.ant-menu-dark) {
  flex: 1;
  padding: 12px 10px;
  background: transparent;
}

.admin-layout :deep(.ant-menu-dark .ant-menu-item) {
  height: 42px;
  margin: 4px 0;
  border-radius: 8px;
  color: rgba(219, 234, 254, 0.84);
}

.admin-layout :deep(.ant-menu-dark .ant-menu-item:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.09);
}

.admin-layout :deep(.ant-menu-dark .ant-menu-item-selected) {
  color: #ffffff;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95), rgba(30, 64, 175, 0.95));
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.3);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(219, 234, 254, 0.12);
}

.logo-icon {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #d97706);
  color: #ffffff;
  font-size: 20px;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.22);
}

.logo-copy {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 0;
}

.logo-subtitle {
  color: rgba(219, 234, 254, 0.68);
  font-size: 12px;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.layout-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(219, 229, 244, 0.88);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.05);
  position: sticky;
  top: 0;
  z-index: 15;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.trigger {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #334155;
  font-size: 18px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.trigger:hover {
  color: var(--edu-primary);
  background: #eff6ff;
}

.header-copy {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.header-title {
  color: var(--edu-text);
  font-size: 16px;
  font-weight: 800;
}

.header-subtitle {
  color: var(--edu-text-muted);
  font-size: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-icon {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #475569;
  font-size: 18px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.header-icon:hover {
  color: var(--edu-primary);
  background: #eff6ff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 3px 6px 3px 3px;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.user-info:hover {
  border-color: var(--edu-border);
  background: #f8fafc;
}

.user-avatar {
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-secondary));
}

.user-name {
  color: #334155;
  font-size: 14px;
  font-weight: 650;
}

.layout-content {
  padding: 22px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.08), transparent 340px),
    var(--edu-page);
  min-height: calc(100vh - 64px);
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .admin-layout {
    display: block;
  }

  .admin-layout :deep(.ant-layout-sider) {
    display: none;
  }

  .admin-layout :deep(.ant-layout) {
    width: 100%;
    flex: 1 1 auto;
    min-width: 0;
  }

  .layout-header {
    padding: 0 14px;
  }

  .header-subtitle,
  .user-name {
    display: none;
  }

  .layout-content {
    padding: 14px;
  }
}
</style>
