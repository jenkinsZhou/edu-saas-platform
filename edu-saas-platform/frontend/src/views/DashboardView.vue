<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="运营总览" sub-title="核心指标、数据趋势分析">
        <template #extra>
          <a-space>
            <a-range-picker v-model:value="dateRange" @change="loadData" />
            <a-button @click="loadData">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="6">
        <a-card :bordered="false" :loading="loading">
          <a-statistic
            title="在读学员"
            :value="metrics.studentCount"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix><UserOutlined /></template>
          </a-statistic>
          <div style="margin-top: 8px; font-size: 12px; color: #8c8c8c">
            较上月 <span style="color: #52c41a">+12.3%</span>
          </div>
        </a-card>
      </a-col>

      <a-col :span="6">
        <a-card :bordered="false" :loading="loading">
          <a-statistic
            title="本月课次"
            :value="metrics.lessonCount"
            :value-style="{ color: '#52c41a' }"
          >
            <template #prefix><BookOutlined /></template>
          </a-statistic>
          <div style="margin-top: 8px; font-size: 12px; color: #8c8c8c">
            较上月 <span style="color: #52c41a">+8.5%</span>
          </div>
        </a-card>
      </a-col>

      <a-col :span="6">
        <a-card :bordered="false" :loading="loading">
          <a-statistic
            title="出勤率"
            :value="metrics.attendanceRate"
            suffix="%"
            :value-style="{ color: '#faad14' }"
            :precision="1"
          >
            <template #prefix><CheckCircleOutlined /></template>
          </a-statistic>
          <div style="margin-top: 8px; font-size: 12px; color: #8c8c8c">
            较上月 <span style="color: #52c41a">+2.1%</span>
          </div>
        </a-card>
      </a-col>

      <a-col :span="6">
        <a-card :bordered="false" :loading="loading">
          <a-statistic
            title="待处理审批"
            :value="metrics.pendingApprovals"
            :value-style="{ color: '#ff4d4f' }"
          >
            <template #prefix><BellOutlined /></template>
          </a-statistic>
          <div style="margin-top: 8px; font-size: 12px; color: #8c8c8c">
            需要及时处理
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="收入趋势" :bordered="false">
          <div id="revenueChart" style="height: 300px"></div>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="学员增长" :bordered="false">
          <div id="studentChart" style="height: 300px"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近订单" :bordered="false">
          <template #extra>
            <a-button type="link" @click="$router.push('/orders')">查看全部</a-button>
          </template>

          <a-list :data-source="recentOrders" :loading="loading">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <span>{{ item.orderNo }}</span>
                      <a-tag :color="getOrderStatusColor(item.orderStatus)">
                        {{ item.orderStatus }}
                      </a-tag>
                    </a-space>
                  </template>
                  <template #description>
                    {{ item.studentName }} · {{ item.courseProductName }}
                  </template>
                </a-list-item-meta>
                <div style="color: #ff4d4f; font-weight: 500">¥{{ item.totalAmount }}</div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="待办事项" :bordered="false">
          <a-list :data-source="todoList">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <a-badge :status="item.status" />
                      <span>{{ item.title }}</span>
                    </a-space>
                  </template>
                  <template #description>{{ item.time }}</template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  BookOutlined,
  CheckCircleOutlined,
  BellOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { apiGet } from '../api/http'

const loading = ref(false)
const dateRange = ref()

const metrics = ref({
  studentCount: 1286,
  lessonCount: 842,
  attendanceRate: 93.6,
  pendingApprovals: 18
})

const recentOrders = ref([
  { orderNo: 'ORD202406110001', studentName: '张三', courseProductName: '数学提高班', totalAmount: 1200, orderStatus: '已确认' },
  { orderNo: 'ORD202406110002', studentName: '李四', courseProductName: '英语基础班', totalAmount: 980, orderStatus: '已创建' },
  { orderNo: 'ORD202406110003', studentName: '王五', courseProductName: '物理竞赛班', totalAmount: 2400, orderStatus: '已确认' }
])

const todoList = ref([
  { title: '审批转班申请', time: '2小时前', status: 'error' },
  { title: '处理退费申请', time: '5小时前', status: 'warning' },
  { title: '确认排课安排', time: '1天前', status: 'processing' }
])

onMounted(() => {
  loadData()
  initCharts()
})

async function loadData() {
  loading.value = true
  try {
    const res = await apiGet<any>('/reports/dashboard')
    if (res) {
      metrics.value = {
        studentCount: res.activeStudents ?? 0,
        lessonCount: res.monthLessonCount ?? 0,
        attendanceRate: Number(res.monthAttendanceRate ?? 0),
        pendingApprovals: res.pendingApprovals ?? 0
      }
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function initCharts() {
  const revenueChart = echarts.init(document.getElementById('revenueChart')!)
  const studentChart = echarts.init(document.getElementById('studentChart')!)

  revenueChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [{
      name: '收入',
      type: 'line',
      data: [12000, 15000, 18000, 22000, 25000, 28000],
      smooth: true,
      itemStyle: { color: '#1890ff' },
      areaStyle: { color: 'rgba(24, 144, 255, 0.1)' }
    }]
  })

  studentChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [{
      name: '学员数',
      type: 'bar',
      data: [980, 1050, 1120, 1180, 1230, 1286],
      itemStyle: { color: '#52c41a' }
    }]
  })
}

function getOrderStatusColor(status: string) {
  const colors: Record<string, string> = {
    '已创建': 'blue',
    '已确认': 'green',
    '已取消': 'red'
  }
  return colors[status] || 'default'
}
</script>

<style scoped>
.page-container {
  padding: 0;
}
</style>
