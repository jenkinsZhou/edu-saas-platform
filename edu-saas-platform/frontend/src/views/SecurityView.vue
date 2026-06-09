<template>
  <h1 class="page-title">账号权限</h1>
  <div class="stack">
    <div class="panel">
      <div class="section-head">
        <strong>账号</strong>
        <el-button type="primary" @click="openCreateAccount">新增账号</el-button>
      </div>
      <el-form class="filter-bar" inline>
        <el-form-item label="关键词">
          <el-input v-model="accountFilters.keyword" clearable placeholder="账号/手机号/邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="accountFilters.status" clearable style="width: 140px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchAccounts">查询</el-button>
          <el-button @click="resetAccountFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="accounts" style="width: 100%">
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="校区范围" min-width="160">
          <template #default="{ row }">
            {{ campusNames(row.campusIds) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditAccount(row)">编辑</el-button>
            <el-button link type="primary" @click="openPasswordDialog(row)">重置密码</el-button>
            <el-button link type="primary" @click="toggleAccountStatus(row)">
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="accountPager.page"
          v-model:page-size="accountPager.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="accountPager.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadAccounts"
          @size-change="loadAccounts"
        />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>角色</strong>
        <el-button type="primary" @click="openCreateRole">新增角色</el-button>
      </div>
      <el-form class="filter-bar" inline>
        <el-form-item label="关键词">
          <el-input v-model="roleFilters.keyword" clearable placeholder="角色名/编码" />
        </el-form-item>
        <el-form-item label="数据权限">
          <el-select v-model="roleFilters.dataScope" clearable style="width: 160px">
            <el-option label="全部数据" value="ALL" />
            <el-option label="机构数据" value="TENANT" />
            <el-option label="校区数据" value="CAMPUS" />
            <el-option label="自定义数据" value="CUSTOM" />
            <el-option label="本人数据" value="OWNER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchRoles">查询</el-button>
          <el-button @click="resetRoleFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="roles" style="width: 100%">
        <el-table-column prop="name" label="角色" />
        <el-table-column prop="code" label="编码" />
        <el-table-column prop="dataScope" label="数据权限" />
        <el-table-column prop="systemBuiltin" label="内置" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditRole(row)">编辑</el-button>
            <el-button link type="primary" @click="openPermissionDialog(row)">授权</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="rolePager.page"
          v-model:page-size="rolePager.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="rolePager.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadRoles"
          @size-change="loadRoles"
        />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>菜单权限</strong>
        <el-button type="primary" @click="openCreateMenu">新增菜单</el-button>
      </div>
      <el-form class="filter-bar" inline>
        <el-form-item label="关键词">
          <el-input v-model="menuFilters.keyword" clearable placeholder="名称/路径/权限码" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="menuFilters.type" clearable style="width: 140px">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchMenus">查询</el-button>
          <el-button @click="resetMenuFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="menus" row-key="id" style="width: 100%; margin-top: 12px">
        <el-table-column prop="name" label="菜单/按钮" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="path" label="路径" />
        <el-table-column prop="permissionCode" label="权限码" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditMenu(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteMenu(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="menuPager.page"
          v-model:page-size="menuPager.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="menuPager.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadMenus"
          @size-change="loadMenus"
        />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>操作日志</strong>
        <el-button @click="loadOperationLogs">刷新</el-button>
      </div>
      <el-form class="log-filter" inline>
        <el-form-item label="操作人">
          <el-input v-model="logFilters.username" clearable placeholder="admin" />
        </el-form-item>
        <el-form-item label="动作">
          <el-input v-model="logFilters.action" clearable placeholder="UPDATE" />
        </el-form-item>
        <el-form-item label="RequestId">
          <el-input v-model="logFilters.requestId" clearable placeholder="req-" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadOperationLogs">查询</el-button>
          <el-button @click="resetLogFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="operationLogs" style="width: 100%">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="动作" width="180" />
        <el-table-column prop="targetType" label="对象" width="130" />
        <el-table-column prop="targetId" label="对象ID" width="120" />
        <el-table-column prop="requestId" label="RequestId" min-width="220" />
        <el-table-column prop="detail" label="详情" min-width="180" />
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="logPager.page"
          v-model:page-size="logPager.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="logPager.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadOperationLogs"
          @size-change="loadOperationLogs"
        />
      </div>
    </div>
  </div>

  <el-dialog v-model="accountDialogVisible" :title="accountForm.id ? '编辑账号' : '新增账号'" width="520px">
    <el-form label-width="90px">
      <el-form-item label="账号">
        <el-input v-model="accountForm.username" :disabled="Boolean(accountForm.id)" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="accountForm.realName" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="accountForm.phone" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="accountForm.email" />
      </el-form-item>
      <el-form-item v-if="!accountForm.id" label="密码">
        <el-input v-model="accountForm.password" type="password" show-password />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="accountForm.roleIds" multiple style="width: 100%">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="校区范围">
        <el-select v-model="accountForm.campusIds" multiple clearable filterable style="width: 100%">
          <el-option v-for="campus in campusOptions" :key="campus.id" :label="campus.name" :value="campus.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="accountDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveAccount">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="roleDialogVisible" :title="roleForm.id ? '编辑角色' : '新增角色'" width="620px">
    <el-form label-width="90px">
      <el-form-item label="角色名">
        <el-input v-model="roleForm.name" />
      </el-form-item>
      <el-form-item label="编码">
        <el-input v-model="roleForm.code" :disabled="Boolean(roleForm.id)" />
      </el-form-item>
      <el-form-item label="数据权限">
        <el-select v-model="roleForm.dataScope" style="width: 100%">
          <el-option label="全部数据" value="ALL" />
          <el-option label="机构数据" value="TENANT" />
          <el-option label="校区数据" value="CAMPUS" />
          <el-option label="自定义数据" value="CUSTOM" />
          <el-option label="本人数据" value="OWNER" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!roleForm.id" label="权限">
        <el-select v-model="roleForm.permissionIds" multiple filterable style="width: 100%">
          <el-option
            v-for="menu in menuOptions"
            :key="menu.id"
            :label="`${menu.name} ${menu.permissionCode}`"
            :value="menu.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="roleDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveRole">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="menuDialogVisible" :title="menuForm.id ? '编辑菜单' : '新增菜单'" width="560px">
    <el-form label-width="90px">
      <el-form-item label="父级">
        <el-select v-model="menuForm.parentId" clearable style="width: 100%">
          <el-option label="根节点" :value="0" />
          <el-option v-for="menu in parentMenuOptions" :key="menu.id" :label="menu.name" :value="menu.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="menuForm.name" />
      </el-form-item>
      <el-form-item label="类型">
        <el-segmented v-model="menuForm.type" :options="['MENU', 'BUTTON']" />
      </el-form-item>
      <el-form-item label="路径">
        <el-input v-model="menuForm.routePath" />
      </el-form-item>
      <el-form-item label="权限码">
        <el-input v-model="menuForm.permissionCode" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="menuForm.sortNo" :min="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="menuDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveMenu">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="permissionDialogVisible" :title="`角色授权 - ${currentRoleName}`" width="640px">
    <el-form label-width="90px">
      <el-form-item label="权限">
        <div class="permission-tree">
          <el-tree
            ref="permissionTreeRef"
            :data="permissionTree"
            node-key="id"
            show-checkbox
            default-expand-all
            :props="{ label: 'label', children: 'children' }"
          />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="permissionDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveRolePermissions">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="passwordDialogVisible" :title="`重置密码 - ${passwordForm.username}`" width="420px">
    <el-form label-width="90px">
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.password" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="resetPassword">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type TreeInstance } from 'element-plus'
import { apiDelete, apiGet, apiPost, apiPut } from '../api/http'

interface AccountItem {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  status: string
  roleIds: number[]
  campusIds: number[]
}

interface RoleItem {
  id: number
  name: string
  code: string
  dataScope: string
  systemBuiltin: boolean
}

interface MenuItem {
  id: number
  parentId: number
  name: string
  type: string
  path: string
  permissionCode: string
  sortNo: number
}

interface CampusItem {
  id: number
  name: string
  code: string
  status: string
}

interface PermissionTreeNode {
  id: number
  label: string
  children: PermissionTreeNode[]
}

interface OperationLogItem {
  id: number
  username: string
  module: string
  action: string
  targetType: string
  targetId: string | number
  requestId: string
  detail: string
  createdAt: string
}

interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  summary?: Record<string, unknown>
}

const accounts = ref<AccountItem[]>([])
const roles = ref<RoleItem[]>([])
const menus = ref<MenuItem[]>([])
const roleOptions = ref<RoleItem[]>([])
const menuOptions = ref<MenuItem[]>([])
const campusOptions = ref<CampusItem[]>([])
const operationLogs = ref<OperationLogItem[]>([])

const accountDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const menuDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const currentRoleName = ref('')
const permissionTreeRef = ref<TreeInstance>()
const accountPager = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})
const rolePager = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})
const menuPager = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})
const logPager = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const accountForm = reactive({
  id: 0,
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: 'demo123456',
  roleIds: [] as number[],
  campusIds: [] as number[]
})

const roleForm = reactive({
  id: 0,
  name: '',
  code: '',
  dataScope: 'CUSTOM',
  permissionIds: [] as number[]
})

const menuForm = reactive({
  id: 0,
  parentId: 0,
  name: '',
  type: 'MENU',
  routePath: '',
  permissionCode: '',
  sortNo: 50
})

const permissionForm = reactive({
  roleId: 0,
  permissionIds: [] as number[]
})

const passwordForm = reactive({
  accountId: 0,
  username: '',
  password: 'demo123456'
})

const logFilters = reactive({
  username: '',
  action: '',
  requestId: ''
})
const accountFilters = reactive({
  keyword: '',
  status: ''
})
const roleFilters = reactive({
  keyword: '',
  dataScope: ''
})
const menuFilters = reactive({
  keyword: '',
  type: ''
})

const parentMenuOptions = computed(() => menuOptions.value.filter((menu) => menu.type === 'MENU' && menu.id !== menuForm.id))
const permissionTree = computed(() => buildPermissionTree(menuOptions.value))

async function loadAll() {
  await Promise.all([
    loadAccounts(),
    loadRoles(),
    loadMenus(),
    loadRoleOptions(),
    loadMenuOptions(),
    loadCampusOptions(),
    loadOperationLogs()
  ])
}

function openCreateAccount() {
  resetAccountForm()
  accountDialogVisible.value = true
}

function openEditAccount(account: AccountItem) {
  accountForm.id = account.id
  accountForm.username = account.username
  accountForm.realName = account.realName
  accountForm.phone = account.phone
  accountForm.email = account.email
  accountForm.password = ''
  accountForm.roleIds = [...account.roleIds]
  accountForm.campusIds = [...account.campusIds]
  accountDialogVisible.value = true
}

async function saveAccount() {
  if (accountForm.id) {
    await apiPut<void>(`/system/accounts/${accountForm.id}`, {
      realName: accountForm.realName,
      phone: accountForm.phone,
      email: accountForm.email,
      roleIds: accountForm.roleIds,
      campusIds: accountForm.campusIds
    })
    ElMessage.success('账号已保存')
  } else {
    await apiPost<number>('/system/accounts', {
      ...accountForm,
      status: 'ACTIVE'
    })
    ElMessage.success('账号已创建')
  }
  accountDialogVisible.value = false
  resetAccountForm()
  await loadAll()
}

async function loadAccounts() {
  const params = new URLSearchParams({
    page: String(accountPager.page),
    pageSize: String(accountPager.pageSize)
  })
  if (accountFilters.keyword) {
    params.set('keyword', accountFilters.keyword)
  }
  if (accountFilters.status) {
    params.set('status', accountFilters.status)
  }
  const result = await apiGet<PageResult<AccountItem>>(`/system/accounts?${params.toString()}`)
  accounts.value = result.records
  accountPager.total = result.total
}

async function loadRoleOptions() {
  const result = await apiGet<PageResult<RoleItem>>('/system/roles?page=1&pageSize=100')
  roleOptions.value = result.records
}

async function loadMenuOptions() {
  const result = await apiGet<PageResult<MenuItem>>('/system/menus?page=1&pageSize=100')
  menuOptions.value = result.records
}

async function loadCampusOptions() {
  campusOptions.value = await apiGet<CampusItem[]>('/courses/campuses')
}

async function toggleAccountStatus(account: AccountItem) {
  const nextStatus = account.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await apiPut<void>(`/system/accounts/${account.id}/status`, {
    status: nextStatus
  })
  ElMessage.success(nextStatus === 'ACTIVE' ? '账号已启用' : '账号已停用')
  await loadAll()
}

function openPasswordDialog(account: AccountItem) {
  passwordForm.accountId = account.id
  passwordForm.username = account.username
  passwordForm.password = 'demo123456'
  passwordDialogVisible.value = true
}

async function resetPassword() {
  await apiPut<void>(`/system/accounts/${passwordForm.accountId}/password`, {
    password: passwordForm.password
  })
  passwordDialogVisible.value = false
  ElMessage.success('密码已重置')
  await loadOperationLogs()
}

function openCreateRole() {
  resetRoleForm()
  roleDialogVisible.value = true
}

function openEditRole(role: RoleItem) {
  roleForm.id = role.id
  roleForm.name = role.name
  roleForm.code = role.code
  roleForm.dataScope = role.dataScope
  roleForm.permissionIds = []
  roleDialogVisible.value = true
}

async function saveRole() {
  if (roleForm.id) {
    await apiPut<void>(`/system/roles/${roleForm.id}`, {
      name: roleForm.name,
      dataScope: roleForm.dataScope
    })
    ElMessage.success('角色已保存')
  } else {
    await apiPost<number>('/system/roles', roleForm)
    ElMessage.success('角色已创建')
  }
  roleDialogVisible.value = false
  resetRoleForm()
  await loadAll()
}

async function loadRoles() {
  const params = new URLSearchParams({
    page: String(rolePager.page),
    pageSize: String(rolePager.pageSize)
  })
  if (roleFilters.keyword) {
    params.set('keyword', roleFilters.keyword)
  }
  if (roleFilters.dataScope) {
    params.set('dataScope', roleFilters.dataScope)
  }
  const result = await apiGet<PageResult<RoleItem>>(`/system/roles?${params.toString()}`)
  roles.value = result.records
  rolePager.total = result.total
}

async function loadMenus() {
  const params = new URLSearchParams({
    page: String(menuPager.page),
    pageSize: String(menuPager.pageSize)
  })
  if (menuFilters.keyword) {
    params.set('keyword', menuFilters.keyword)
  }
  if (menuFilters.type) {
    params.set('type', menuFilters.type)
  }
  const result = await apiGet<PageResult<MenuItem>>(`/system/menus?${params.toString()}`)
  menus.value = result.records
  menuPager.total = result.total
}

function resetAccountFilters() {
  accountFilters.keyword = ''
  accountFilters.status = ''
  accountPager.page = 1
  loadAccounts()
}

function resetRoleFilters() {
  roleFilters.keyword = ''
  roleFilters.dataScope = ''
  rolePager.page = 1
  loadRoles()
}

function resetMenuFilters() {
  menuFilters.keyword = ''
  menuFilters.type = ''
  menuPager.page = 1
  loadMenus()
}

function searchAccounts() {
  accountPager.page = 1
  loadAccounts()
}

function searchRoles() {
  rolePager.page = 1
  loadRoles()
}

function searchMenus() {
  menuPager.page = 1
  loadMenus()
}

async function loadOperationLogs() {
  const params = new URLSearchParams()
  params.set('page', String(logPager.page))
  params.set('pageSize', String(logPager.pageSize))
  if (logFilters.username) {
    params.set('username', logFilters.username)
  }
  if (logFilters.action) {
    params.set('action', logFilters.action)
  }
  if (logFilters.requestId) {
    params.set('requestId', logFilters.requestId)
  }
  const result = await apiGet<PageResult<OperationLogItem>>(`/system/operation-logs?${params.toString()}`)
  operationLogs.value = result.records
  logPager.total = result.total
}

async function resetLogFilters() {
  logFilters.username = ''
  logFilters.action = ''
  logFilters.requestId = ''
  logPager.page = 1
  await loadOperationLogs()
}

async function openPermissionDialog(role: RoleItem) {
  permissionForm.roleId = role.id
  currentRoleName.value = role.name
  permissionForm.permissionIds = await apiGet<number[]>(`/system/roles/${role.id}/permissions`)
  permissionDialogVisible.value = true
  requestAnimationFrame(() => {
    permissionTreeRef.value?.setCheckedKeys(permissionForm.permissionIds, false)
  })
}

async function saveRolePermissions() {
  const checkedIds = permissionTreeRef.value?.getCheckedKeys(false) ?? []
  const halfCheckedIds = permissionTreeRef.value?.getHalfCheckedKeys() ?? []
  permissionForm.permissionIds = [...checkedIds, ...halfCheckedIds].map(Number)
  await apiPut<void>(`/system/roles/${permissionForm.roleId}/permissions`, {
    permissionIds: permissionForm.permissionIds
  })
  permissionDialogVisible.value = false
  ElMessage.success('角色权限已保存')
}

function openCreateMenu() {
  resetMenuForm()
  menuDialogVisible.value = true
}

function openEditMenu(menu: MenuItem) {
  menuForm.id = menu.id
  menuForm.parentId = menu.parentId
  menuForm.name = menu.name
  menuForm.type = menu.type
  menuForm.routePath = menu.path
  menuForm.permissionCode = menu.permissionCode
  menuForm.sortNo = menu.sortNo
  menuDialogVisible.value = true
}

async function saveMenu() {
  const payload = {
    parentId: menuForm.parentId,
    name: menuForm.name,
    type: menuForm.type,
    routePath: menuForm.routePath,
    permissionCode: menuForm.permissionCode,
    sortNo: menuForm.sortNo
  }
  if (menuForm.id) {
    await apiPut<void>(`/system/menus/${menuForm.id}`, payload)
  } else {
    await apiPost<number>('/system/menus', payload)
  }
  menuDialogVisible.value = false
  ElMessage.success('菜单已保存')
  await loadAll()
}

async function deleteMenu(id: number) {
  await apiDelete<void>(`/system/menus/${id}`)
  ElMessage.success('菜单已删除')
  await loadAll()
}

function resetAccountForm() {
  accountForm.id = 0
  accountForm.username = ''
  accountForm.realName = ''
  accountForm.phone = ''
  accountForm.email = ''
  accountForm.password = 'demo123456'
  accountForm.roleIds = []
  accountForm.campusIds = []
}

function campusNames(ids: number[]) {
  if (!ids || ids.length === 0) {
    return '全部/未限制'
  }
  const names = ids
    .map((id) => campusOptions.value.find((campus) => campus.id === id)?.name)
    .filter(Boolean)
  return names.length > 0 ? names.join('、') : ids.join('、')
}

function resetRoleForm() {
  roleForm.id = 0
  roleForm.name = ''
  roleForm.code = ''
  roleForm.dataScope = 'CUSTOM'
  roleForm.permissionIds = []
}

function resetMenuForm() {
  menuForm.id = 0
  menuForm.parentId = 0
  menuForm.name = ''
  menuForm.type = 'MENU'
  menuForm.routePath = ''
  menuForm.permissionCode = ''
  menuForm.sortNo = 50
}

function buildPermissionTree(source: MenuItem[]) {
  const nodeMap = new Map<number, PermissionTreeNode>()
  source.forEach((menu) => {
    nodeMap.set(menu.id, {
      id: menu.id,
      label: menu.permissionCode ? `${menu.name} (${menu.permissionCode})` : menu.name,
      children: []
    })
  })

  const roots: PermissionTreeNode[] = []
  source.forEach((menu) => {
    const node = nodeMap.get(menu.id)
    if (!node) {
      return
    }
    const parent = menu.parentId ? nodeMap.get(menu.parentId) : undefined
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

onMounted(loadAll)
</script>

<style scoped>
.stack {
  display: grid;
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filter-bar {
  margin-bottom: 12px;
}

.log-filter {
  margin-bottom: 12px;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.permission-tree {
  width: 100%;
  max-height: 420px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 10px 6px;
}
</style>
