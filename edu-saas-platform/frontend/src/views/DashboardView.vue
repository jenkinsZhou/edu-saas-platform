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
          />
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
                        {{ getOrderStatusLabel(item.orderStatus) }}
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
  studentCount: 0,
  newStudents: 0,
  lessonCount: 0,
  lastMonthLessonCount: 0,
  attendanceRate: 0,
  lastMonthAttendanceRate: 0,
  pendingApprovals: 0,
  unpaidOrders: 0,
  expiringContracts: 0
})

function deltaTrend(current: number, previous: number, suffix = '') {
  if (previous <= 0) {
    return { trend: current > 0 ? '新增' : '暂无数据', trendClass: current > 0 ? 'trend-up' : 'trend-warning' }
  }
  const delta = ((current - previous) / previous) * 100
  const sign = delta >= 0 ? '+' : ''
  return {
    trend: `${sign}${delta.toFixed(1)}%${suffix}`,
    trendClass: delta >= 0 ? 'trend-up' : 'trend-warning'
  }
}

const metricCards = computed(() => {
  const lessonTrend = deltaTrend(metrics.value.lessonCount, metrics.value.lastMonthLessonCount)
  const attendanceTrend = deltaTrend(metrics.value.attendanceRate, metrics.value.lastMonthAttendanceRate)
  return [
    {
      key: 'students',
      title: '在读学员',
      value: metrics.value.studentCount,
      icon: UserOutlined,
      color: '#1e40af',
      trend: `本月新增 ${metrics.value.newStudents}`,
      trendClass: metrics.value.newStudents > 0 ? 'trend-up' : 'trend-warning',
      hint: ' 名学员'
    },
    {
      key: 'lessons',
      title: '本月课次',
      value: metrics.value.lessonCount,
      icon: BookOutlined,
      color: '#16a34a',
      trend: lessonTrend.trend,
      trendClass: lessonTrend.trendClass,
      hint: ' 较上月'
    },
    {
      key: 'attendance',
      title: '出勤率',
      value: metrics.value.attendanceRate,
      suffix: '%',
      precision: 1,
      icon: CheckCircleOutlined,
      color: '#d97706',
      trend: attendanceTrend.trend,
      trendClass: attendanceTrend.trendClass,
      hint: ' 较上月'
    },
    {
      key: 'approvals',
      title: '待处理审批',
      value: metrics.value.pendingApprovals,
      icon: BellOutlined,
      color: '#dc2626',
      trend: metrics.value.pendingApprovals > 0 ? '需处理' : '已清零',
      trendClass: metrics.value.pendingApprovals > 0 ? 'trend-warning' : 'trend-up',
      hint: ' 转班审批'
    }
  ]
})

const recentOrders = ref<any[]>([])

const todoList = computed(() => {
  const items = [
    { title: `待审批转班申请 ${metrics.value.pendingApprovals} 件`, time: '来自转班流程', status: 'error', show: metrics.value.pendingApprovals > 0 },
    { title: `未付款订单 ${metrics.value.unpaidOrders} 笔`, time: '建议跟进收款', status: 'warning', show: metrics.value.unpaidOrders > 0 },
    { title: `30天内到期合同 ${metrics.value.expiringContracts} 份`, time: '建议提醒续费', status: 'processing', show: metrics.value.expiringContracts > 0 }
  ].filter(item => item.show)
  return items.length > 0 ? items : [{ title: '暂无待办事项', time: '一切正常', status: 'success' }]
})

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
        newStudents: res.newStudentsThisMonth ?? 0,
        lessonCount: res.monthLessonCount ?? 0,
        lastMonthLessonCount: res.lastMonthLessonCount ?? 0,
        attendanceRate: Number(res.monthAttendanceRate ?? 0),
        lastMonthAttendanceRate: Number(res.lastMonthAttendanceRate ?? 0),
        pendingApprovals: res.pendingApprovals ?? 0,
        unpaidOrders: res.unpaidOrders ?? 0,
        expiringContracts: res.expiringContracts ?? 0
      }
      updateCharts(res.revenueTrend ?? [], res.studentTrend ?? [])
    }
    const orderRes = await apiGet<any>('/orders', { page: 1, pageSize: 5 })
    recentOrders.value = orderRes.records || []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载数据失败')
  } finally {
    loading.value = false
  }
}

function updateCharts(revenueTrend: any[], studentTrend: any[]) {
  revenueChart?.setOption({
    xAxis: { data: revenueTrend.map(p => p.month) },
    series: [{ data: revenueTrend.map(p => Number(p.value)) }]
  })
  studentChart?.setOption({
    xAxis: { data: studentTrend.map(p => p.month) },
    series: [{ data: studentTrend.map(p => Number(p.value)) }]
  })
}

function initCharts() {
  if (revenueChartRef.value) {
    revenueChart = echarts.init(revenueChartRef.value)
    revenueChart.setOption({
      color: ['#1e40af'],
      grid: { top: 24, right: 16, bottom: 26, left: 46 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 28, 52, 0.94)',
        borderWidth: 0,
        padding: [8, 12],
        textStyle: { color: '#fff', fontSize: 12 },
        extraCssText: 'border-radius:8px;box-shadow:0 8px 24px rgba(15,28,52,0.25);'
      },
      xAxis: {
        type: 'category',
        data: [],
        boundaryGap: false,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: '#8a96a8', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#8a96a8', fontSize: 12 },
        splitLine: { lineStyle: { color: '#eef1f6', type: 'dashed' } }
      },
      series: [{
        name: '实收金额',
        type: 'line',
        data: [],
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(30, 64, 175, 0.20)' },
            { offset: 1, color: 'rgba(30, 64, 175, 0)' }
          ])
        }
      }]
    })
  }

  if (studentChartRef.value) {
    studentChart = echarts.init(studentChartRef.value)
    studentChart.setOption({
      grid: { top: 24, right: 16, bottom: 26, left: 46 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 28, 52, 0.94)',
        borderWidth: 0,
        padding: [8, 12],
        textStyle: { color: '#fff', fontSize: 12 },
        extraCssText: 'border-radius:8px;box-shadow:0 8px 24px rgba(15,28,52,0.25);'
      },
      xAxis: {
        type: 'category',
        data: [],
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: '#8a96a8', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#8a96a8', fontSize: 12 },
        splitLine: { lineStyle: { color: '#eef1f6', type: 'dashed' } }
      },
      series: [{
        name: '新增学员',
        type: 'bar',
        data: [],
        barWidth: 22,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#22c55e' },
            { offset: 1, color: '#16a34a' }
          ])
        }
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
    CREATED: 'blue',
    CONFIRMED: 'green',
    CANCELLED: 'red',
    COMPLETED: 'cyan'
  }
  return colors[status] || 'default'
}

function getOrderStatusLabel(status: string) {
  const labels: Record<string, string> = {
    CREATED: '已创建',
    CONFIRMED: '已确认',
    CANCELLED: '已取消',
    COMPLETED: '已完成'
  }
  return labels[status] || status
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
  color: var(--edu-text);
  font-family: var(--edu-font-mono);
  font-weight: 650;
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
