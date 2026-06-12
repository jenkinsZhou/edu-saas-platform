<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="账号权限" sub-title="角色管理、权限分配">
        <template #extra>
          <a-space>
            <a-button type="primary" @click="openCreateRole">
              <template #icon><PlusOutlined /></template>
              新增角色
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="8">
        <a-card title="角色列表" :bordered="false">
          <template #extra>
            <a-button type="link" size="small" @click="loadRoles">刷新</a-button>
          </template>

          <a-list :data-source="roles" :loading="rolesLoading">
            <template #renderItem="{ item }">
              <a-list-item>
                <template #actions>
                  <a @click="selectRole(item)">配置</a>
                  <a @click="editRole(item)">编辑</a>
                </template>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <a-tag v-if="item.systemBuiltin" color="blue">系统</a-tag>
                      <span>{{ item.name }}</span>
                    </a-space>
                  </template>
                  <template #description>
                    <div>{{ item.description || '暂无描述' }}</div>
                    <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px">
                      数据范围：{{ getDataScopeText(item.dataScope) }}
                    </div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :span="16">
        <a-card :title="`权限配置 - ${selectedRole?.name || '请选择角色'}`" :bordered="false">
          <template v-if="selectedRole">
            <a-alert
              message="提示"
              description="勾选权限后点击保存按钮生效"
              type="info"
              show-icon
              style="margin-bottom: 16px"
            />

            <a-tree
              v-model:checkedKeys="checkedPermissions"
              :tree-data="permissionTree"
              :field-names="{ title: 'name', key: 'id', children: 'children' }"
              checkable
              :selectable="false"
            >
              <template #title="{ name, permissionCode }">
                <span>{{ name }}</span>
                <span style="color: #8c8c8c; margin-left: 8px; font-size: 12px">{{ permissionCode }}</span>
              </template>
            </a-tree>

            <a-divider />

            <a-space>
              <a-button type="primary" :loading="saving" @click="savePermissions">
                保存权限
              </a-button>
              <a-button @click="selectedRole = null">取消</a-button>
            </a-space>
          </template>

          <a-empty v-else description="请从左侧选择角色" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { apiGet, apiPut } from '../api/http'

const rolesLoading = ref(false)
const saving = ref(false)
const roles = ref<any[]>([])
const permissionTree = ref<any[]>([])
const selectedRole = ref<any>(null)
const checkedPermissions = ref<number[]>([])

onMounted(() => {
  loadRoles()
  loadPermissions()
})

async function loadRoles() {
  rolesLoading.value = true
  try {
    const res = await apiGet<any>('/system/roles')
    roles.value = res.records || []
  } catch (error) {
    message.error('加载角色失败')
  } finally {
    rolesLoading.value = false
  }
}

async function loadPermissions() {
  try {
    const res = await apiGet<any>('/system/menus', { pageSize: 100 })
    permissionTree.value = buildTree(res.records || [])
  } catch (error) {
    message.error('加载权限失败')
  }
}

function buildTree(items: any[]) {
  const map = new Map()
  const roots: any[] = []

  items.forEach(item => {
    map.set(item.id, { ...item, children: [] })
  })

  items.forEach(item => {
    const node = map.get(item.id)
    if (item.parentId === 0) {
      roots.push(node)
    } else {
      const parent = map.get(item.parentId)
      if (parent) {
        parent.children.push(node)
      }
    }
  })

  return roots
}

async function selectRole(role: any) {
  selectedRole.value = role
  try {
    const res = await apiGet<any>(`/system/roles/${role.id}/permissions`)
    checkedPermissions.value = res.permissionIds || []
  } catch (error) {
    message.error('加载角色权限失败')
  }
}

async function savePermissions() {
  if (!selectedRole.value) return

  saving.value = true
  try {
    await apiPut(`/system/roles/${selectedRole.value.id}/permissions`, {
      permissionIds: checkedPermissions.value
    })
    message.success('保存成功')
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function openCreateRole() {
  message.info('新增角色功能开发中')
}

function editRole(role: any) {
  message.info('编辑角色功能开发中')
}

function getDataScopeText(scope: string) {
  const texts: Record<string, string> = {
    ALL: '全部数据',
    CAMPUS: '本校区',
    SELF: '仅本人'
  }
  return texts[scope] || scope
}
</script>

<style scoped>
.page-container {
  padding: 0;
}
</style>
