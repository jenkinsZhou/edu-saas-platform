<template>
  <h1 class="page-title">订单中心</h1>
  <div class="stack">
    <div class="panel">
      <div class="section-head">
        <strong>报名单 / 订单</strong>
        <div class="actions">
          <el-button @click="loadOrders">刷新</el-button>
          <el-button type="primary" @click="openCreateOrder">新增订单</el-button>
        </div>
      </div>

      <el-form class="filter-bar" inline>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="订单号 / 学员 / 联系人" style="width: 240px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="filters.orderStatus" clearable style="width: 140px">
            <el-option label="已创建" value="CREATED" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款状态">
          <el-select v-model="filters.payStatus" clearable style="width: 150px">
            <el-option label="未付款" value="UNPAID" />
            <el-option label="部分付款" value="PART_PAID" />
            <el-option label="已付款" value="PAID" />
            <el-option label="部分退款" value="PART_REFUNDED" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="order-stats">
        <div class="stat-block">
          <span>订单数</span>
          <strong>{{ orderSummary.orderCount }}</strong>
        </div>
        <div class="stat-block">
          <span>已确认</span>
          <strong>{{ orderSummary.confirmedCount }}</strong>
        </div>
        <div class="stat-block">
          <span>未付款</span>
          <strong>{{ orderSummary.unpaidCount }}</strong>
        </div>
        <div class="stat-block">
          <span>应收总额</span>
          <strong>{{ orderSummary.totalPayable }}</strong>
        </div>
      </div>

      <el-table :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="210" />
        <el-table-column prop="studentName" label="学员" width="110" />
        <el-table-column prop="classGroupName" label="班级" min-width="180" />
        <el-table-column prop="courseProductName" label="课程" min-width="180" />
        <el-table-column prop="customerName" label="联系人" width="110" />
        <el-table-column prop="orderStatus" label="订单状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)">{{ row.orderStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="付款状态" width="110">
          <template #default="{ row }">
            <el-tag :type="payTagType(row.payStatus)">{{ row.payStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payableAmount" label="应收" width="100" />
        <el-table-column prop="outstandingAmount" label="待收" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="primary" :disabled="row.orderStatus === 'CANCELLED' || row.payStatus === 'PAID'" @click="openPaymentDialog(row)">
              收款
            </el-button>
            <el-button link type="warning" :disabled="refundableAmount(row) <= 0" @click="openRefundDialog(row)">
              退款
            </el-button>
            <el-button link type="primary" :disabled="row.orderStatus !== 'CREATED'" @click="confirmOrder(row.id)">
              确认
            </el-button>
            <el-button link type="danger" :disabled="row.orderStatus === 'CANCELLED'" @click="cancelOrder(row.id)">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pager.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadOrders"
          @size-change="loadOrders"
        />
      </div>
    </div>
  </div>

  <el-dialog v-model="orderDialogVisible" :title="orderForm.id ? '编辑订单' : '新增订单'" width="720px">
    <el-form label-width="100px">
      <el-form-item label="班级">
        <el-select v-model="orderForm.classGroupId" filterable style="width: 100%" @change="syncCustomerFromStudent">
          <el-option v-for="item in classGroups" :key="item.id" :label="`${item.name} / ${item.courseProductName}`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="学员">
        <el-select v-model="orderForm.studentId" filterable style="width: 100%" @change="syncCustomerFromStudent">
          <el-option v-for="student in students" :key="student.id" :label="student.name" :value="student.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="orderForm.customerName" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="orderForm.customerPhone" />
      </el-form-item>
      <el-form-item label="来源渠道">
        <el-input v-model="orderForm.sourceChannel" placeholder="MANUAL / ONLINE / COUNTER" />
      </el-form-item>
      <el-form-item label="优惠金额">
        <el-input-number v-model="orderForm.discountAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="orderForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="orderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveOrder">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailDialogVisible" title="订单详情" width="860px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">{{ detail.orderStatus }}</el-descriptions-item>
      <el-descriptions-item label="学员">{{ detail.studentName }}</el-descriptions-item>
      <el-descriptions-item label="班级">{{ detail.classGroupName }}</el-descriptions-item>
      <el-descriptions-item label="课程">{{ detail.courseProductName }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ detail.customerName }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ detail.customerPhone }}</el-descriptions-item>
      <el-descriptions-item label="应收">{{ detail.payableAmount }}</el-descriptions-item>
      <el-descriptions-item label="已收">{{ detail.paidAmount }}</el-descriptions-item>
      <el-descriptions-item label="已退">{{ detail.refundedAmount }}</el-descriptions-item>
      <el-descriptions-item label="净收">{{ detail.netPaidAmount }}</el-descriptions-item>
      <el-descriptions-item label="待收">{{ detail.outstandingAmount }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ detail.sourceChannel }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
    </el-descriptions>

    <el-table :data="detail.items || []" style="width: 100%; margin-top: 16px">
      <el-table-column prop="itemName" label="明细名称" min-width="220" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="unitPrice" label="单价" width="100" />
      <el-table-column prop="discountAmount" label="优惠" width="100" />
      <el-table-column prop="amount" label="小计" width="100" />
    </el-table>

    <div class="sub-section-head">
      <strong>收款流水</strong>
      <el-button
        type="primary"
        :disabled="detail.orderStatus === 'CANCELLED' || detail.payStatus === 'PAID'"
        @click="openPaymentDialog(detail)"
      >
        登记收款
      </el-button>
    </div>
    <el-table :data="detail.payments || []" style="width: 100%">
      <el-table-column prop="paymentNo" label="流水号" width="210" />
      <el-table-column prop="paymentMethod" label="方式" width="120" />
      <el-table-column prop="amount" label="金额" width="120" />
      <el-table-column prop="channelTradeNo" label="渠道单号" min-width="180" />
      <el-table-column prop="receivedAt" label="收款时间" width="180" />
      <el-table-column prop="remark" label="备注" min-width="160" />
    </el-table>

    <div class="sub-section-head">
      <strong>退款流水</strong>
      <el-button type="warning" :disabled="refundableAmount(detail) <= 0" @click="openRefundDialog(detail)">
        登记退款
      </el-button>
    </div>
    <el-table :data="detail.refunds || []" style="width: 100%">
      <el-table-column prop="refundNo" label="退款号" width="210" />
      <el-table-column prop="refundMethod" label="方式" width="120" />
      <el-table-column prop="amount" label="金额" width="120" />
      <el-table-column prop="channelTradeNo" label="渠道单号" min-width="180" />
      <el-table-column prop="refundedAt" label="退款时间" width="180" />
      <el-table-column prop="refundReason" label="原因" min-width="160" />
    </el-table>

    <template #footer>
      <el-button v-if="detail.orderStatus !== 'CANCELLED' && detail.payStatus !== 'PAID'" type="primary" @click="openPaymentDialog(detail)">
        登记收款
      </el-button>
      <el-button v-if="refundableAmount(detail) > 0" type="warning" @click="openRefundDialog(detail)">
        登记退款
      </el-button>
      <el-button v-if="detail.orderStatus === 'CREATED'" type="primary" @click="confirmDetailOrder">确认报名</el-button>
      <el-button v-if="detail.orderStatus !== 'CANCELLED'" type="danger" @click="cancelDetailOrder">取消订单</el-button>
      <el-button @click="detailDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="paymentDialogVisible" :title="`登记收款 - ${paymentForm.orderNo}`" width="520px">
    <el-form label-width="100px">
      <el-form-item label="待收金额">
        <el-input v-model="paymentForm.outstandingAmount" disabled />
      </el-form-item>
      <el-form-item label="收款金额">
        <el-input-number v-model="paymentForm.amount" :min="0" :precision="2" :step="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="收款方式">
        <el-select v-model="paymentForm.paymentMethod" style="width: 100%">
          <el-option label="现金" value="CASH" />
          <el-option label="刷卡" value="CARD" />
          <el-option label="银行转账" value="BANK_TRANSFER" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="支付宝" value="ALIPAY" />
        </el-select>
      </el-form-item>
      <el-form-item label="渠道单号">
        <el-input v-model="paymentForm.channelTradeNo" />
      </el-form-item>
      <el-form-item label="收款时间">
        <el-date-picker
          v-model="paymentForm.receivedAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="paymentForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="paymentDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="savePayment">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="refundDialogVisible" :title="`登记退款 - ${refundForm.orderNo}`" width="520px">
    <el-form label-width="100px">
      <el-form-item label="可退金额">
        <el-input v-model="refundForm.refundableAmount" disabled />
      </el-form-item>
      <el-form-item label="退款金额">
        <el-input-number v-model="refundForm.amount" :min="0" :precision="2" :step="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="退款方式">
        <el-select v-model="refundForm.refundMethod" style="width: 100%">
          <el-option label="现金" value="CASH" />
          <el-option label="刷卡退回" value="CARD" />
          <el-option label="银行转账" value="BANK_TRANSFER" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="支付宝" value="ALIPAY" />
        </el-select>
      </el-form-item>
      <el-form-item label="渠道单号">
        <el-input v-model="refundForm.channelTradeNo" />
      </el-form-item>
      <el-form-item label="退款时间">
        <el-date-picker
          v-model="refundForm.refundedAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="退款原因">
        <el-input v-model="refundForm.refundReason" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="refundDialogVisible = false">取消</el-button>
      <el-button type="warning" @click="saveRefund">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost, apiPut } from '../api/http'

interface OrderItem {
  id: number
  itemType: string
  itemName: string
  quantity: number
  unitPrice: string
  discountAmount: string
  amount: string
}

interface PaymentItem {
  id: number
  paymentNo: string
  paymentType: string
  paymentMethod: string
  amount: string
  channelTradeNo: string
  receivedAt: string
  remark: string
}

interface RefundItem {
  id: number
  refundNo: string
  refundType: string
  refundMethod: string
  amount: string
  channelTradeNo: string
  refundedAt: string
  refundReason: string
}

interface OrderListItem {
  id: number
  orderNo: string
  studentName: string
  classGroupName: string
  courseProductName: string
  customerName: string
  orderStatus: string
  payStatus: string
  payableAmount: string
  paidAmount: string
  refundedAmount: string
  netPaidAmount: string
  outstandingAmount: string
  sourceChannel: string
  createdAt: string
}

interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  summary: {
    orderCount: number
    confirmedCount: number
    unpaidCount: number
    totalPayable: string
  }
}

interface ClassGroupItem {
  id: number
  name: string
  courseProductName: string
}

interface StudentItem {
  id: number
  name: string
  phone: string
  guardianName: string
  guardianPhone: string
}

interface OrderDetail extends OrderListItem {
  customerPhone: string
  totalAmount: string
  discountAmount: string
  remark: string
  items: OrderItem[]
  payments: PaymentItem[]
  refunds: RefundItem[]
}

const orders = ref<OrderListItem[]>([])
const students = ref<StudentItem[]>([])
const classGroups = ref<ClassGroupItem[]>([])
const orderDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const paymentDialogVisible = ref(false)
const refundDialogVisible = ref(false)
const filters = reactive({
  keyword: '',
  orderStatus: '',
  payStatus: ''
})
const orderSummary = reactive({
  orderCount: 0,
  confirmedCount: 0,
  unpaidCount: 0,
  totalPayable: '0.00'
})
const pager = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})
const detail = reactive<OrderDetail>({
  id: 0,
  orderNo: '',
  studentName: '',
  classGroupName: '',
  courseProductName: '',
  customerName: '',
  customerPhone: '',
  orderStatus: '',
  payStatus: '',
  payableAmount: '0.00',
  outstandingAmount: '0.00',
  sourceChannel: '',
  createdAt: '',
  totalAmount: '0.00',
  discountAmount: '0.00',
  paidAmount: '0.00',
  refundedAmount: '0.00',
  netPaidAmount: '0.00',
  remark: '',
  items: [],
  payments: [],
  refunds: []
})

const orderForm = reactive({
  id: 0,
  classGroupId: 0,
  studentId: 0,
  customerName: '',
  customerPhone: '',
  sourceChannel: 'MANUAL',
  discountAmount: 0,
  remark: ''
})

const paymentForm = reactive({
  orderId: 0,
  orderNo: '',
  outstandingAmount: '0.00',
  amount: 0,
  paymentMethod: 'CASH',
  channelTradeNo: '',
  receivedAt: '',
  remark: ''
})

const refundForm = reactive({
  orderId: 0,
  orderNo: '',
  refundableAmount: '0.00',
  amount: 0,
  refundMethod: 'CASH',
  channelTradeNo: '',
  refundedAt: '',
  refundReason: ''
})

async function loadOrders() {
  const result = await apiGet<PageResult<OrderListItem>>(
    `/orders?page=${pager.page}&pageSize=${pager.pageSize}&keyword=${encodeURIComponent(filters.keyword)}&orderStatus=${encodeURIComponent(filters.orderStatus)}&payStatus=${encodeURIComponent(filters.payStatus)}`
  )
  orders.value = result.records
  pager.total = result.total
  pager.page = result.page
  pager.pageSize = result.pageSize
  Object.assign(orderSummary, result.summary)
}

async function loadStudents() {
  students.value = await apiGet<StudentItem[]>('/courses/students')
}

async function loadClasses() {
  classGroups.value = await apiGet<ClassGroupItem[]>('/courses/classes')
}

function openCreateOrder() {
  resetOrderForm()
  syncCustomerFromStudent()
  orderDialogVisible.value = true
}

async function applyFilters() {
  pager.page = 1
  await loadOrders()
}

async function resetFilters() {
  filters.keyword = ''
  filters.orderStatus = ''
  filters.payStatus = ''
  pager.page = 1
  await loadOrders()
}

function resetOrderForm() {
  orderForm.id = 0
  orderForm.classGroupId = classGroups.value[0]?.id ?? 0
  orderForm.studentId = students.value[0]?.id ?? 0
  orderForm.customerName = ''
  orderForm.customerPhone = ''
  orderForm.sourceChannel = 'MANUAL'
  orderForm.discountAmount = 0
  orderForm.remark = ''
}

function syncCustomerFromStudent() {
  const student = students.value.find((item) => item.id === orderForm.studentId)
  if (!student) {
    return
  }
  orderForm.customerName = student.guardianName || student.name
  orderForm.customerPhone = student.guardianPhone || student.phone
}

async function saveOrder() {
  await apiPost<number>('/orders/enrollment', {
    classGroupId: orderForm.classGroupId,
    studentId: orderForm.studentId,
    customerName: orderForm.customerName,
    customerPhone: orderForm.customerPhone,
    sourceChannel: orderForm.sourceChannel,
    discountAmount: orderForm.discountAmount,
    remark: orderForm.remark
  })
  ElMessage.success('订单已创建')
  orderDialogVisible.value = false
  await loadOrders()
}

async function openDetail(id: number) {
  Object.assign(detail, await apiGet<OrderDetail>(`/orders/${id}`))
  detailDialogVisible.value = true
}

function openPaymentDialog(order: Pick<OrderListItem, 'id' | 'orderNo' | 'outstandingAmount'>) {
  paymentForm.orderId = order.id
  paymentForm.orderNo = order.orderNo
  paymentForm.outstandingAmount = order.outstandingAmount
  paymentForm.amount = Number(order.outstandingAmount || 0)
  paymentForm.paymentMethod = 'CASH'
  paymentForm.channelTradeNo = ''
  paymentForm.receivedAt = formatLocalDateTime(new Date())
  paymentForm.remark = ''
  paymentDialogVisible.value = true
}

async function savePayment() {
  if (!paymentForm.orderId) {
    ElMessage.warning('请先选择订单')
    return
  }
  if (paymentForm.amount <= 0) {
    ElMessage.warning('收款金额必须大于 0')
    return
  }
  await apiPost<number>(`/orders/${paymentForm.orderId}/payments`, {
    amount: paymentForm.amount,
    paymentMethod: paymentForm.paymentMethod,
    channelTradeNo: paymentForm.channelTradeNo,
    receivedAt: paymentForm.receivedAt,
    remark: paymentForm.remark
  })
  ElMessage.success('收款已登记')
  paymentDialogVisible.value = false
  await loadOrders()
  if (detailDialogVisible.value && detail.id === paymentForm.orderId) {
    await openDetail(paymentForm.orderId)
  }
}

function openRefundDialog(order: Pick<OrderListItem, 'id' | 'orderNo' | 'paidAmount' | 'refundedAmount'>) {
  const amount = refundableAmount(order)
  refundForm.orderId = order.id
  refundForm.orderNo = order.orderNo
  refundForm.refundableAmount = amount.toFixed(2)
  refundForm.amount = amount
  refundForm.refundMethod = 'CASH'
  refundForm.channelTradeNo = ''
  refundForm.refundedAt = formatLocalDateTime(new Date())
  refundForm.refundReason = ''
  refundDialogVisible.value = true
}

async function saveRefund() {
  if (!refundForm.orderId) {
    ElMessage.warning('请先选择订单')
    return
  }
  if (refundForm.amount <= 0) {
    ElMessage.warning('退款金额必须大于 0')
    return
  }
  await apiPost<number>(`/orders/${refundForm.orderId}/refunds`, {
    amount: refundForm.amount,
    refundMethod: refundForm.refundMethod,
    channelTradeNo: refundForm.channelTradeNo,
    refundedAt: refundForm.refundedAt,
    refundReason: refundForm.refundReason
  })
  ElMessage.success('退款已登记')
  refundDialogVisible.value = false
  await loadOrders()
  if (detailDialogVisible.value && detail.id === refundForm.orderId) {
    await openDetail(refundForm.orderId)
  }
}

async function confirmOrder(id: number) {
  await apiPut<void>(`/orders/${id}/confirm`)
  ElMessage.success('订单已确认')
  await loadOrders()
  if (detailDialogVisible.value && detail.id === id) {
    await openDetail(id)
  }
}

async function cancelOrder(id: number) {
  await apiPut<void>(`/orders/${id}/cancel`)
  ElMessage.success('订单已取消')
  await loadOrders()
  if (detailDialogVisible.value && detail.id === id) {
    await openDetail(id)
  }
}

async function confirmDetailOrder() {
  await confirmOrder(detail.id)
}

async function cancelDetailOrder() {
  await cancelOrder(detail.id)
}

function statusTagType(status: string) {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

function payTagType(status: string) {
  if (status === 'PAID') return 'success'
  if (status === 'PART_PAID') return 'warning'
  if (status === 'PART_REFUNDED') return 'warning'
  if (status === 'REFUNDED') return 'info'
  return 'danger'
}

function refundableAmount(order: Pick<OrderListItem, 'paidAmount' | 'refundedAmount'>) {
  return Math.max(Number(order.paidAmount || 0) - Number(order.refundedAmount || 0), 0)
}

function formatLocalDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

onMounted(async () => {
  await Promise.all([loadStudents(), loadClasses()])
  if (classGroups.value.length > 0) {
    orderForm.classGroupId = classGroups.value[0].id
  }
  if (students.value.length > 0) {
    orderForm.studentId = students.value[0].id
    syncCustomerFromStudent()
  }
  await loadOrders()
})
</script>

<style scoped>
.stack {
  display: grid;
  gap: 16px;
}

.actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  margin-bottom: 12px;
}

.order-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.stat-block span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.stat-block strong {
  font-size: 18px;
}

.sub-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 10px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
