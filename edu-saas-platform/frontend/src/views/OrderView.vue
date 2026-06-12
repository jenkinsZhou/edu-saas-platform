<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="订单中心" sub-title="报名订单、确认入班与线下收款">
        <template #extra>
          <a-space wrap>
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

    <a-card :bordered="false">
      <div class="metric-strip order-summary">
        <a-card :bordered="false" class="data-card" style="--card-accent: #1e40af">
          <a-statistic title="订单总数" :value="orderSummary.orderCount" />
        </a-card>
        <a-card :bordered="false" class="data-card" style="--card-accent: #16a34a">
          <a-statistic title="已确认" :value="orderSummary.confirmedCount" />
        </a-card>
        <a-card :bordered="false" class="data-card" style="--card-accent: #d97706">
          <a-statistic title="未付款" :value="orderSummary.unpaidCount" />
        </a-card>
        <a-card :bordered="false" class="data-card" style="--card-accent: #dc2626">
          <a-statistic title="订单金额" :value="orderSummary.totalAmount" prefix="¥" :precision="2" />
        </a-card>
      </div>

      <div class="compact-toolbar">
        <a-form layout="inline" class="filter-form">
          <a-form-item label="关键词">
            <a-input v-model:value="filters.keyword" placeholder="订单号/学员/联系人" style="width: 220px" allow-clear />
          </a-form-item>
          <a-form-item label="订单状态">
            <a-select v-model:value="filters.orderStatus" placeholder="全部" style="width: 130px" allow-clear>
              <a-select-option value="CREATED">已创建</a-select-option>
              <a-select-option value="CONFIRMED">已确认</a-select-option>
              <a-select-option value="CANCELLED">已取消</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="付款状态">
            <a-select v-model:value="filters.payStatus" placeholder="全部" style="width: 140px" allow-clear>
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
      </div>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="orders"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'studentInfo'">
            <div class="stacked-cell">
              <strong>{{ record.studentName }}</strong>
              <span>{{ record.customerPhone || '未填写联系方式' }}</span>
            </div>
          </template>
          <template v-if="column.key === 'amount'">
            <div class="stacked-cell">
              <strong class="amount-text">¥{{ record.payableAmount || record.totalAmount }}</strong>
              <span>已付 ¥{{ record.paidAmount }} · 待收 ¥{{ record.outstandingAmount }}</span>
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
              <a-button type="link" size="small" @click="openOrderDetail(record)">详情</a-button>
              <a-button type="link" size="small" v-if="record.orderStatus === 'CREATED'" @click="confirmOrder(record)">确认</a-button>
              <a-button type="link" size="small" v-if="record.payStatus !== 'PAID' && record.orderStatus !== 'CANCELLED'" @click="openPayment(record)">收款</a-button>
              <a-button type="link" danger size="small" v-if="record.orderStatus === 'CREATED'" @click="cancelOrder(record)">取消</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="orderModalOpen"
      title="新增报名订单"
      :confirm-loading="orderSaving"
      @ok="submitOrder"
    >
      <a-form ref="orderFormRef" :model="orderForm" :rules="orderRules" layout="vertical">
        <a-form-item label="学员" name="studentId">
          <a-select v-model:value="orderForm.studentId" placeholder="请选择学员" show-search option-filter-prop="label">
            <a-select-option v-for="student in students" :key="student.id" :value="student.id" :label="student.name">
              {{ student.name }} {{ student.phone ? `(${student.phone})` : '' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="班级" name="classGroupId">
          <a-select v-model:value="orderForm.classGroupId" placeholder="请选择班级" show-search option-filter-prop="label">
            <a-select-option v-for="group in classGroups" :key="group.id" :value="group.id" :label="group.name">
              {{ group.name }} · {{ group.courseProductName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="联系人" name="customerName">
              <a-input v-model:value="orderForm.customerName" placeholder="默认取学员/家长" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="customerPhone">
              <a-input v-model:value="orderForm.customerPhone" placeholder="默认取学员/家长手机号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="来源" name="sourceChannel">
              <a-select v-model:value="orderForm.sourceChannel">
                <a-select-option value="MANUAL">人工录入</a-select-option>
                <a-select-option value="OFFLINE">线下到店</a-select-option>
                <a-select-option value="ONLINE">线上咨询</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="优惠金额" name="discountAmount">
              <a-input-number v-model:value="orderForm.discountAmount" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="orderForm.remark" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="paymentModalOpen"
      :title="`订单收款 - ${activeOrder?.orderNo || ''}`"
      :confirm-loading="paymentSaving"
      @ok="submitPayment"
    >
      <a-form ref="paymentFormRef" :model="paymentForm" :rules="paymentRules" layout="vertical">
        <a-alert
          v-if="activeOrder"
          :message="`待收金额：¥${activeOrder.outstandingAmount}`"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-form-item label="收款金额" name="amount">
          <a-input-number v-model:value="paymentForm.amount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="收款方式" name="paymentMethod">
          <a-select v-model:value="paymentForm.paymentMethod">
            <a-select-option value="CASH">现金</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="BANK_CARD">银行卡</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="收款时间" name="receivedAt">
          <a-date-picker v-model:value="paymentForm.receivedAt" show-time style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="paymentForm.remark" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailDrawerOpen" title="订单详情" width="620">
      <a-skeleton v-if="detailLoading" active />
      <a-descriptions v-else-if="orderDetail" bordered :column="1" size="small">
        <a-descriptions-item label="订单号">{{ orderDetail.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="学员">{{ orderDetail.studentName }}</a-descriptions-item>
        <a-descriptions-item label="课程">{{ orderDetail.courseProductName }}</a-descriptions-item>
        <a-descriptions-item label="班级">{{ orderDetail.classGroupName }}</a-descriptions-item>
        <a-descriptions-item label="金额">
          应收 ¥{{ orderDetail.payableAmount }}，已付 ¥{{ orderDetail.paidAmount }}，待收 ¥{{ orderDetail.outstandingAmount }}
        </a-descriptions-item>
        <a-descriptions-item label="订单状态">{{ getOrderStatusText(orderDetail.orderStatus) }}</a-descriptions-item>
        <a-descriptions-item label="付款状态">{{ getPayStatusText(orderDetail.payStatus) }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{ orderDetail.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { apiGet, apiPost, apiPut } from '../api/http'

interface PageResult<T> {
  records: T[]
  total: number
}

interface OrderRecord {
  id: number
  orderNo: string
  studentName: string
  customerPhone: string
  classGroupName: string
  courseProductName: string
  totalAmount: string
  payableAmount: string
  paidAmount: string
  outstandingAmount: string
  orderStatus: string
  payStatus: string
  createdAt: string
  remark: string
}

interface StudentOption {
  id: number
  name: string
  phone: string
}

interface ClassOption {
  id: number
  name: string
  courseProductName: string
}

const loading = ref(false)
const orderSaving = ref(false)
const paymentSaving = ref(false)
const detailLoading = ref(false)
const orders = ref<OrderRecord[]>([])
const students = ref<StudentOption[]>([])
const classGroups = ref<ClassOption[]>([])
const activeOrder = ref<OrderRecord>()
const orderDetail = ref<any>()

const pager = ref({ page: 1, pageSize: 20, total: 0 })
const orderModalOpen = ref(false)
const paymentModalOpen = ref(false)
const detailDrawerOpen = ref(false)
const orderFormRef = ref()
const paymentFormRef = ref()

const filters = reactive<{ keyword: string; orderStatus?: string; payStatus?: string }>({
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

const orderForm = reactive({
  studentId: undefined as number | undefined,
  classGroupId: undefined as number | undefined,
  customerName: '',
  customerPhone: '',
  sourceChannel: 'MANUAL',
  discountAmount: 0,
  remark: ''
})

const paymentForm = reactive({
  amount: 0,
  paymentMethod: 'CASH',
  receivedAt: null as Dayjs | null,
  remark: ''
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '学员信息', key: 'studentInfo', width: 160 },
  { title: '课程', dataIndex: 'courseProductName', key: 'courseProductName' },
  { title: '班级', dataIndex: 'classGroupName', key: 'classGroupName' },
  { title: '金额', key: 'amount', width: 170 },
  { title: '订单状态', key: 'orderStatus', width: 100 },
  { title: '付款状态', key: 'payStatus', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]

const orderRules = {
  studentId: [{ required: true, message: '请选择学员' }],
  classGroupId: [{ required: true, message: '请选择班级' }]
}

const paymentRules = {
  amount: [{ required: true, message: '请输入收款金额' }],
  paymentMethod: [{ required: true, message: '请选择收款方式' }]
}

const pagination = computed(() => ({
  current: pager.value.page,
  pageSize: pager.value.pageSize,
  total: pager.value.total,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
}))

onMounted(() => {
  loadOrders()
  loadOptions()
})

async function loadOrders() {
  loading.value = true
  try {
    const res = await apiGet<PageResult<OrderRecord> & { summary?: any }>('/orders', {
      page: pager.value.page,
      pageSize: pager.value.pageSize,
      keyword: filters.keyword || undefined,
      orderStatus: filters.orderStatus,
      payStatus: filters.payStatus
    })
    orders.value = res.records || []
    pager.value.total = res.total || 0
    if (res.summary) {
      orderSummary.orderCount = Number(res.summary.orderCount ?? 0)
      orderSummary.confirmedCount = Number(res.summary.confirmedCount ?? 0)
      orderSummary.unpaidCount = Number(res.summary.unpaidCount ?? 0)
      orderSummary.totalAmount = Number(res.summary.totalPayable ?? 0)
    }
  } catch (error) {
    message.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [studentRes, classRes] = await Promise.all([
      apiGet<PageResult<StudentOption>>('/courses/students', { page: 1, pageSize: 100, status: 'ACTIVE' }),
      apiGet<PageResult<ClassOption>>('/courses/classes', { page: 1, pageSize: 100 })
    ])
    students.value = studentRes.records || []
    classGroups.value = classRes.records || []
  } catch (error) {
    message.warning('订单选项加载失败，请确认课程中心已有学员和班级')
  }
}

function handleTableChange(paginationValue: any) {
  pager.value.page = paginationValue.current
  pager.value.pageSize = paginationValue.pageSize
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
  Object.assign(orderForm, {
    studentId: students.value[0]?.id,
    classGroupId: classGroups.value[0]?.id,
    customerName: '',
    customerPhone: '',
    sourceChannel: 'MANUAL',
    discountAmount: 0,
    remark: ''
  })
  orderModalOpen.value = true
}

async function submitOrder() {
  await orderFormRef.value?.validate?.()
  orderSaving.value = true
  try {
    await apiPost('/orders/enrollment', orderForm)
    message.success('订单已创建')
    orderModalOpen.value = false
    await loadOrders()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '创建订单失败')
  } finally {
    orderSaving.value = false
  }
}

async function openOrderDetail(record: OrderRecord) {
  detailDrawerOpen.value = true
  detailLoading.value = true
  try {
    orderDetail.value = await apiGet(`/orders/${record.id}`)
  } catch (error) {
    message.error('加载订单详情失败')
  } finally {
    detailLoading.value = false
  }
}

function confirmOrder(record: OrderRecord) {
  Modal.confirm({
    title: '确认订单并入班？',
    content: `确认后会为 ${record.studentName} 创建/激活班级报名记录。`,
    async onOk() {
      await apiPut(`/orders/${record.id}/confirm`)
      message.success('订单已确认')
      await loadOrders()
    }
  })
}

function cancelOrder(record: OrderRecord) {
  Modal.confirm({
    title: '取消订单？',
    content: '已收款且未退款的订单不能直接取消。',
    okButtonProps: { danger: true },
    async onOk() {
      await apiPut(`/orders/${record.id}/cancel`)
      message.success('订单已取消')
      await loadOrders()
    }
  })
}

function openPayment(record: OrderRecord) {
  activeOrder.value = record
  Object.assign(paymentForm, {
    amount: Number(record.outstandingAmount || 0),
    paymentMethod: 'CASH',
    receivedAt: dayjs(),
    remark: ''
  })
  paymentModalOpen.value = true
}

async function submitPayment() {
  await paymentFormRef.value?.validate?.()
  if (!activeOrder.value) return
  paymentSaving.value = true
  try {
    await apiPost(`/orders/${activeOrder.value.id}/payments`, {
      amount: paymentForm.amount,
      paymentMethod: paymentForm.paymentMethod,
      receivedAt: paymentForm.receivedAt ? paymentForm.receivedAt.format('YYYY-MM-DDTHH:mm:ss') : undefined,
      remark: paymentForm.remark
    })
    message.success('收款已记录')
    paymentModalOpen.value = false
    await loadOrders()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '收款失败')
  } finally {
    paymentSaving.value = false
  }
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
.order-summary {
  margin-bottom: 16px;
}

.filter-form {
  row-gap: 8px;
}

.stacked-cell {
  display: grid;
  gap: 3px;
}

.stacked-cell strong {
  color: var(--edu-text);
  font-weight: 750;
}

.stacked-cell span {
  color: var(--edu-text-muted);
  font-size: 12px;
}
</style>
