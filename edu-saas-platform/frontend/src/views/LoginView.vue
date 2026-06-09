<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <span>EduSphere</span>
        <strong>教培机构管理后台</strong>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="机构编码">
          <el-input v-model="form.organizationCode" autocomplete="organization" placeholder="demo" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="login-button" type="primary" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/http'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({
  organizationCode: 'demo',
  username: '',
  password: ''
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await login({
      organizationCode: form.organizationCode || undefined,
      username: form.username,
      password: form.password
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
