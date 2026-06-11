<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="机构主题" sub-title="自定义品牌颜色和视觉风格" />
    </a-card>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="主题配置" :bordered="false">
          <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-form-item label="主色">
              <a-space>
                <input type="color" v-model="theme.primaryColor" @change="preview" style="width: 80px; height: 40px; cursor: pointer" />
                <a-input v-model:value="theme.primaryColor" style="width: 120px" />
                <a-tag color="blue">品牌主色</a-tag>
              </a-space>
            </a-form-item>

            <a-form-item label="成功色">
              <a-space>
                <input type="color" v-model="theme.accentColor" @change="preview" style="width: 80px; height: 40px; cursor: pointer" />
                <a-input v-model:value="theme.accentColor" style="width: 120px" />
                <a-tag color="green">操作成功</a-tag>
              </a-space>
            </a-form-item>

            <a-form-item label="侧边栏背景">
              <a-space>
                <input type="color" v-model="theme.sidebarColor" @change="preview" style="width: 80px; height: 40px; cursor: pointer" />
                <a-input v-model:value="theme.sidebarColor" style="width: 120px" />
                <a-tag>侧边栏</a-tag>
              </a-space>
            </a-form-item>

            <a-form-item label="侧边栏文字">
              <a-space>
                <input type="color" v-model="theme.sidebarTextColor" @change="preview" style="width: 80px; height: 40px; cursor: pointer" />
                <a-input v-model:value="theme.sidebarTextColor" style="width: 120px" />
                <a-tag>文字颜色</a-tag>
              </a-space>
            </a-form-item>

            <a-form-item label="预设方案">
              <a-space>
                <a-button @click="applyPreset('default')">默认蓝色</a-button>
                <a-button @click="applyPreset('green')">清新绿色</a-button>
                <a-button @click="applyPreset('purple')">优雅紫色</a-button>
                <a-button @click="applyPreset('red')">活力红色</a-button>
              </a-space>
            </a-form-item>

            <a-divider />

            <a-form-item :wrapper-col="{ offset: 6, span: 18 }">
              <a-space>
                <a-button type="primary" :loading="saving" @click="saveTheme">
                  保存主题
                </a-button>
                <a-button @click="preview">预览效果</a-button>
                <a-button @click="resetTheme">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="效果预览" :bordered="false">
          <a-space direction="vertical" :size="16" style="width: 100%">
            <a-alert message="预览提示" description="修改颜色后点击「预览效果」按钮查看实际效果" type="info" show-icon />

            <div class="preview-section">
              <h4>按钮预览</h4>
              <a-space>
                <a-button type="primary">主要按钮</a-button>
                <a-button>默认按钮</a-button>
                <a-button type="dashed">虚线按钮</a-button>
                <a-button type="link">链接按钮</a-button>
              </a-space>
            </div>

            <div class="preview-section">
              <h4>标签预览</h4>
              <a-space>
                <a-tag color="blue">进行中</a-tag>
                <a-tag color="green">已完成</a-tag>
                <a-tag color="orange">待处理</a-tag>
                <a-tag color="red">已取消</a-tag>
              </a-space>
            </div>

            <div class="preview-section">
              <h4>侧边栏预览</h4>
              <div class="sidebar-preview" :style="{ backgroundColor: theme.sidebarColor, color: theme.sidebarTextColor }">
                <div class="preview-menu-item">
                  <DashboardOutlined />
                  <span>运营总览</span>
                </div>
                <div class="preview-menu-item">
                  <BookOutlined />
                  <span>课程中心</span>
                </div>
                <div class="preview-menu-item">
                  <ShoppingOutlined />
                  <span>订单中心</span>
                </div>
              </div>
            </div>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DashboardOutlined, BookOutlined, ShoppingOutlined } from '@ant-design/icons-vue'
import { applyTenantTheme } from '../theme/applyTenantTheme'
import { apiPost } from '../api/http'

const saving = ref(false)

const theme = reactive({
  primaryColor: '#1890ff',
  accentColor: '#52c41a',
  surfaceColor: '#ffffff',
  sidebarColor: '#001529',
  sidebarTextColor: '#ffffff'
})

const presets: Record<string, any> = {
  default: {
    primaryColor: '#1890ff',
    accentColor: '#52c41a',
    sidebarColor: '#001529',
    sidebarTextColor: '#ffffff'
  },
  green: {
    primaryColor: '#52c41a',
    accentColor: '#1890ff',
    sidebarColor: '#002329',
    sidebarTextColor: '#ffffff'
  },
  purple: {
    primaryColor: '#722ed1',
    accentColor: '#eb2f96',
    sidebarColor: '#1a0033',
    sidebarTextColor: '#ffffff'
  },
  red: {
    primaryColor: '#f5222d',
    accentColor: '#fa8c16',
    sidebarColor: '#330000',
    sidebarTextColor: '#ffffff'
  }
}

function preview() {
  applyTenantTheme(theme)
  message.info('预览已应用，刷新页面可恢复')
}

function applyPreset(name: string) {
  const preset = presets[name]
  if (preset) {
    Object.assign(theme, preset)
    preview()
  }
}

async function saveTheme() {
  saving.value = true
  try {
    await apiPost('/tenant/theme', theme)
    message.success('保存成功')
    applyTenantTheme(theme)
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function resetTheme() {
  Object.assign(theme, presets.default)
  preview()
}
</script>

<style scoped>
.page-container {
  padding: 0;
}

.preview-section {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.preview-section h4 {
  margin: 0 0 12px 0;
  font-weight: 500;
}

.sidebar-preview {
  padding: 16px;
  border-radius: 4px;
}

.preview-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.preview-menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
