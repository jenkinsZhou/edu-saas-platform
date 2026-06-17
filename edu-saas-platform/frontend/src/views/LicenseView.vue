<template>
  <div class="license-page">
    <a-card :bordered="false" class="license-card">
      <template #title>
        <span class="card-title">授权许可</span>
      </template>
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadInfo">刷新</a-button>
          <a-button :loading="reloading" @click="reloadFromSource">重新加载授权</a-button>
        </a-space>
      </template>

      <a-spin :spinning="loading">
        <a-alert
          v-if="info"
          class="status-alert"
          :type="alertType"
          :message="info.message"
          show-icon
        />

        <a-descriptions v-if="info" bordered :column="2" size="middle" class="desc">
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor">{{ statusText }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="授权客户">{{ info.customer || '—' }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{ info.edition || '—' }}</a-descriptions-item>
          <a-descriptions-item label="授权编号">{{ info.licenseId || '—' }}</a-descriptions-item>
          <a-descriptions-item label="签发日期">{{ info.issuedAt || '—' }}</a-descriptions-item>
          <a-descriptions-item label="到期日期">{{ expiresText }}</a-descriptions-item>
          <a-descriptions-item label="宽限期">{{ info.graceDays ?? '—' }} 天</a-descriptions-item>
          <a-descriptions-item label="距到期">{{ daysText }}</a-descriptions-item>
          <a-descriptions-item label="账号上限">{{ capText(info.maxAccounts) }}</a-descriptions-item>
          <a-descriptions-item label="学员上限">{{ capText(info.maxStudents) }}</a-descriptions-item>
          <a-descriptions-item label="校区上限">{{ capText(info.maxCampuses) }}</a-descriptions-item>
          <a-descriptions-item label="授权模块" :span="2">
            <template v-if="info.features && info.features.length">
              <a-tag v-for="f in info.features" :key="f" color="blue">{{ featureLabel(f) }}</a-tag>
            </template>
            <span v-else>—</span>
          </a-descriptions-item>
          <a-descriptions-item label="本机机器码" :span="2">
            <a-typography-text copyable code>{{ info.machineFingerprint }}</a-typography-text>
            <span v-if="info.boundFingerprint" class="bound-hint">
              （授权绑定：{{ info.boundFingerprint }}）
            </span>
            <span v-else class="bound-hint">（未绑定，任意部署可用）</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-card>

    <a-card :bordered="false" class="license-card activate-card">
      <template #title><span class="card-title">激活 / 更新授权</span></template>
      <p class="hint">
        将厂商签发的授权文件内容粘贴到下方并提交。若需绑定本机，请先把上方“本机机器码”反馈给厂商。
      </p>
      <a-textarea
        v-model:value="licenseText"
        :rows="6"
        placeholder="粘贴授权字符串（形如 xxxxx.yyyyy）"
        allow-clear
      />
      <div class="activate-actions">
        <a-button type="primary" :loading="activating" :disabled="!licenseText.trim()" @click="activate">
          提交激活
        </a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { apiGet, apiPost } from '../api/http'

interface LicenseInfo {
  status: string
  writable: boolean
  message: string
  customer: string | null
  edition: string | null
  licenseId: string | null
  issuedAt: string | null
  expiresAt: string | null
  graceDays: number | null
  daysToExpiry: number
  maxAccounts: number | null
  maxStudents: number | null
  maxCampuses: number | null
  features: string[]
  machineFingerprint: string
  boundFingerprint: string | null
}

const info = ref<LicenseInfo | null>(null)
const loading = ref(false)
const reloading = ref(false)
const activating = ref(false)
const licenseText = ref('')

const FEATURE_LABELS: Record<string, string> = {
  attendance: '考勤',
  classroom: '教室',
  scheduling: '排课',
  consumption: '课消',
  transfer: '转班',
  teacher: '教师',
  marketing: '营销/优惠券',
  contract: '合同',
  notification: '通知',
  report: '报表'
}

const STATUS_TEXT: Record<string, string> = {
  VALID: '有效',
  GRACE: '宽限期',
  EXPIRED: '已过期（只读）',
  INVALID: '无效（只读）',
  UNLICENSED: '未激活（只读）'
}

const statusText = computed(() => STATUS_TEXT[info.value?.status ?? ''] ?? info.value?.status ?? '—')
const statusColor = computed(() => {
  switch (info.value?.status) {
    case 'VALID': return 'green'
    case 'GRACE': return 'orange'
    case 'EXPIRED':
    case 'INVALID': return 'red'
    default: return 'default'
  }
})
const alertType = computed<'success' | 'warning' | 'error'>(() => {
  switch (info.value?.status) {
    case 'VALID': return 'success'
    case 'GRACE': return 'warning'
    default: return 'error'
  }
})
const expiresText = computed(() => info.value?.expiresAt ?? '永久')
const daysText = computed(() => {
  const d = info.value?.daysToExpiry
  if (d === undefined || d === null || d > 36500) return '永久'
  return d >= 0 ? `${d} 天` : `已过期 ${-d} 天`
})

function capText(cap: number | null | undefined) {
  return cap === null || cap === undefined || cap < 0 ? '不限' : String(cap)
}
function featureLabel(code: string) {
  return FEATURE_LABELS[code] ?? code
}

async function loadInfo() {
  loading.value = true
  try {
    info.value = await apiGet<LicenseInfo>('/system/license')
  } catch (e) {
    message.error((e as Error).message || '加载授权信息失败')
  } finally {
    loading.value = false
  }
}

async function reloadFromSource() {
  reloading.value = true
  try {
    info.value = await apiPost<LicenseInfo>('/system/license/reload')
    message.success('已重新加载授权')
  } catch (e) {
    message.error((e as Error).message || '重新加载失败')
  } finally {
    reloading.value = false
  }
}

async function activate() {
  activating.value = true
  try {
    info.value = await apiPost<LicenseInfo>('/system/license', { licenseText: licenseText.value.trim() })
    licenseText.value = ''
    message.success('授权已激活')
  } catch (e) {
    message.error((e as Error).message || '激活失败，请检查授权文件')
  } finally {
    activating.value = false
  }
}

onMounted(loadInfo)
</script>

<style scoped>
.license-page {
  display: grid;
  gap: 18px;
}

.license-card {
  border-radius: 14px;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.06);
}

.card-title {
  font-weight: 700;
  font-size: 16px;
}

.status-alert {
  margin-bottom: 18px;
  border-radius: 10px;
}

.desc :deep(.ant-descriptions-item-label) {
  width: 140px;
}

.bound-hint {
  margin-left: 10px;
  color: var(--edu-text-muted);
  font-size: 12px;
}

.hint {
  color: var(--edu-text-muted);
  margin-bottom: 12px;
}

.activate-actions {
  margin-top: 14px;
}
</style>
