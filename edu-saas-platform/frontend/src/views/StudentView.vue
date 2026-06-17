<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="学员管理" sub-title="学员档案、联系人与状态管理">
        <template #extra>
          <a-space>
            <a-input-search
              v-model:value="filters.keyword"
              placeholder="姓名 / 电话 / 家长"
              style="width: 220px"
              allow-clear
              @search="applyFilters"
            />
            <a-select
              v-model:value="filters.status"
              placeholder="状态"
              style="width: 120px"
              allow-clear
              @change="applyFilters"
            >
              <a-select-option value="ACTIVE">在读</a-select-option>
              <a-select-option value="INACTIVE">停课</a-select-option>
              <a-select-option value="GRADUATED">结业</a-select-option>
            </a-select>
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              新增学员
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <div class="metric-strip student-summary">
      <a-card :bordered="false" :loading="loading" class="data-card" style="--card-accent: #1e40af">
        <a-statistic title="学员总数" :value="stats.total" />
      </a-card>
      <a-card :bordered="false" :loading="loading" class="data-card" style="--card-accent: #16a34a">
        <a-statistic title="在读学员" :value="stats.active" />
      </a-card>
      <a-card :bordered="false" :loading="loading" class="data-card" style="--card-accent: #d97706">
        <a-statistic title="停课 / 结业（本页）" :value="stats.inactive" />
      </a-card>
    </div>

    <a-card :bordered="false">
      <a-table
        :data-source="students"
        :columns="columns"
        :loading="loading"
        :pagination="{
          current: pager.page,
          pageSize: pager.pageSize,
          total: pager.total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 名学员`
        }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑学员' : '新增学员'"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form :model="form" layout="vertical" style="margin-top: 16px">
        <a-form-item label="姓名" required>
          <a-input v-model:value="form.name" placeholder="学员姓名" :maxlength="80" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="联系电话">
              <a-input v-model:value="form.phone" placeholder="学员电话" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="来源">
              <a-select v-model:value="form.source" placeholder="获客来源" allow-clear>
                <a-select-option value="转介绍">转介绍</a-select-option>
                <a-select-option value="试听">试听</a-select-option>
                <a-select-option value="线上咨询">线上咨询</a-select-option>
                <a-select-option value="地推活动">地推活动</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="家长姓名">
              <a-input v-model:value="form.guardianName" placeholder="家长/监护人" :maxlength="80" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="家长电话">
              <a-input v-model:value="form.guardianPhone" placeholder="家长电话" :maxlength="32" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="状态" required>
          <a-radio-group v-model:value="form.status">
            <a-radio-button value="ACTIVE">在读</a-radio-button>
            <a-radio-button value="INACTIVE">停课</a-radio-button>
            <a-radio-button value="GRADUATED">结业</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { apiGet, apiPost, apiPut } from '../api/http'

interface StudentRecord {
  id: number
  name: string
  phone: string
  guardianName: string
  guardianPhone: string
  source: string
  status: string
}

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const students = ref<StudentRecord[]>([])

const filters = reactive({ keyword: '', status: undefined as string | undefined })
const pager = ref({ page: 1, pageSize: 10, total: 0 })

const form = reactive({
  name: '',
  phone: '',
  guardianName: '',
  guardianPhone: '',
  source: '',
  status: 'ACTIVE'
})

const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '家长', dataIndex: 'guardianName', key: 'guardianName' },
  { title: '家长电话', dataIndex: 'guardianPhone', key: 'guardianPhone' },
  { title: '来源', dataIndex: 'source', key: 'source' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 90 }
]

const stats = computed(() => {
  const active = students.value.filter(s => s.status === 'ACTIVE').length
  return {
    total: pager.value.total,
    active,
    inactive: students.value.length - active
  }
})

onMounted(loadStudents)

async function loadStudents() {
  loading.value = true
  try {
    const res = await apiGet<any>('/courses/students', {
      page: pager.value.page,
      pageSize: pager.value.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    students.value = res.records || []
    pager.value.total = res.total || 0
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载学员失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  pager.value.page = 1
  loadStudents()
}

function handleTableChange(pagination: any) {
  pager.value.page = pagination.current
  pager.value.pageSize = pagination.pageSize
  loadStudents()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', phone: '', guardianName: '', guardianPhone: '', source: '', status: 'ACTIVE' })
  modalOpen.value = true
}

function openEdit(record: StudentRecord) {
  editingId.value = record.id
  Object.assign(form, {
    name: record.name,
    phone: record.phone,
    guardianName: record.guardianName,
    guardianPhone: record.guardianPhone,
    source: record.source,
    status: record.status
  })
  modalOpen.value = true
}

async function handleSave() {
  if (!form.name.trim()) {
    message.warning('请输入学员姓名')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await apiPut(`/courses/students/${editingId.value}`, { ...form })
      message.success('学员已更新')
    } else {
      await apiPost('/courses/students', { ...form })
      message.success('学员已创建')
    }
    modalOpen.value = false
    loadStudents()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function statusLabel(status: string) {
  return { ACTIVE: '在读', INACTIVE: '停课', GRADUATED: '结业' }[status] ?? status
}

function statusColor(status: string) {
  return { ACTIVE: 'green', INACTIVE: 'orange', GRADUATED: 'blue' }[status] ?? 'default'
}
</script>

<style scoped>
.page-container {
  padding: 0;
}

.student-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 768px) {
  .student-summary {
    grid-template-columns: 1fr;
  }
}
</style>
