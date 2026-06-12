<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="订单中心" sub-title="报名订单、支付记录管理">
        <template #extra>
          <a-space>
            <a-button @click="loadOrders">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button type="primary" @click="openCreateOrder">
              <template #icon><PlusOutlined /></template>
              新增订单
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <a-card :bordered="false" style="margin-top: 16px">
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col :span="6">
          <a-statistic title="订单总数" :value="orderSummary.orderCount" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已确认" :value="orderSummary.confirmedCount" :value-style="{ color: '#52c41a' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="未付款" :value="orderSummary.unpaidCount" :value-style="{ color: '#faad14' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="订单金额" :value="orderSummary.totalAmount" prefix="¥" :precision="2" :value-style="{ color: '#ff4d4f' }" />
        </a-col>
      </a-row>

      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="关键词">
          <a-input v-model:value="filters.keyword" placeholder="订单号/学员/联系人" style="width: 200px" allowClear />
        </a-form-item>
        <a-form-item label="订单状态">
          <a-select v-model:value="filters.orderStatus" placeholder="全部" style="width: 120px" allowClear>
            <a-select-option value="CREATED">已创建</a-select-option>
            <a-select-option value="CONFIRMED">已确认</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="付款状态">
          <a-select v-model:value="filters.payStatus" placeholder="全部" style="width: 120px" allowClear>
            <a-select-option value="UNPAID">未付款</a-select-option>
            <a-select-option value="PART_PAID">部分付款</a-select-option>
            <a-select-option value="PAID">已付款</a-select-option>
            <a-select-option value="PART_REFUNDED">部分退款</a-select-option>
            <a-select-option value="REFUNDED">已退款</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="applyFilters">查询</a-button>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="orders"
        :loading="loading"
        :pagination="{
          current: pager.page,
          pageSize: pager.pageSize,
          total: pager.total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`
        }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'studentInfo'">
            <div>
              <div style="font-weight: 500">{{ record.studentName }}</div>
              <div style="font-size: 12px; color: #8c8c8c">{{ record.contactPhone }}</div>
            </div>
          </template>
          <template v-if="column.key === 'amount'">
            <div>
              <div>总额: <span style="color: #ff4d4f; font-weight: 500">¥{{ record.totalAmount }}</span></div>
              <div style="font-size: 12px; color: #8c8c8c">已付: ¥{{ record.paidAmount }}</div>
            </div>
          </template>
          <template v-if="column.key === 'orderStatus'">
            <a-tag :color="getOrderStatusColor(record.orderStatus)">
              {{ getOrderStatusText(record.orderStatus) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'payStatus'">
            <a-tag :color="getPayStatusColor(record.payStatus)">
              {{ getPayStatusText(record.payStatus) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small">详情</a-button>
              <a-button type="link" size="small" v-if="record.orderStatus === 'CREATED'">确认</a-button>
              <a-button type="link" size="small" v-if="record.payStatus === 'UNPAID'">收款</a-button>
              <a-button type="link" danger size="small" v-if="record.orderStatus === 'CREATED'">取消</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { apiGet } from '../api/http'

const loading = ref(false)
const orders = ref<any[]>([])
const pager = ref({ page: 1, pageSize: 20, total: 0 })

const filters = reactive({
  keyword: '',
  orderStatus: undefined,
  payStatus: undefined
})

const orderSummary = reactive({
  orderCount: 0,
  confirmedCount: 0,
  unpaidCount: 0,
  totalAmount: 0
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '学员信息', key: 'studentInfo', width: 150 },
  { title: '课程', dataIndex: 'courseProductName', key: 'courseProductName' },
  { title: '金额', key: 'amount', width: 150 },
  { title: '订单状态', key: 'orderStatus', width: 100 },
  { title: '付款状态', key: 'payStatus', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

onMounted(() => {
  loadOrders()
  loadSummary()
})

async function loadOrders() {
  loading.value = true
  try {
    const res = await apiGet<any>('/orders', {
      page: pager.value.page,
      pageSize: pager.value.pageSize,
      ...filters
    })
    orders.value = res.items || []
    pager.value.total = res.total || 0
  } catch (error) {
    message.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    const res = await apiGet<any>('/orders/summary')
    Object.assign(orderSummary, res)
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

function handleTableChange(pagination: any) {
  pager.value.page = pagination.current
  pager.value.pageSize = pagination.pageSize
  loadOrders()
}

function applyFilters() {
  pager.value.page = 1
  loadOrders()
}

function resetFilters() {
  filters.keyword = ''
  filters.orderStatus = undefined
  filters.payStatus = undefined
  pager.value.page = 1
  loadOrders()
}

function openCreateOrder() {
  message.info('新增订单功能开发中')
}

function getOrderStatusColor(status: string) {
  const colors: Record<string, string> = {
    CREATED: 'blue',
    CONFIRMED: 'green',
    CANCELLED: 'red'
  }
  return colors[status] || 'default'
}

function getOrderStatusText(status: string) {
  const texts: Record<string, string> = {
    CREATED: '已创建',
    CONFIRMED: '已确认',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

function getPayStatusColor(status: string) {
  const colors: Record<string, string> = {
    UNPAID: 'orange',
    PART_PAID: 'blue',
    PAID: 'green',
    PART_REFUNDED: 'purple',
    REFUNDED: 'default'
  }
  return colors[status] || 'default'
}

function getPayStatusText(status: string) {
  const texts: Record<string, string> = {
    UNPAID: '未付款',
    PART_PAID: '部分付款',
    PAID: '已付款',
    PART_REFUNDED: '部分退款',
    REFUNDED: '已退款'
  }
  return texts[status] || status
}
</script>

<style scoped>
.page-container {
  padding: 0;
}
</style>
