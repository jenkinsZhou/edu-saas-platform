<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand">EduSphere</div>
      <nav>
        <RouterLink v-for="menu in visibleMenus" :key="menu.id" class="nav-link" :to="menu.path">
          <component :is="menuIcon(menu.path)" class="nav-icon" />
          <span>{{ menu.name }}</span>
        </RouterLink>
      </nav>
    </aside>
    <main class="admin-main">
      <header class="topbar">
        <strong>教培 SaaS 管理平台</strong>
        <div class="topbar-actions">
          <span>{{ userLabel }}</span>
          <el-button size="small" @click="handleLogout">退出</el-button>
        </div>
      </header>
      <section class="page-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Document, Goods, Lock, Setting } from '@element-plus/icons-vue'
import { apiGet, getLoginUser, logout } from '../api/http'

interface NavMenu {
  id: number
  parentId: number
  name: string
  path: string
  permissionCode: string
  sortNo: number
  children?: NavMenu[]
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

const fallbackMenus: NavMenu[] = [
  { id: 1, parentId: 0, name: '运营总览', path: '/', permissionCode: 'dashboard:view', sortNo: 10 },
  { id: 2, parentId: 0, name: '课程中心', path: '/courses', permissionCode: 'course:product:view', sortNo: 20 },
  { id: 3, parentId: 0, name: '订单中心', path: '/orders', permissionCode: 'order:order:view', sortNo: 30 },
  { id: 4, parentId: 0, name: '账号权限', path: '/security', permissionCode: 'system:role:view', sortNo: 40 },
  { id: 5, parentId: 0, name: '机构主题', path: '/tenant-theme', permissionCode: 'tenant:theme:view', sortNo: 50 }
]

const navPayload = ref<NavPayload>()
const menus = ref<NavMenu[]>(fallbackMenus)
const router = useRouter()
const loginUser = getLoginUser()

const visibleMenus = computed(() => menus.value.filter((menu) => menu.path).sort((a, b) => a.sortNo - b.sortNo))
const userLabel = computed(() => {
  const user = navPayload.value?.user
  if (!user) {
    return loginUser?.organizationName ?? '演示机构'
  }
  return `${loginUser?.organizationName ?? '机构'} · 账号 ${user.accountId}`
})

onMounted(async () => {
  try {
    navPayload.value = await apiGet<NavPayload>('/system/nav')
    menus.value = navPayload.value.menus.length > 0 ? navPayload.value.menus : fallbackMenus
  } catch (error) {
    console.warn('加载动态菜单失败，使用默认菜单', error)
  }
})

function menuIcon(path: string) {
  if (path === '/') {
    return DataAnalysis
  }
  if (path.startsWith('/courses')) {
    return Document
  }
  if (path.startsWith('/orders')) {
    return Goods
  }
  if (path.startsWith('/security')) {
    return Lock
  }
  return Setting
}

async function handleLogout() {
  await logout()
  await router.replace('/login')
}
</script>
