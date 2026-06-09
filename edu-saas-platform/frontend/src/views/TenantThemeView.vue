<template>
  <h1 class="page-title">机构主题</h1>
  <div class="panel">
    <el-form label-width="100px">
      <el-form-item label="主色">
        <el-color-picker v-model="theme.primaryColor" @change="preview" />
      </el-form-item>
      <el-form-item label="强调色">
        <el-color-picker v-model="theme.accentColor" @change="preview" />
      </el-form-item>
      <el-form-item label="侧边栏">
        <el-color-picker v-model="theme.sidebarColor" @change="preview" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="preview">预览主题</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { applyTenantTheme } from '../theme/applyTenantTheme'

const theme = reactive({
  primaryColor: '#2563eb',
  accentColor: '#16a34a',
  surfaceColor: '#ffffff',
  sidebarColor: '#111827',
  sidebarTextColor: '#e5e7eb'
})

function preview() {
  applyTenantTheme(theme)
}
</script>
