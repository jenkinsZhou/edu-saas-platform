<template>
  <div class="page-container dashboard-page">
    <a-card :bordered="false" class="page-header dashboard-hero">
      <a-page-header title="运营总览" sub-title="实时掌握招生、课消、出勤和审批风险">
        <template #extra>
          <a-space wrap>
            <a-range-picker v-model:value="dateRange" @change="loadData" />
            <a-button @click="loadData">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <div class="metric-strip">
      <a-card
        v-for="card in metricCards"
        :key="card.key"
        :bordered="false"
        :loading="loading"
        class="data-card"
        :style="{ '--card-accent': card.color }"
      >
        <div class="data-card-meta">
          <a-statistic
            :title="card.title"
            :value="card.value"
            :suffix="card.suffix"
            :precision="card.precision"
          >
            <template #prefix>
              <component :is="card.icon" />
            </template>
          </a-statistic>
          <span class="data-card-icon">
            <component :is="card.icon" />
          </span>
        </div>
        <div class="data-card-trend">
          <span :class="card.trendClass">{{ card.trend }}</span>
          <span>{{ card.hint }}</span>
        </div>
      </a-card>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="12">
        <a-card title="收入趋势" :bordered="false" class="chart-panel">
          <template #extra>
            <a-tag color="blue">月度</a-tag>
          </template>
          <div ref="revenueChartRef" class="chart-box"></div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="12">
        <a-card title="学员增长" :bordered="false" class="chart-panel">
          <template #extra>
            <a-tag color="green">新增/在读</a-tag>
          </template>
          <div ref="studentChartRef" class="chart-box"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="16">
        <a-card title="最近订单" :bordered="false" class="work-panel">
          <template #extra>
            <a-button type="link" @click="$router.push('/orders')">查看全部</a-button>
          </template>

          <a-list :data-source="recentOrders" :loading="loading" item-layout="horizontal">
            <template #renderItem="{ item }">
              <a-list-item class="order-row">
                <a-list-item-meta>
                  <template #title>
                    <a-space wrap>
                      <span class="order-no">{{ item.orderNo }}</span>
                      <a-tag :color="getOrderStatusColor(item.orderStatus)">
                        {{ item.orderStatus }}
                      </a-tag>
                    </a-space>
                  </template>
                  <template #description>
                    <span>{{ item.studentName }}</span>
                    <span class="dot-separator"></span>
                    <span>{{ item.courseProductName }}</span>
                  </template>
                </a-list-item-meta>
                <div class="amount-text">¥{{ item.totalAmount }}</div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="8">
        <a-card title="待办事项" :bordered="false" class="work-panel">
          <a-list :data-source="todoList">
            <template #renderItem="{ item }">
              <a-list-item class="todo-row">
                <a-list-item-meta>
                  <template #avatar>
                    <a-badge :status="item.status" />
                  </template>
                  <template #title>
                    <span class="todo-title">{{ item.title }}</span>
                  </template>
                  <template #description>
                    <span class="muted-text">{{ item.time }}</span>
                  </template>
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
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
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
const revenueChartRef = ref<HTMLDivElement>()
const studentChartRef = ref<HTMLDivElement>()
let revenueChart: echarts.ECharts | undefined
let studentChart: echarts.ECharts | undefined

const metrics = ref({
  studentCount: 1286,
  lessonCount: 842,
  attendanceRate: 93.6,
  pendingApprovals: 18
})

const metricCards = computed(() => [
  {
    key: 'students',
    title: '在读学员',
    value: metrics.value.studentCount,
    icon: UserOutlined,
    color: '#1e40af',
    trend: '+12.3%',
    trendClass: 'trend-up',
    hint: ' 较上月'
  },
  {
    key: 'lessons',
    title: '本月课次',
    value: metrics.value.lessonCount,
    icon: BookOutlined,
    color: '#16a34a',
    trend: '+8.5%',
    trendClass: 'trend-up',
    hint: ' 排课消耗'
  },
  {
    key: 'attendance',
    title: '出勤率',
    value: metrics.value.attendanceRate,
    suffix: '%',
    precision: 1,
    icon: CheckCircleOutlined,
    color: '#d97706',
    trend: '+2.1%',
    trendClass: 'trend-up',
    hint: ' 稳定提升'
  },
  {
    key: 'approvals',
    title: '待处理审批',
    value: metrics.value.pendingApprovals,
    icon: BellOutlined,
    color: '#dc2626',
    trend: '需处理',
    trendClass: 'trend-warning',
    hint: ' 转班/退费/排课'
  }
])

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

onMounted(async () => {
  await nextTick()
  initCharts()
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  revenueChart?.dispose()
  studentChart?.dispose()
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
  if (revenueChartRef.value) {
    revenueChart = echarts.init(revenueChartRef.value)
    revenueChart.setOption({
      color: ['#1e40af'],
      grid: { top: 26, right: 18, bottom: 28, left: 42 },
      tooltip: { trigger: 'axis', backgroundColor: '#0f172a', borderWidth: 0, textStyle: { color: '#fff' } },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月'],
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#dbe5f4' } }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#edf2f7' } }
      },
      series: [{
        name: '收入',
        type: 'line',
        data: [12000, 15000, 18000, 22000, 25000, 28000],
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(30, 64, 175, 0.22)' },
            { offset: 1, color: 'rgba(30, 64, 175, 0)' }
          ])
        }
      }]
    })
  }

  if (studentChartRef.value) {
    studentChart = echarts.init(studentChartRef.value)
    studentChart.setOption({
      color: ['#16a34a'],
      grid: { top: 26, right: 18, bottom: 28, left: 42 },
      tooltip: { trigger: 'axis', backgroundColor: '#0f172a', borderWidth: 0, textStyle: { color: '#fff' } },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月'],
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#dbe5f4' } }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#edf2f7' } }
      },
      series: [{
        name: '学员数',
        type: 'bar',
        data: [980, 1050, 1120, 1180, 1230, 1286],
        barWidth: 26,
        itemStyle: { borderRadius: [6, 6, 0, 0] }
      }]
    })
  }
}

function resizeCharts() {
  revenueChart?.resize()
  studentChart?.resize()
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
.dashboard-hero :deep(.ant-card-body) {
  padding-bottom: 20px;
}

.work-panel {
  min-height: 310px;
}

.order-row {
  padding: 14px 0;
}

.order-no {
  color: #1e293b;
  font-family: "Fira Code", "SFMono-Regular", Consolas, monospace;
  font-weight: 700;
}

.dot-separator {
  display: inline-flex;
  width: 4px;
  height: 4px;
  margin: 0 8px 2px;
  border-radius: 999px;
  background: #cbd5e1;
}

.todo-row {
  padding: 13px 0;
}

.todo-title {
  color: #1e293b;
  font-weight: 700;
}
</style>
