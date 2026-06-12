<template>
  <div class="auth-page">
    <section class="auth-visual" aria-label="EduSphere 产品概览">
      <div class="auth-brand">
        <span class="auth-logo">
          <BookOutlined />
        </span>
        <div>
          <h1>EduSphere</h1>
          <p>面向多校区教培机构的运营中台</p>
        </div>
      </div>

      <div class="auth-board">
        <div class="board-header">
          <span>今日校区概览</span>
          <a-tag color="blue">实时</a-tag>
        </div>
        <div class="board-grid">
          <div>
            <strong>1,286</strong>
            <span>在读学员</span>
          </div>
          <div>
            <strong>93.6%</strong>
            <span>出勤率</span>
          </div>
          <div>
            <strong>842</strong>
            <span>本月课次</span>
          </div>
        </div>
        <div class="board-bars">
          <span style="width: 78%"></span>
          <span style="width: 64%"></span>
          <span style="width: 88%"></span>
        </div>
      </div>
    </section>

    <section class="auth-panel">
      <a-card class="login-card" :bordered="false">
        <div class="login-header">
          <span class="eyebrow">机构工作台</span>
          <h2>欢迎回来</h2>
          <p>登录后继续处理课程、订单和审批事项</p>
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
          <div class="demo-account">
            <span>demo</span>
            <span>admin</span>
            <span>demo123456</span>
          </div>
          <div class="register-entry">
            <span>还没有机构？</span>
            <router-link to="/register">新机构入驻</router-link>
          </div>
        </div>
      </a-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, BankOutlined, BookOutlined } from '@ant-design/icons-vue'
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
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.92fr);
  background:
    linear-gradient(135deg, rgba(15, 27, 61, 0.98), rgba(30, 64, 175, 0.92)),
    #0f1b3d;
}

.auth-visual {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(36px, 5vw, 72px);
  color: #ffffff;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.auth-logo {
  display: inline-flex;
  width: 54px;
  height: 54px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #d97706);
  font-size: 26px;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.26);
}

.auth-brand h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 58px);
  font-weight: 850;
  letter-spacing: 0;
}

.auth-brand p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 16px;
}

.auth-board {
  width: min(620px, 100%);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.26);
  backdrop-filter: blur(18px);
}

.board-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.86);
  font-weight: 750;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.board-grid div {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
}

.board-grid strong {
  font-family: "Fira Code", "SFMono-Regular", Consolas, monospace;
  font-size: 26px;
}

.board-grid span {
  color: rgba(255, 255, 255, 0.66);
  font-size: 12px;
}

.board-bars {
  display: grid;
  gap: 10px;
  margin-top: 22px;
}

.board-bars span {
  display: block;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #93c5fd, #fbbf24);
}

.auth-panel {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 32px;
  background: var(--edu-page);
}

.login-card {
  width: min(440px, 100%);
  border: 1px solid rgba(219, 229, 244, 0.95);
  box-shadow: var(--edu-shadow-md);
}

.login-card :deep(.ant-card-body) {
  padding: 30px;
}

.login-header {
  margin-bottom: 24px;
}

.eyebrow {
  color: var(--edu-accent);
  font-size: 12px;
  font-weight: 800;
}

.login-header h2 {
  margin: 6px 0 6px;
  color: var(--edu-text);
  font-size: 30px;
  font-weight: 850;
}

.login-header p {
  margin: 0;
  color: var(--edu-text-muted);
}

.login-footer {
  display: grid;
  gap: 14px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--edu-border);
  color: var(--edu-text-muted);
  font-size: 13px;
}

.demo-account {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-account span {
  padding: 4px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: var(--edu-primary);
  font-family: "Fira Code", "SFMono-Regular", Consolas, monospace;
  font-weight: 650;
}

.register-entry a {
  margin-left: 6px;
  font-weight: 750;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: auto;
    padding: 32px 22px 24px;
  }

  .auth-board {
    margin-top: 34px;
  }

  .auth-panel {
    min-height: auto;
    padding: 22px;
  }
}

@media (max-width: 520px) {
  .board-grid {
    grid-template-columns: 1fr;
  }

  .login-card :deep(.ant-card-body) {
    padding: 22px;
  }
}
</style>
