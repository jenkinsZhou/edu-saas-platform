<template>
  <div class="login-container">
    <div class="login-content">
      <a-card class="login-card" :bordered="false">
        <div class="login-header">
          <div class="logo">
            <div class="logo-icon">📚</div>
            <div class="logo-text">
              <h1>EduSphere</h1>
              <p>教育SaaS管理平台</p>
            </div>
          </div>
        </div>

        <a-form
          :model="form"
          :rules="rules"
          @finish="handleSubmit"
          layout="vertical"
          size="large"
        >
          <a-form-item name="organizationCode" label="机构编码">
            <a-input
              v-model:value="form.organizationCode"
              placeholder="请输入机构编码（默认：demo）"
              autocomplete="organization"
            >
              <template #prefix>
                <BankOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="username" label="账号">
            <a-input
              v-model:value="form.username"
              placeholder="请输入账号"
              autocomplete="username"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password" label="密码">
            <a-input-password
              v-model:value="form.password"
              placeholder="请输入密码"
              autocomplete="current-password"
              @pressEnter="handleSubmit"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              block
              size="large"
              :loading="loading"
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>

        <div class="login-footer">
          <a-space :size="16">
            <span>机构编码：demo</span>
            <a-divider type="vertical" />
            <span>账号：admin</span>
            <a-divider type="vertical" />
            <span>密码：demo123456</span>
          </a-space>
          <div class="register-entry">
            <span>还没有机构？</span>
            <router-link to="/register">新机构入驻</router-link>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, BankOutlined } from '@ant-design/icons-vue'
import { login } from '../api/http'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const form = reactive({
  organizationCode: typeof route.query.org === 'string' ? route.query.org : 'demo',
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: ''
})

const rules = {
  organizationCode: [{ required: true, message: '请输入机构编码' }],
  username: [{ required: true, message: '请输入账号' }],
  password: [{ required: true, message: '请输入密码' }]
}

async function handleSubmit() {
  loading.value = true
  try {
    await login({
      organizationCode: form.organizationCode || undefined,
      username: form.username,
      password: form.password
    })
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.login-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 450px;
  padding: 20px;
}

.login-card {
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border-radius: 16px;
  overflow: hidden;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.logo-icon {
  font-size: 48px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.logo-text h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-text p {
  margin: 4px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  color: #8c8c8c;
  font-size: 14px;
}

.register-entry {
  margin-top: 12px;
}

.register-entry a {
  margin-left: 4px;
}
</style>
