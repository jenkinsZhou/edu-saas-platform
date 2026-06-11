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
        <div class="logo-icon">📚</div>
        <transition name="fade">
          <span v-if="!collapsed" class="logo-text">EduSphere</span>
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
            @click="() => (collapsed = !collapsed)"
          />
          <MenuFoldOutlined
            v-else
            class="trigger"
            @click="() => (collapsed = !collapsed)"
          />
          <span class="header-title">教育SaaS管理平台</span>
        </div>

        <div class="header-right">
          <a-space :size="16">
            <a-badge :count="5">
              <BellOutlined class="header-icon" />
            </a-badge>

            <a-dropdown>
              <div class="user-info">
                <a-avatar :size="32" style="background-color: #1890ff">
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

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 32px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}

.trigger:hover {
  color: #1890ff;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-icon {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}

.header-icon:hover {
  color: #1890ff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
}

.layout-content {
  padding: 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
