<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="机构主题" sub-title="自定义品牌图标、配色与视觉风格，实时预览后保存即全局生效" />
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="14">
        <a-card title="主题配置" :bordered="false">
          <!-- 品牌图标 -->
          <div class="config-block">
            <div class="block-head">
              <span class="block-title">品牌图标</span>
              <span class="block-desc">显示在侧边栏 Logo 处</span>
            </div>
            <div class="icon-grid">
              <button
                v-for="icon in brandIcons"
                :key="icon.key"
                type="button"
                class="icon-tile"
                :class="{ active: theme.brandIcon === icon.key }"
                :title="icon.label"
                @click="theme.brandIcon = icon.key"
              >
                <component :is="icon.component" />
                <span>{{ icon.label }}</span>
              </button>
            </div>
          </div>

          <a-divider />

          <!-- 颜色 -->
          <div class="config-block">
            <div class="block-head">
              <span class="block-title">配色</span>
              <span class="block-desc">点击色块或输入色值，右侧实时预览</span>
            </div>
            <div class="color-row" v-for="field in colorFields" :key="field.key">
              <label class="swatch" :style="{ background: (theme as any)[field.key] }">
                <input type="color" v-model="(theme as any)[field.key]" />
              </label>
              <div class="color-meta">
                <strong>{{ field.label }}</strong>
                <span>{{ field.desc }}</span>
              </div>
              <a-input v-model:value="(theme as any)[field.key]" class="hex-input" spellcheck="false" />
            </div>
          </div>

          <a-divider />

          <!-- 预设方案 -->
          <div class="config-block">
            <div class="block-head">
              <span class="block-title">快捷预设</span>
              <span class="block-desc">一键套用整套配色</span>
            </div>
            <div class="preset-grid">
              <button
                v-for="(preset, name) in presets"
                :key="name"
                type="button"
                class="preset-card"
                :class="{ active: isPresetActive(preset) }"
                @click="applyPreset(preset)"
              >
                <span class="preset-swatches">
                  <i :style="{ background: preset.primaryColor }"></i>
                  <i :style="{ background: preset.accentColor }"></i>
                  <i :style="{ background: preset.sidebarColor }"></i>
                </span>
                <span class="preset-name">{{ preset.label }}</span>
              </button>
            </div>
          </div>

          <a-divider />

          <a-space>
            <a-button type="primary" :loading="saving" @click="saveTheme">
              <template #icon><SaveOutlined /></template>
              保存并应用
            </a-button>
            <a-button @click="previewGlobal">
              <template #icon><EyeOutlined /></template>
              全局预览
            </a-button>
            <a-button @click="resetTheme">
              <template #icon><UndoOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-card>
      </a-col>

      <!-- 实时预览 -->
      <a-col :xs="24" :lg="10">
        <a-card title="实时预览" :bordered="false" class="preview-card">
          <div class="brand-bar">
            <span class="brand-tile" :style="brandTileStyle">
              <component :is="previewIcon" />
            </span>
            <div>
              <strong>EduSphere</strong>
              <span>校区运营中台</span>
            </div>
          </div>

          <div class="kpi-mini" :style="{ '--c': theme.primaryColor }">
            <span class="kpi-label">在读学员</span>
            <span class="kpi-value">1,286</span>
          </div>

          <div class="preview-group">
            <span class="group-label">按钮</span>
            <div class="btn-row">
              <span class="mock-btn primary" :style="{ background: theme.primaryColor }">主要按钮</span>
              <span class="mock-btn ghost" :style="{ color: theme.primaryColor, borderColor: theme.primaryColor }">次要按钮</span>
            </div>
          </div>

          <div class="preview-group">
            <span class="group-label">标签</span>
            <div class="tag-row">
              <span class="mock-tag" :style="tagStyle(theme.primaryColor)">进行中</span>
              <span class="mock-tag" :style="tagStyle(theme.accentColor)">已完成</span>
              <span class="mock-tag" :style="tagStyle('#d97706')">待处理</span>
              <span class="mock-tag" :style="tagStyle('#dc2626')">已取消</span>
            </div>
          </div>

          <div class="preview-group">
            <span class="group-label">侧边栏</span>
            <div class="sidebar-preview" :style="{ background: theme.sidebarColor, color: theme.sidebarTextColor }">
              <div class="sp-brand">
                <span class="sp-tile" :style="brandTileStyle"><component :is="previewIcon" /></span>
                <strong>EduSphere</strong>
              </div>
              <div class="sp-item active" :style="{ background: theme.primaryColor, color: '#fff' }">
                <DashboardOutlined /><span>运营总览</span>
              </div>
              <div class="sp-item"><BookOutlined /><span>课程中心</span></div>
              <div class="sp-item"><ShoppingOutlined /><span>订单中心</span></div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  DashboardOutlined,
  BookOutlined,
  ShoppingOutlined,
  SaveOutlined,
  EyeOutlined,
  UndoOutlined
} from '@ant-design/icons-vue'
import { applyTenantTheme } from '../theme/applyTenantTheme'
import { BRAND_ICONS, resolveBrandIcon, DEFAULT_BRAND_ICON } from '../theme/brandIcons'
import { apiGet, apiPost } from '../api/http'

const saving = ref(false)
const brandIcons = BRAND_ICONS

const theme = reactive({
  primaryColor: '#1e40af',
  accentColor: '#16a34a',
  surfaceColor: '#ffffff',
  sidebarColor: '#0f1b3d',
  sidebarTextColor: '#dbeafe',
  brandIcon: DEFAULT_BRAND_ICON
})

const colorFields = [
  { key: 'primaryColor', label: '品牌主色', desc: '按钮、链接与重点强调' },
  { key: 'accentColor', label: '强调 / 成功色', desc: '成功状态与点缀色' },
  { key: 'sidebarColor', label: '侧边栏背景', desc: '左侧导航底色' },
  { key: 'sidebarTextColor', label: '侧边栏文字', desc: '导航文字颜色' }
]

interface Preset {
  label: string
  primaryColor: string
  accentColor: string
  sidebarColor: string
  sidebarTextColor: string
}

const presets: Record<string, Preset> = {
  default: { label: '默认蓝', primaryColor: '#1e40af', accentColor: '#16a34a', sidebarColor: '#0f1b3d', sidebarTextColor: '#dbeafe' },
  green: { label: '清新绿', primaryColor: '#16a34a', accentColor: '#1e40af', sidebarColor: '#052e2b', sidebarTextColor: '#d1fae5' },
  purple: { label: '优雅紫', primaryColor: '#7c3aed', accentColor: '#db2777', sidebarColor: '#1e1b4b', sidebarTextColor: '#ede9fe' },
  red: { label: '活力红', primaryColor: '#dc2626', accentColor: '#d97706', sidebarColor: '#3b0a0a', sidebarTextColor: '#fee2e2' }
}

const previewIcon = computed(() => resolveBrandIcon(theme.brandIcon))

const brandTileStyle = computed(() => ({
  background: `linear-gradient(135deg, ${theme.primaryColor}, ${theme.accentColor})`
}))

function tagStyle(color: string) {
  return { color, background: hexToSoft(color) }
}

function hexToSoft(hex: string) {
  // 半透明底色，兼容任意主题色
  return `color-mix(in srgb, ${hex} 12%, white)`
}

function isPresetActive(preset: Preset) {
  return (
    preset.primaryColor.toLowerCase() === theme.primaryColor.toLowerCase() &&
    preset.sidebarColor.toLowerCase() === theme.sidebarColor.toLowerCase()
  )
}

function applyPreset(preset: Preset) {
  theme.primaryColor = preset.primaryColor
  theme.accentColor = preset.accentColor
  theme.sidebarColor = preset.sidebarColor
  theme.sidebarTextColor = preset.sidebarTextColor
}

function previewGlobal() {
  applyTenantTheme({ ...theme })
  message.info('已应用到当前界面，刷新可恢复；点击「保存并应用」永久生效')
}

async function saveTheme() {
  saving.value = true
  try {
    await apiPost('/tenant/theme', { ...theme })
    applyTenantTheme({ ...theme })
    message.success('主题已保存并全局生效')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function resetTheme() {
  applyPreset(presets.default)
  theme.brandIcon = DEFAULT_BRAND_ICON
}

onMounted(async () => {
  try {
    const data = await apiGet<Partial<typeof theme>>('/tenant/theme')
    Object.assign(theme, {
      primaryColor: data.primaryColor || theme.primaryColor,
      accentColor: data.accentColor || theme.accentColor,
      sidebarColor: data.sidebarColor || theme.sidebarColor,
      sidebarTextColor: data.sidebarTextColor || theme.sidebarTextColor,
      brandIcon: data.brandIcon || theme.brandIcon
    })
  } catch {
    // 读取失败时保持默认值
  }
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.config-block {
  margin-bottom: 4px;
}

.block-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
}

.block-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--edu-text);
}

.block-desc {
  font-size: 12px;
  color: var(--edu-text-muted);
}

/* 图标选择 */
.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(82px, 1fr));
  gap: 10px;
}

.icon-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 6px;
  border: 1px solid var(--edu-border);
  border-radius: 10px;
  background: var(--edu-surface);
  color: var(--edu-text-secondary);
  font-size: 18px;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease, transform 0.06s ease;
}

.icon-tile span {
  font-size: 12px;
}

.icon-tile:hover {
  border-color: var(--edu-secondary);
  color: var(--edu-primary);
}

.icon-tile:active {
  transform: translateY(1px);
}

.icon-tile.active {
  border-color: var(--edu-primary);
  color: var(--edu-primary);
  background: var(--edu-primary-soft);
  box-shadow: 0 0 0 1px var(--edu-primary) inset;
}

/* 颜色行 */
.color-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
}

.color-row + .color-row {
  border-top: 1px solid var(--edu-border);
}

.swatch {
  position: relative;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  border-radius: 10px;
  border: 1px solid rgba(15, 28, 52, 0.12);
  box-shadow: var(--edu-shadow-xs);
  cursor: pointer;
  overflow: hidden;
}

.swatch input[type='color'] {
  position: absolute;
  inset: -4px;
  width: calc(100% + 8px);
  height: calc(100% + 8px);
  border: none;
  padding: 0;
  background: none;
  cursor: pointer;
  opacity: 0;
}

.color-meta {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-width: 0;
}

.color-meta strong {
  font-size: 14px;
  color: var(--edu-text);
}

.color-meta span {
  font-size: 12px;
  color: var(--edu-text-muted);
}

.hex-input {
  width: 122px;
  flex: 0 0 auto;
  font-family: var(--edu-font-mono);
}

/* 预设 */
.preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.preset-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--edu-border);
  border-radius: 10px;
  background: var(--edu-surface);
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.preset-card:hover {
  border-color: var(--edu-secondary);
}

.preset-card.active {
  border-color: var(--edu-primary);
  box-shadow: 0 0 0 1px var(--edu-primary) inset;
}

.preset-swatches {
  display: inline-flex;
}

.preset-swatches i {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(15, 28, 52, 0.08);
}

.preset-swatches i + i {
  margin-left: -6px;
}

.preset-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--edu-text);
}

/* 预览面板 */
.preview-card :deep(.ant-card-body) {
  display: grid;
  gap: 16px;
}

.brand-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  background: var(--edu-surface-soft);
  border: 1px solid var(--edu-border);
}

.brand-tile {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  color: #fff;
  font-size: 21px;
  box-shadow: var(--edu-shadow-sm);
}

.brand-bar strong {
  display: block;
  font-size: 16px;
  color: var(--edu-text);
}

.brand-bar span {
  font-size: 12px;
  color: var(--edu-text-muted);
}

.kpi-mini {
  position: relative;
  padding: 14px 16px 14px 20px;
  border: 1px solid var(--edu-border);
  border-radius: 12px;
  background: var(--edu-surface);
  overflow: hidden;
}

.kpi-mini::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--c, var(--edu-primary));
}

.kpi-label {
  display: block;
  font-size: 13px;
  color: var(--edu-text-muted);
}

.kpi-value {
  font-size: 26px;
  font-weight: 700;
  font-family: var(--edu-font-mono);
  color: var(--edu-text);
}

.preview-group {
  display: grid;
  gap: 8px;
}

.group-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--edu-text-muted);
}

.btn-row,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mock-btn {
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 550;
  cursor: default;
}

.mock-btn.primary {
  color: #fff;
}

.mock-btn.ghost {
  background: #fff;
  border: 1px solid;
}

.mock-tag {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 550;
}

.sidebar-preview {
  padding: 12px;
  border-radius: 12px;
  display: grid;
  gap: 6px;
}

.sp-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 10px;
  margin-bottom: 2px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.sp-tile {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  font-size: 16px;
}

.sp-brand strong {
  font-size: 15px;
}

.sp-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 13px;
  opacity: 0.85;
}

.sp-item.active {
  opacity: 1;
  font-weight: 600;
}
</style>
