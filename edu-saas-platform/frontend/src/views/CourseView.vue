<template>
  <div class="page-container course-page">
    <a-card :bordered="false" class="page-header">
      <a-page-header title="课程中心" sub-title="学员、课程产品、班级与课次的真实业务管理">
        <template #extra>
          <a-space wrap>
            <a-button @click="reloadAll">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button type="primary" @click="openCreateStudent">
              <template #icon><UserAddOutlined /></template>
              新增学员
            </a-button>
            <a-button type="primary" @click="openCreateCourse">
              <template #icon><PlusOutlined /></template>
              新增课程
            </a-button>
          </a-space>
        </template>
      </a-page-header>
    </a-card>

    <a-card :bordered="false" title="学员档案">
      <template #extra>
        <a-space wrap>
          <a-input-search
            v-model:value="studentFilters.keyword"
            placeholder="搜索学员/手机号/家长"
            allow-clear
            style="width: 240px"
            @search="applyStudentFilters"
          />
          <a-select
            v-model:value="studentFilters.status"
            placeholder="状态"
            allow-clear
            style="width: 120px"
            @change="applyStudentFilters"
          >
            <a-select-option value="ACTIVE">在读</a-select-option>
            <a-select-option value="INACTIVE">停课</a-select-option>
          </a-select>
          <a-button type="primary" @click="openCreateStudent">
            <template #icon><UserAddOutlined /></template>
            新增学员
          </a-button>
        </a-space>
      </template>

      <a-table
        row-key="id"
        :columns="studentColumns"
        :data-source="students"
        :loading="studentLoading"
        :pagination="studentPagination"
        @change="handleStudentTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'contact'">
            <div class="stacked-cell">
              <strong>{{ record.phone || '未填写手机号' }}</strong>
              <span>{{ record.guardianName || '未填写家长' }} {{ record.guardianPhone || '' }}</span>
            </div>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStudentStatusColor(record.status)">
              {{ getStudentStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditStudent(record)">编辑</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" title="课程产品">
      <template #extra>
        <a-space wrap>
          <a-input-search
            v-model:value="courseFilters.keyword"
            placeholder="搜索课程/分类"
            allow-clear
            style="width: 220px"
            @search="applyCourseFilters"
          />
          <a-select
            v-model:value="courseFilters.status"
            placeholder="状态"
            allow-clear
            style="width: 120px"
            @change="applyCourseFilters"
          >
            <a-select-option value="ON_SALE">上架</a-select-option>
            <a-select-option value="DISABLED">下架</a-select-option>
          </a-select>
          <a-button type="primary" @click="openCreateCourse">
            <template #icon><PlusOutlined /></template>
            新增课程
          </a-button>
        </a-space>
      </template>

      <a-table
        row-key="id"
        :columns="courseColumns"
        :data-source="courses"
        :loading="courseLoading"
        :pagination="coursePagination"
        @change="handleCourseTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'listPrice'">
            <span class="amount-text">¥{{ record.listPrice }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getCourseStatusColor(record.status)">
              {{ getCourseStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditCourse(record)">编辑</a-button>
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

    <a-card :bordered="false" title="班级管理">
      <template #extra>
        <a-space wrap>
          <a-input-search
            v-model:value="classFilters.keyword"
            placeholder="搜索班级"
            allow-clear
            style="width: 220px"
            @search="applyClassFilters"
          />
          <a-select
            v-model:value="classFilters.status"
            placeholder="状态"
            allow-clear
            style="width: 120px"
            @change="applyClassFilters"
          >
            <a-select-option value="ACTIVE">进行中</a-select-option>
            <a-select-option value="INACTIVE">未开始</a-select-option>
            <a-select-option value="COMPLETED">已结束</a-select-option>
          </a-select>
          <a-button type="primary" @click="openCreateClass">
            <template #icon><PlusOutlined /></template>
            新增班级
          </a-button>
        </a-space>
      </template>

      <a-table
        row-key="id"
        :columns="classColumns"
        :data-source="classGroups"
        :loading="classLoading"
        :pagination="classPagination"
        @change="handleClassTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getClassStatusColor(record.status)">
              {{ getClassStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditClass(record)">编辑</a-button>
              <a-button type="link" size="small" @click="openEnrollmentDrawer(record)">学员</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" title="课次安排">
      <template #extra>
        <a-space wrap>
          <a-select
            v-model:value="lessonFilters.status"
            placeholder="状态"
            allow-clear
            style="width: 140px"
            @change="applyLessonFilters"
          >
            <a-select-option value="SCHEDULED">待上课</a-select-option>
            <a-select-option value="IN_PROGRESS">上课中</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
          </a-select>
          <a-button type="primary" @click="openCreateLesson">
            <template #icon><PlusOutlined /></template>
            新增课次
          </a-button>
        </a-space>
      </template>

      <a-table
        row-key="id"
        :columns="lessonColumns"
        :data-source="lessons"
        :loading="lessonLoading"
        :pagination="lessonPagination"
        @change="handleLessonTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'time'">
            <div class="stacked-cell">
              <strong>{{ formatDateTime(record.plannedStartAt) }}</strong>
              <span>至 {{ formatDateTime(record.plannedEndAt) }}</span>
            </div>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getLessonStatusColor(record.status)">
              {{ getLessonStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditLesson(record)">编辑</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="studentModalOpen"
      :title="editingStudentId ? '编辑学员' : '新增学员'"
      :confirm-loading="studentSaving"
      @ok="submitStudent"
    >
      <a-form ref="studentFormRef" :model="studentForm" :rules="studentRules" layout="vertical">
        <a-form-item label="学员姓名" name="name">
          <a-input v-model:value="studentForm.name" placeholder="请输入学员姓名" />
        </a-form-item>
        <a-form-item label="学员手机号" name="phone">
          <a-input v-model:value="studentForm.phone" placeholder="请输入学员手机号" />
        </a-form-item>
        <a-form-item label="家长姓名" name="guardianName">
          <a-input v-model:value="studentForm.guardianName" placeholder="请输入家长姓名" />
        </a-form-item>
        <a-form-item label="家长手机号" name="guardianPhone">
          <a-input v-model:value="studentForm.guardianPhone" placeholder="请输入家长手机号" />
        </a-form-item>
        <a-form-item label="来源" name="source">
          <a-select v-model:value="studentForm.source" placeholder="请选择来源">
            <a-select-option value="WALK_IN">到店咨询</a-select-option>
            <a-select-option value="ONLINE">线上线索</a-select-option>
            <a-select-option value="REFERRAL">转介绍</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="studentForm.status">
            <a-select-option value="ACTIVE">在读</a-select-option>
            <a-select-option value="INACTIVE">停课</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="courseModalOpen"
      :title="editingCourseId ? '编辑课程' : '新增课程'"
      :confirm-loading="courseSaving"
      @ok="submitCourse"
    >
      <a-form ref="courseFormRef" :model="courseForm" :rules="courseRules" layout="vertical">
        <a-form-item label="课程名称" name="name">
          <a-input v-model:value="courseForm.name" placeholder="例如：初中数学春季班" />
        </a-form-item>
        <a-form-item label="业务类型" name="categoryCode">
          <a-input v-model:value="courseForm.categoryCode" placeholder="例如：ACADEMIC" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="授课方式" name="deliveryMode">
              <a-select v-model:value="courseForm.deliveryMode">
                <a-select-option value="OFFLINE">线下</a-select-option>
                <a-select-option value="LIVE">直播</a-select-option>
                <a-select-option value="ONLINE">线上</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计费方式" name="billingMode">
              <a-select v-model:value="courseForm.billingMode">
                <a-select-option value="LESSON">按课时</a-select-option>
                <a-select-option value="TERM">按学期</a-select-option>
                <a-select-option value="PACKAGE">按套餐</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="总课时" name="totalLessons">
              <a-input-number v-model:value="courseForm.totalLessons" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标价" name="listPrice">
              <a-input-number v-model:value="courseForm.listPrice" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="扩展模板" name="extensionTemplateCode">
          <a-input v-model:value="courseForm.extensionTemplateCode" placeholder="例如：academic_course" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="courseForm.status">
            <a-select-option value="ON_SALE">上架</a-select-option>
            <a-select-option value="DISABLED">下架</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="classModalOpen"
      :title="editingClassId ? '编辑班级' : '新增班级'"
      :confirm-loading="classSaving"
      @ok="submitClass"
    >
      <a-form ref="classFormRef" :model="classForm" :rules="classRules" layout="vertical">
        <a-form-item label="班级名称" name="name">
          <a-input v-model:value="classForm.name" placeholder="请输入班级名称" />
        </a-form-item>
        <a-form-item label="课程产品" name="courseProductId">
          <a-select v-model:value="classForm.courseProductId" placeholder="请选择课程产品" show-search option-filter-prop="label">
            <a-select-option v-for="course in courseOptions" :key="course.id" :value="course.id" :label="course.name">
              {{ course.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="校区" name="campusId">
          <a-select v-model:value="classForm.campusId" placeholder="请选择校区" allow-clear option-filter-prop="label">
            <a-select-option v-for="campus in campuses" :key="campus.id" :value="campus.id" :label="campus.name">
              {{ campus.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="容量" name="capacity">
              <a-input-number v-model:value="classForm.capacity" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="classForm.status">
                <a-select-option value="ACTIVE">进行中</a-select-option>
                <a-select-option value="INACTIVE">未开始</a-select-option>
                <a-select-option value="COMPLETED">已结束</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate">
              <a-date-picker v-model:value="classForm.startDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="classForm.endDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="lessonModalOpen"
      :title="editingLessonId ? '编辑课次' : '新增课次'"
      :confirm-loading="lessonSaving"
      @ok="submitLesson"
    >
      <a-form ref="lessonFormRef" :model="lessonForm" :rules="lessonRules" layout="vertical">
        <a-form-item label="班级" name="classGroupId">
          <a-select v-model:value="lessonForm.classGroupId" placeholder="请选择班级" show-search option-filter-prop="label">
            <a-select-option v-for="group in classOptions" :key="group.id" :value="group.id" :label="group.name">
              {{ group.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="上课时间" name="timeRange">
          <a-range-picker
            v-model:value="lessonForm.timeRange"
            show-time
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="线上教室链接" name="onlineRoomUrl">
          <a-input v-model:value="lessonForm.onlineRoomUrl" placeholder="可选，直播/线上课程填写" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="lessonForm.status">
            <a-select-option value="SCHEDULED">待上课</a-select-option>
            <a-select-option value="IN_PROGRESS">上课中</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="enrollmentDrawerOpen"
      :title="`${selectedClass?.name || '班级'} - 学员管理`"
      width="520"
    >
      <a-space direction="vertical" :size="16" style="width: 100%">
        <a-alert message="选择学员后保存，会把学员加入当前班级；已在班级中的学员会保持激活状态。" type="info" show-icon />
        <a-select
          v-model:value="selectedStudentIds"
          mode="multiple"
          placeholder="请选择学员"
          style="width: 100%"
          option-filter-prop="label"
        >
          <a-select-option v-for="student in studentOptions" :key="student.id" :value="student.id" :label="student.name">
            {{ student.name }} {{ student.phone ? `(${student.phone})` : '' }}
          </a-select-option>
        </a-select>
        <a-textarea v-model:value="enrollmentRemark" placeholder="备注" :rows="3" />
        <a-button type="primary" :loading="enrollmentSaving" @click="submitEnrollments">保存学员</a-button>

        <a-table
          row-key="id"
          size="small"
          :columns="enrollmentColumns"
          :data-source="enrollments"
          :loading="enrollmentLoading"
          :pagination="false"
        />
      </a-space>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import { apiGet, apiPost, apiPut } from '../api/http'

interface Pager {
  page: number
  pageSize: number
  total: number
}

interface StudentRecord {
  id: number
  name: string
  phone: string
  guardianName: string
  guardianPhone: string
  source: string
  status: string
}

interface CourseRecord {
  id: number
  name: string
  categoryCode: string
  deliveryMode: string
  billingMode: string
  totalLessons: number
  listPrice: string
  extensionTemplateCode: string
  status: string
}

interface ClassRecord {
  id: number
  name: string
  courseProductId: number
  courseProductName: string
  campusId: number
  campusName: string
  headTeacherId: number
  capacity: number
  startDate: string
  endDate: string
  status: string
}

interface LessonRecord {
  id: number
  classGroupId: number
  classGroupName: string
  courseProductName: string
  campusName: string
  teacherId: number
  classroomId: number
  onlineRoomUrl: string
  plannedStartAt: string
  plannedEndAt: string
  status: string
}

interface CampusRecord {
  id: number
  name: string
  code: string
  address: string
  status: string
}

interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

const studentLoading = ref(false)
const courseLoading = ref(false)
const classLoading = ref(false)
const lessonLoading = ref(false)
const enrollmentLoading = ref(false)

const students = ref<StudentRecord[]>([])
const courses = ref<CourseRecord[]>([])
const classGroups = ref<ClassRecord[]>([])
const lessons = ref<LessonRecord[]>([])
const campuses = ref<CampusRecord[]>([])
const enrollments = ref<any[]>([])

const studentPager = ref<Pager>({ page: 1, pageSize: 10, total: 0 })
const coursePager = ref<Pager>({ page: 1, pageSize: 10, total: 0 })
const classPager = ref<Pager>({ page: 1, pageSize: 10, total: 0 })
const lessonPager = ref<Pager>({ page: 1, pageSize: 10, total: 0 })

const studentFilters = reactive<{ keyword: string; status?: string }>({ keyword: '', status: undefined })
const courseFilters = reactive<{ keyword: string; status?: string }>({ keyword: '', status: undefined })
const classFilters = reactive<{ keyword: string; status?: string }>({ keyword: '', status: undefined })
const lessonFilters = reactive<{ status?: string }>({ status: undefined })

const studentModalOpen = ref(false)
const courseModalOpen = ref(false)
const classModalOpen = ref(false)
const lessonModalOpen = ref(false)
const enrollmentDrawerOpen = ref(false)

const studentSaving = ref(false)
const courseSaving = ref(false)
const classSaving = ref(false)
const lessonSaving = ref(false)
const enrollmentSaving = ref(false)

const editingStudentId = ref<number>()
const editingCourseId = ref<number>()
const editingClassId = ref<number>()
const editingLessonId = ref<number>()
const selectedClass = ref<ClassRecord>()
const selectedStudentIds = ref<number[]>([])
const enrollmentRemark = ref('')

const studentFormRef = ref()
const courseFormRef = ref()
const classFormRef = ref()
const lessonFormRef = ref()

const studentForm = reactive({
  name: '',
  phone: '',
  guardianName: '',
  guardianPhone: '',
  source: 'WALK_IN',
  status: 'ACTIVE'
})

const courseForm = reactive({
  name: '',
  categoryCode: 'ACADEMIC',
  deliveryMode: 'OFFLINE',
  billingMode: 'LESSON',
  totalLessons: 0,
  listPrice: 0,
  extensionTemplateCode: '',
  status: 'ON_SALE'
})

const classForm = reactive<{
  name: string
  courseProductId?: number
  campusId?: number
  headTeacherId?: number
  capacity: number
  startDate: Dayjs | null
  endDate: Dayjs | null
  status: string
}>({
  name: '',
  courseProductId: undefined,
  campusId: undefined,
  headTeacherId: undefined,
  capacity: 30,
  startDate: null,
  endDate: null,
  status: 'ACTIVE'
})

const lessonForm = reactive<{
  classGroupId?: number
  timeRange: [Dayjs, Dayjs] | null
  teacherId?: number
  classroomId?: number
  onlineRoomUrl: string
  status: string
}>({
  classGroupId: undefined,
  timeRange: null,
  teacherId: undefined,
  classroomId: undefined,
  onlineRoomUrl: '',
  status: 'SCHEDULED'
})

const studentColumns = [
  { title: '学员姓名', dataIndex: 'name', key: 'name', width: 140 },
  { title: '联系方式', key: 'contact' },
  { title: '来源', dataIndex: 'source', key: 'source', width: 130 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

const courseColumns = [
  { title: '课程名称', dataIndex: 'name', key: 'name' },
  { title: '业务类型', dataIndex: 'categoryCode', key: 'categoryCode', width: 120 },
  { title: '授课方式', dataIndex: 'deliveryMode', key: 'deliveryMode', width: 120 },
  { title: '计费方式', dataIndex: 'billingMode', key: 'billingMode', width: 120 },
  { title: '课时', dataIndex: 'totalLessons', key: 'totalLessons', width: 80 },
  { title: '标价', dataIndex: 'listPrice', key: 'listPrice', width: 110 },
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
  { title: '课程', dataIndex: 'courseProductName', key: 'courseProductName' },
  { title: '上课时间', key: 'time', width: 260 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

const enrollmentColumns = [
  { title: '学员', dataIndex: 'studentName', key: 'studentName' },
  { title: '状态', dataIndex: 'enrollStatus', key: 'enrollStatus', width: 100 },
  { title: '入班日期', dataIndex: 'enrollDate', key: 'enrollDate', width: 120 }
]

const studentRules = {
  name: [{ required: true, message: '请输入学员姓名' }],
  status: [{ required: true, message: '请选择状态' }]
}

const courseRules = {
  name: [{ required: true, message: '请输入课程名称' }],
  categoryCode: [{ required: true, message: '请输入业务类型' }],
  deliveryMode: [{ required: true, message: '请选择授课方式' }],
  billingMode: [{ required: true, message: '请选择计费方式' }],
  status: [{ required: true, message: '请选择状态' }]
}

const classRules = {
  name: [{ required: true, message: '请输入班级名称' }],
  courseProductId: [{ required: true, message: '请选择课程产品' }],
  status: [{ required: true, message: '请选择状态' }]
}

const lessonRules = {
  classGroupId: [{ required: true, message: '请选择班级' }],
  timeRange: [{ required: true, message: '请选择上课时间' }],
  status: [{ required: true, message: '请选择状态' }]
}

const studentPagination = computed(() => buildPagination(studentPager.value))
const coursePagination = computed(() => buildPagination(coursePager.value))
const classPagination = computed(() => buildPagination(classPager.value))
const lessonPagination = computed(() => buildPagination(lessonPager.value))
const courseOptions = computed(() => courses.value.filter(course => course.status !== 'DISABLED'))
const classOptions = computed(() => classGroups.value.filter(group => group.status !== 'COMPLETED'))
const studentOptions = computed(() => students.value.filter(student => student.status === 'ACTIVE'))

onMounted(() => {
  reloadAll()
})

async function reloadAll() {
  await Promise.all([
    loadStudents(),
    loadCourses(),
    loadClasses(),
    loadLessons(),
    loadCampuses()
  ])
}

async function loadStudents() {
  studentLoading.value = true
  try {
    const res = await apiGet<PageResult<StudentRecord>>('/courses/students', {
      page: studentPager.value.page,
      pageSize: studentPager.value.pageSize,
      keyword: studentFilters.keyword || undefined,
      status: studentFilters.status
    })
    students.value = res.records || []
    studentPager.value.total = res.total || 0
  } catch (error) {
    message.error('加载学员失败')
  } finally {
    studentLoading.value = false
  }
}

async function loadCourses() {
  courseLoading.value = true
  try {
    const res = await apiGet<PageResult<CourseRecord>>('/courses/products', {
      page: coursePager.value.page,
      pageSize: coursePager.value.pageSize,
      keyword: courseFilters.keyword || undefined,
      status: courseFilters.status
    })
    courses.value = res.records || []
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
    const res = await apiGet<PageResult<ClassRecord>>('/courses/classes', {
      page: classPager.value.page,
      pageSize: classPager.value.pageSize,
      keyword: classFilters.keyword || undefined,
      status: classFilters.status
    })
    classGroups.value = res.records || []
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
    const res = await apiGet<PageResult<LessonRecord>>('/courses/lessons', {
      page: lessonPager.value.page,
      pageSize: lessonPager.value.pageSize,
      status: lessonFilters.status
    })
    lessons.value = res.records || []
    lessonPager.value.total = res.total || 0
  } catch (error) {
    message.error('加载课次失败')
  } finally {
    lessonLoading.value = false
  }
}

async function loadCampuses() {
  try {
    campuses.value = await apiGet<CampusRecord[]>('/courses/campuses')
  } catch (error) {
    campuses.value = []
  }
}

async function loadEnrollments(classGroupId: number) {
  enrollmentLoading.value = true
  try {
    const res = await apiGet<PageResult<any>>('/courses/enrollments', {
      classGroupId,
      page: 1,
      pageSize: 100
    })
    enrollments.value = res.records || []
    selectedStudentIds.value = enrollments.value.map(item => item.studentId)
  } catch (error) {
    message.error('加载班级学员失败')
  } finally {
    enrollmentLoading.value = false
  }
}

function handleStudentTableChange(pagination: any) {
  studentPager.value.page = pagination.current
  studentPager.value.pageSize = pagination.pageSize
  loadStudents()
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

function applyStudentFilters() {
  studentPager.value.page = 1
  loadStudents()
}

function applyCourseFilters() {
  coursePager.value.page = 1
  loadCourses()
}

function applyClassFilters() {
  classPager.value.page = 1
  loadClasses()
}

function applyLessonFilters() {
  lessonPager.value.page = 1
  loadLessons()
}

function openCreateStudent() {
  editingStudentId.value = undefined
  Object.assign(studentForm, {
    name: '',
    phone: '',
    guardianName: '',
    guardianPhone: '',
    source: 'WALK_IN',
    status: 'ACTIVE'
  })
  studentModalOpen.value = true
  nextTick(() => studentFormRef.value?.clearValidate?.())
}

function openEditStudent(record: StudentRecord) {
  editingStudentId.value = record.id
  Object.assign(studentForm, {
    name: record.name,
    phone: record.phone,
    guardianName: record.guardianName,
    guardianPhone: record.guardianPhone,
    source: record.source || 'WALK_IN',
    status: record.status || 'ACTIVE'
  })
  studentModalOpen.value = true
  nextTick(() => studentFormRef.value?.clearValidate?.())
}

function openCreateCourse() {
  editingCourseId.value = undefined
  Object.assign(courseForm, {
    name: '',
    categoryCode: 'ACADEMIC',
    deliveryMode: 'OFFLINE',
    billingMode: 'LESSON',
    totalLessons: 0,
    listPrice: 0,
    extensionTemplateCode: '',
    status: 'ON_SALE'
  })
  courseModalOpen.value = true
  nextTick(() => courseFormRef.value?.clearValidate?.())
}

function openEditCourse(record: CourseRecord) {
  editingCourseId.value = record.id
  Object.assign(courseForm, {
    name: record.name,
    categoryCode: record.categoryCode,
    deliveryMode: record.deliveryMode,
    billingMode: record.billingMode,
    totalLessons: Number(record.totalLessons ?? 0),
    listPrice: Number(record.listPrice ?? 0),
    extensionTemplateCode: record.extensionTemplateCode,
    status: record.status
  })
  courseModalOpen.value = true
  nextTick(() => courseFormRef.value?.clearValidate?.())
}

function openCreateClass() {
  editingClassId.value = undefined
  Object.assign(classForm, {
    name: '',
    courseProductId: courseOptions.value[0]?.id,
    campusId: campuses.value[0]?.id,
    headTeacherId: undefined,
    capacity: 30,
    startDate: null,
    endDate: null,
    status: 'ACTIVE'
  })
  classModalOpen.value = true
  nextTick(() => classFormRef.value?.clearValidate?.())
}

function openEditClass(record: ClassRecord) {
  editingClassId.value = record.id
  Object.assign(classForm, {
    name: record.name,
    courseProductId: record.courseProductId,
    campusId: record.campusId || undefined,
    headTeacherId: record.headTeacherId || undefined,
    capacity: Number(record.capacity ?? 0),
    startDate: record.startDate ? dayjs(record.startDate) : null,
    endDate: record.endDate ? dayjs(record.endDate) : null,
    status: record.status
  })
  classModalOpen.value = true
  nextTick(() => classFormRef.value?.clearValidate?.())
}

function openCreateLesson() {
  editingLessonId.value = undefined
  Object.assign(lessonForm, {
    classGroupId: classOptions.value[0]?.id,
    timeRange: [dayjs().add(1, 'day').hour(9).minute(0).second(0), dayjs().add(1, 'day').hour(10).minute(30).second(0)],
    teacherId: undefined,
    classroomId: undefined,
    onlineRoomUrl: '',
    status: 'SCHEDULED'
  })
  lessonModalOpen.value = true
  nextTick(() => lessonFormRef.value?.clearValidate?.())
}

function openEditLesson(record: LessonRecord) {
  editingLessonId.value = record.id
  Object.assign(lessonForm, {
    classGroupId: record.classGroupId,
    timeRange: record.plannedStartAt && record.plannedEndAt
      ? [dayjs(record.plannedStartAt), dayjs(record.plannedEndAt)]
      : null,
    teacherId: record.teacherId || undefined,
    classroomId: record.classroomId || undefined,
    onlineRoomUrl: record.onlineRoomUrl,
    status: record.status
  })
  lessonModalOpen.value = true
  nextTick(() => lessonFormRef.value?.clearValidate?.())
}

async function submitStudent() {
  await studentFormRef.value?.validate?.()
  studentSaving.value = true
  try {
    const payload = { ...studentForm }
    if (editingStudentId.value) {
      await apiPut(`/courses/students/${editingStudentId.value}`, payload)
      message.success('学员已更新')
    } else {
      await apiPost('/courses/students', payload)
      message.success('学员已创建')
    }
    studentModalOpen.value = false
    await loadStudents()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存学员失败')
  } finally {
    studentSaving.value = false
  }
}

async function submitCourse() {
  await courseFormRef.value?.validate?.()
  courseSaving.value = true
  try {
    const payload = { ...courseForm }
    if (editingCourseId.value) {
      await apiPut(`/courses/products/${editingCourseId.value}`, payload)
      message.success('课程已更新')
    } else {
      await apiPost('/courses/products', payload)
      message.success('课程已创建')
    }
    courseModalOpen.value = false
    await loadCourses()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存课程失败')
  } finally {
    courseSaving.value = false
  }
}

async function submitClass() {
  await classFormRef.value?.validate?.()
  classSaving.value = true
  try {
    const payload = {
      name: classForm.name,
      courseProductId: classForm.courseProductId,
      campusId: classForm.campusId,
      headTeacherId: classForm.headTeacherId,
      capacity: classForm.capacity,
      startDate: classForm.startDate ? classForm.startDate.format('YYYY-MM-DD') : null,
      endDate: classForm.endDate ? classForm.endDate.format('YYYY-MM-DD') : null,
      status: classForm.status
    }
    if (editingClassId.value) {
      await apiPut(`/courses/classes/${editingClassId.value}`, payload)
      message.success('班级已更新')
    } else {
      await apiPost('/courses/classes', payload)
      message.success('班级已创建')
    }
    classModalOpen.value = false
    await loadClasses()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存班级失败')
  } finally {
    classSaving.value = false
  }
}

async function submitLesson() {
  await lessonFormRef.value?.validate?.()
  if (!lessonForm.timeRange) return
  lessonSaving.value = true
  try {
    const payload = {
      classGroupId: lessonForm.classGroupId,
      teacherId: lessonForm.teacherId,
      classroomId: lessonForm.classroomId,
      onlineRoomUrl: lessonForm.onlineRoomUrl,
      plannedStartAt: lessonForm.timeRange[0].format('YYYY-MM-DDTHH:mm:ss'),
      plannedEndAt: lessonForm.timeRange[1].format('YYYY-MM-DDTHH:mm:ss'),
      status: lessonForm.status
    }
    if (editingLessonId.value) {
      await apiPut(`/courses/lessons/${editingLessonId.value}`, payload)
      message.success('课次已更新')
    } else {
      await apiPost('/courses/lessons', payload)
      message.success('课次已创建')
    }
    lessonModalOpen.value = false
    await loadLessons()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存课次失败')
  } finally {
    lessonSaving.value = false
  }
}

async function disableCourse(id: number) {
  try {
    await apiPut(`/courses/products/${id}/disable`)
    message.success('课程已下架')
    await loadCourses()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '下架失败')
  }
}

async function openEnrollmentDrawer(record: ClassRecord) {
  selectedClass.value = record
  selectedStudentIds.value = []
  enrollmentRemark.value = ''
  enrollmentDrawerOpen.value = true
  await loadEnrollments(record.id)
}

async function submitEnrollments() {
  if (!selectedClass.value) return
  enrollmentSaving.value = true
  try {
    await apiPost('/courses/enrollments', {
      classGroupId: selectedClass.value.id,
      studentIds: selectedStudentIds.value,
      remark: enrollmentRemark.value
    })
    message.success('班级学员已保存')
    await loadEnrollments(selectedClass.value.id)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存班级学员失败')
  } finally {
    enrollmentSaving.value = false
  }
}

function buildPagination(pager: Pager) {
  return {
    current: pager.page,
    pageSize: pager.pageSize,
    total: pager.total,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`
  }
}

function getStudentStatusColor(status: string) {
  return status === 'ACTIVE' ? 'green' : 'default'
}

function getStudentStatusText(status: string) {
  const texts: Record<string, string> = {
    ACTIVE: '在读',
    INACTIVE: '停课'
  }
  return texts[status] || status
}

function getCourseStatusColor(status: string) {
  return status === 'ON_SALE' ? 'green' : 'default'
}

function getCourseStatusText(status: string) {
  const texts: Record<string, string> = {
    ON_SALE: '上架',
    DISABLED: '下架'
  }
  return texts[status] || status
}

function getClassStatusColor(status: string) {
  const colors: Record<string, string> = {
    ACTIVE: 'green',
    INACTIVE: 'default',
    COMPLETED: 'blue'
  }
  return colors[status] || 'default'
}

function getClassStatusText(status: string) {
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

function formatDateTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped>
.course-page :deep(.ant-card-extra) {
  max-width: 100%;
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

@media (max-width: 768px) {
  .course-page :deep(.ant-card-head) {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    padding-block: 12px;
  }

  .course-page :deep(.ant-card-extra) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
