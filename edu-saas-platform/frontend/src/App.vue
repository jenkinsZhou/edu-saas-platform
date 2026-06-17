<template>
  <a-config-provider :theme="themeConfig">
    <RouterView />
  </a-config-provider>
</template>

<script setup lang="ts">
const fontFamily =
  '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif'

const themeConfig = {
  token: {
    colorPrimary: '#1e40af',
    colorInfo: '#2563eb',
    colorSuccess: '#16a34a',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorTextBase: '#0f1c34',
    colorBgLayout: '#f4f6fb',
    colorBorder: '#d4ddec',
    colorBorderSecondary: '#e6ebf3',
    borderRadius: 8,
    borderRadiusLG: 12,
    borderRadiusSM: 6,
    fontFamily,
    wireframe: false
  },
  components: {
    Button: {
      controlHeight: 36,
      fontWeight: 550,
      primaryShadow: '0 1px 2px rgba(30, 64, 175, 0.25)'
    },
    Card: {
      borderRadiusLG: 12,
      headerFontSize: 15
    },
    Table: {
      headerBg: '#f7f9fc',
      headerColor: '#475467',
      headerSplitColor: 'transparent',
      rowHoverBg: '#f2f6fd',
      borderColor: '#e6ebf3'
    },
    Menu: {
      itemBorderRadius: 8,
      itemHeight: 42
    },
    Input: {
      controlHeight: 36
    },
    Select: {
      controlHeight: 36
    }
  }
}
</script>
