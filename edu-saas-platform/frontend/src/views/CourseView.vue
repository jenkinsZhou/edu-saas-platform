<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="课程中心" sub-title="课程产品、班级、课次管理">
        <template #extra>
          <a-space>
            <a-button type="primary" @click="createDemoCourse">
              <template #icon><PlusOutlined /></template>
              新增示例课程
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <a-card :bordered="false" title="课程产品" style="margin-top: 16px">
      <template #extra>
        <a-button type="primary" @click="createDemoCourse">
          <template #icon><PlusOutlined /></template>
          新增课程
        </a-button>
      </template>

      <a-table
        :columns="courseColumns"
        :data-source="courses"
        :loading="courseLoading"
        :pagination="{
          current: coursePager.page,
          pageSize: coursePager.pageSize,
          total: coursePager.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`
        }"
        @change="handleCourseTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'listPrice'">
            <span style="color: #ff4d4f; font-weight: 500">¥{{ record.listPrice }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'ENABLED' ? 'green' : 'default'">
              {{ record.status === 'ENABLED' ? '上架' : '下架' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small">编辑</a-button>
              <a-button
                type="link"
                danger
                size="small"
                :disabled="record.status === 'DISABLED'"
                @click="disableCourse(record.id)"
              >
                下架
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" title="班级管理" style="margin-top: 16px">
      <template #extra>
        <a-button type="primary" @click="openCreateClass">
          <template #icon><PlusOutlined /></template>
          新增班级
        </a-button>
      </template>

      <a-table
        :columns="classColumns"
        :data-source="classGroups"
        :loading="classLoading"
        :pagination="{
          current: classPager.page,
          pageSize: classPager.pageSize,
          total: classPager.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`
        }"
        @change="handleClassTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditClass(record)">编辑</a-button>
              <a-button type="link" size="small">学员</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" title="课次安排" style="margin-top: 16px">
      <template #extra>
        <a-button type="primary" @click="openCreateLesson">
          <template #icon><PlusOutlined /></template>
          新增课次
        </a-button>
      </template>

      <a-table
        :columns="lessonColumns"
        :data-source="lessons"
        :loading="lessonLoading"
        :pagination="{
          current: lessonPager.page,
          pageSize: lessonPager.pageSize,
          total: lessonPager.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`
        }"
        @change="handleLessonTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'time'">
            <div>{{ record.plannedStartAt }} ~ {{ record.plannedEndAt }}</div>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getLessonStatusColor(record.status)">
              {{ getLessonStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small">编辑</a-button>
              <a-button type="link" size="small">考勤</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { apiGet, apiPost } from '../api/http'

const courseLoading = ref(false)
const classLoading = ref(false)
const lessonLoading = ref(false)

const courses = ref<any[]>([])
const classGroups = ref<any[]>([])
const lessons = ref<any[]>([])

const coursePager = ref({ page: 1, pageSize: 10, total: 0 })
const classPager = ref({ page: 1, pageSize: 10, total: 0 })
const lessonPager = ref({ page: 1, pageSize: 10, total: 0 })

const courseColumns = [
  { title: '课程名称', dataIndex: 'name', key: 'name' },
  { title: '业务类型', dataIndex: 'categoryCode', key: 'categoryCode', width: 120 },
  { title: '授课方式', dataIndex: 'deliveryMode', key: 'deliveryMode', width: 120 },
  { title: '计费方式', dataIndex: 'billingMode', key: 'billingMode', width: 120 },
  { title: '课时', dataIndex: 'totalLessons', key: 'totalLessons', width: 80 },
  { title: '标价', dataIndex: 'listPrice', key: 'listPrice', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150 }
]

const classColumns = [
  { title: '班级名称', dataIndex: 'name', key: 'name' },
  { title: '课程产品', dataIndex: 'courseProductName', key: 'courseProductName' },
  { title: '校区', dataIndex: 'campusName', key: 'campusName', width: 120 },
  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 80 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150 }
]

const lessonColumns = [
  { title: '班级', dataIndex: 'classGroupName', key: 'classGroupName' },
  { title: '上课时间', key: 'time', width: 300 },
  { title: '教室', dataIndex: 'classroomName', key: 'classroomName', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150 }
]

onMounted(() => {
  loadCourses()
  loadClasses()
  loadLessons()
})

async function loadCourses() {
  courseLoading.value = true
  try {
    const res = await apiGet<any>('/course/products', {
      page: coursePager.value.page,
      pageSize: coursePager.value.pageSize
    })
    courses.value = res.items || []
    coursePager.value.total = res.total || 0
  } catch (error) {
    message.error('加载课程失败')
  } finally {
    courseLoading.value = false
  }
}

async function loadClasses() {
  classLoading.value = true
  try {
    const res = await apiGet<any>('/course/class-groups', {
      page: classPager.value.page,
      pageSize: classPager.value.pageSize
    })
    classGroups.value = res.items || []
    classPager.value.total = res.total || 0
  } catch (error) {
    message.error('加载班级失败')
  } finally {
    classLoading.value = false
  }
}

async function loadLessons() {
  lessonLoading.value = true
  try {
    const res = await apiGet<any>('/course/lessons', {
      page: lessonPager.value.page,
      pageSize: lessonPager.value.pageSize
    })
    lessons.value = res.items || []
    lessonPager.value.total = res.total || 0
  } catch (error) {
    message.error('加载课次失败')
  } finally {
    lessonLoading.value = false
  }
}

function handleCourseTableChange(pagination: any) {
  coursePager.value.page = pagination.current
  coursePager.value.pageSize = pagination.pageSize
  loadCourses()
}

function handleClassTableChange(pagination: any) {
  classPager.value.page = pagination.current
  classPager.value.pageSize = pagination.pageSize
  loadClasses()
}

function handleLessonTableChange(pagination: any) {
  lessonPager.value.page = pagination.current
  lessonPager.value.pageSize = pagination.pageSize
  loadLessons()
}

async function createDemoCourse() {
  try {
    await apiPost('/course/products', {
      name: '示例课程',
      categoryCode: 'DEMO',
      deliveryMode: 'OFFLINE',
      billingMode: 'BY_SESSION',
      totalLessons: 12,
      listPrice: 1200
    })
    message.success('创建成功')
    loadCourses()
  } catch (error) {
    message.error('创建失败')
  }
}

async function disableCourse(id: number) {
  try {
    await apiPost(`/course/products/${id}/disable`)
    message.success('下架成功')
    loadCourses()
  } catch (error) {
    message.error('下架失败')
  }
}

function openCreateClass() {
  message.info('新增班级功能开发中')
}

function openEditClass(record: any) {
  message.info('编辑班级功能开发中')
}

function openCreateLesson() {
  message.info('新增课次功能开发中')
}

function getStatusColor(status: string) {
  const colors: Record<string, string> = {
    ACTIVE: 'green',
    INACTIVE: 'default',
    COMPLETED: 'blue'
  }
  return colors[status] || 'default'
}

function getStatusText(status: string) {
  const texts: Record<string, string> = {
    ACTIVE: '进行中',
    INACTIVE: '未开始',
    COMPLETED: '已结束'
  }
  return texts[status] || status
}

function getLessonStatusColor(status: string) {
  const colors: Record<string, string> = {
    SCHEDULED: 'blue',
    IN_PROGRESS: 'green',
    COMPLETED: 'default',
    CANCELLED: 'red'
  }
  return colors[status] || 'default'
}

function getLessonStatusText(status: string) {
  const texts: Record<string, string> = {
    SCHEDULED: '待上课',
    IN_PROGRESS: '上课中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}
</script>

<style scoped>
.page-container {
  padding: 0;
}

.page-header {
  margin-bottom: 0;
}
</style>
