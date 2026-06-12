<template>
  <div class="register-container">
    <div class="register-content">
      <a-card class="register-card" :bordered="false">
        <div class="register-header">
          <div class="logo">
            <div class="logo-icon">
              <BookOutlined />
            </div>
            <div class="logo-text">
              <h1>EduSphere</h1>
              <p>新机构入驻</p>
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
          <a-form-item name="organizationName" label="机构名称">
            <a-input
              v-model:value="form.organizationName"
              placeholder="例如：阳光教育培训中心"
              :maxlength="50"
            >
              <template #prefix>
                <HomeOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="organizationCode" label="机构编码" extra="登录时使用，小写字母开头，可含数字和连字符">
            <a-input
              v-model:value="form.organizationCode"
              placeholder="例如：sunshine-edu"
              :maxlength="32"
            >
              <template #prefix>
                <BankOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="adminUsername" label="管理员账号">
            <a-input
              v-model:value="form.adminUsername"
              placeholder="字母开头，可含字母、数字、下划线"
              autocomplete="username"
              :maxlength="32"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="adminPassword" label="管理员密码">
            <a-input-password
              v-model:value="form.adminPassword"
              placeholder="至少8位"
              autocomplete="new-password"
              :maxlength="64"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item name="confirmPassword" label="确认密码">
            <a-input-password
              v-model:value="form.confirmPassword"
              placeholder="再次输入密码"
              autocomplete="new-password"
              :maxlength="64"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item name="planCode" label="选择套餐">
            <div class="plan-list">
              <div
                v-for="plan in plans"
                :key="plan.code"
                class="plan-card"
                :class="{ active: form.planCode === plan.code }"
                @click="form.planCode = plan.code"
              >
                <div class="plan-name">{{ plan.name }}</div>
                <div class="plan-price">{{ plan.price }}</div>
                <div class="plan-desc">{{ plan.desc }}</div>
              </div>
            </div>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              block
              size="large"
              :loading="loading"
            >
              立即开通
            </a-button>
          </a-form-item>
        </a-form>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">返回登录</router-link>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { UserOutlined, LockOutlined, BankOutlined, HomeOutlined, BookOutlined } from '@ant-design/icons-vue'
import { apiPublicPost } from '../api/http'

const router = useRouter()
const loading = ref(false)

const plans = [
  { code: 'basic', name: '基础版', price: '免费试用', desc: '单校区 · 核心教务功能' },
  { code: 'standard', name: '标准版', price: '¥299/月', desc: '多校区 · 报表与营销' },
  { code: 'premium', name: '旗舰版', price: '¥899/月', desc: '全部功能 · 专属支持' }
]

const form = reactive({
  organizationName: '',
  organizationCode: '',
  adminUsername: 'admin',
  adminPassword: '',
  confirmPassword: '',
  planCode: 'standard'
})

const rules: Record<string, Rule[]> = {
  organizationName: [
    { required: true, message: '请输入机构名称' },
    { min: 2, max: 50, message: '长度需在2-50个字符之间' }
  ],
  organizationCode: [
    { required: true, message: '请输入机构编码' },
    { pattern: /^[a-z][a-z0-9-]{1,31}$/, message: '小写字母开头，仅含小写字母、数字、连字符，长度2-32' }
  ],
  adminUsername: [
    { required: true, message: '请输入管理员账号' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,31}$/, message: '字母开头，仅含字母、数字、下划线，长度3-32' }
  ],
  adminPassword: [
    { required: true, message: '请输入密码' },
    { min: 8, max: 64, message: '密码长度需在8-64个字符之间' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码' },
    {
      validator: (_rule: Rule, value: string) =>
        value === form.adminPassword ? Promise.resolve() : Promise.reject('两次输入的密码不一致')
    }
  ],
  planCode: [{ required: true, message: '请选择套餐' }]
}

async function handleSubmit() {
  loading.value = true
  try {
    const result = await apiPublicPost<{ organizationCode: string; adminUsername: string }>(
      '/tenant/register',
      {
        organizationName: form.organizationName,
        organizationCode: form.organizationCode,
        adminUsername: form.adminUsername,
        adminPassword: form.adminPassword,
        planCode: form.planCode
      }
    )
    message.success(`开通成功！请用机构编码 ${result.organizationCode} 登录`)
    await router.replace({ path: '/login', query: { org: result.organizationCode, username: result.adminUsername } })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '开通失败，请稍后再试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(15, 27, 61, 0.96), rgba(30, 64, 175, 0.9)),
    #0f1b3d;
  padding: 32px 16px;
}

.register-content {
  width: 100%;
  max-width: 520px;
}

.register-card {
  border: 1px solid rgba(219, 229, 244, 0.95);
  border-radius: 8px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.28);
}

.register-card :deep(.ant-card-body) {
  padding: 30px;
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  display: inline-flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-accent));
  color: #ffffff;
  font-size: 24px;
}

.logo-text h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 850;
  color: var(--edu-text);
}

.logo-text p {
  margin: 4px 0 0;
  color: rgba(0, 0, 0, 0.45);
}

.plan-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.plan-card {
  border: 1px solid var(--edu-border);
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.plan-card:hover {
  border-color: var(--edu-secondary);
  box-shadow: 0 8px 20px rgba(30, 64, 175, 0.12);
}

.plan-card.active {
  border-color: var(--edu-primary);
  background: #eff6ff;
}

.plan-name {
  font-weight: 600;
  font-size: 15px;
}

.plan-price {
  color: var(--edu-primary);
  font-weight: 700;
  margin: 4px 0;
}

.plan-desc {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.register-footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
}

.register-footer a {
  margin-left: 4px;
  font-weight: 750;
}

@media (max-width: 560px) {
  .plan-list {
    grid-template-columns: 1fr;
  }

  .register-card :deep(.ant-card-body) {
    padding: 22px;
  }
}
</style>
