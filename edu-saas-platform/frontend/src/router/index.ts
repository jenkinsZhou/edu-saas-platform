import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../views/AdminLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import CourseView from '../views/CourseView.vue'
import OrderView from '../views/OrderView.vue'
import SecurityView from '../views/SecurityView.vue'
import TenantThemeView from '../views/TenantThemeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import { isAuthenticated } from '../api/http'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
    {
      path: '/',
      component: AdminLayout,
      children: [
        { path: '', name: 'dashboard', component: DashboardView },
        { path: 'courses', name: 'courses', component: CourseView },
        { path: 'orders', name: 'orders', component: OrderView },
        { path: 'security', name: 'security', component: SecurityView },
        { path: 'tenant-theme', name: 'tenant-theme', component: TenantThemeView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  if (!isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})
