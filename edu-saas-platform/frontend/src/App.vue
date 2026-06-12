<template>
  <a-config-provider :theme="themeConfig">
    <RouterView />
  </a-config-provider>
</template>

<script setup lang="ts">
const themeConfig = {
  token: {
    colorPrimary: '#1e40af',
    colorInfo: '#2563eb',
    colorSuccess: '#16a34a',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorTextBase: '#0f172a',
    colorBgLayout: '#f5f7fb',
    colorBorder: '#dbeafe',
    borderRadius: 8,
    wireframe: false
  },
  components: {
    Button: {
      controlHeight: 36,
      borderRadius: 8
    },
    Card: {
      borderRadiusLG: 8
    },
    Table: {
      headerBg: '#f8fafc',
      headerColor: '#334155',
      rowHoverBg: '#f8fbff'
    }
  }
}
</script>
