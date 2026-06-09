<template>
  <h1 class="page-title">课程中心</h1>
  <div class="stack">
    <div class="panel">
      <div class="section-head">
        <strong>课程产品</strong>
        <el-button type="primary" @click="createDemoCourse">新增示例课程</el-button>
      </div>
      <el-table :data="courses" style="width: 100%">
        <el-table-column prop="name" label="课程名称" />
        <el-table-column prop="categoryCode" label="业务类型" width="120" />
        <el-table-column prop="deliveryMode" label="授课方式" width="120" />
        <el-table-column prop="billingMode" label="计费方式" width="120" />
        <el-table-column prop="totalLessons" label="课时" width="80" />
        <el-table-column prop="listPrice" label="标价" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 'DISABLED'" @click="disableCourse(row.id)">
              下架
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination v-model:current-page="coursePager.page" v-model:page-size="coursePager.pageSize" :total="coursePager.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" background @current-change="loadCourses" @size-change="loadCourses" />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>班级</strong>
        <el-button type="primary" @click="openCreateClass">新增班级</el-button>
      </div>
      <el-table :data="classGroups" style="width: 100%">
        <el-table-column prop="name" label="班级名称" />
        <el-table-column prop="courseProductName" label="课程产品" />
        <el-table-column prop="campusName" label="校区" width="120" />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditClass(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination v-model:current-page="classPager.page" v-model:page-size="classPager.pageSize" :total="classPager.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" background @current-change="loadClasses" @size-change="loadClasses" />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>课次</strong>
        <el-button type="primary" @click="openCreateLesson">新增课次</el-button>
      </div>
      <el-table :data="lessons" style="width: 100%">
        <el-table-column prop="classGroupName" label="班级" />
        <el-table-column prop="courseProductName" label="课程" />
        <el-table-column prop="campusName" label="校区" width="120" />
        <el-table-column prop="teacherId" label="老师ID" width="100" />
        <el-table-column prop="plannedStartAt" label="上课时间" width="180" />
        <el-table-column prop="plannedEndAt" label="下课时间" width="180" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditLesson(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination v-model:current-page="lessonPager.page" v-model:page-size="lessonPager.pageSize" :total="lessonPager.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" background @current-change="loadLessons" @size-change="loadLessons" />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>学员</strong>
        <el-button type="primary" @click="openCreateStudent">新增学员</el-button>
      </div>
      <el-table :data="students" style="width: 100%">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="guardianName" label="家长" width="110" />
        <el-table-column prop="guardianPhone" label="家长电话" width="120" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditStudent(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-row">
        <el-pagination v-model:current-page="studentPager.page" v-model:page-size="studentPager.pageSize" :total="studentPager.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" background @current-change="loadStudents" @size-change="loadStudents" />
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>考勤</strong>
        <div class="attendance-head">
          <el-select v-model="attendanceLessonId" filterable style="width: 320px" @change="loadAttendance">
            <el-option
              v-for="lesson in lessonOptions"
              :key="lesson.id"
              :label="`${lesson.classGroupName} ${lesson.plannedStartAt}`"
              :value="lesson.id"
            />
          </el-select>
          <el-button @click="loadAttendance">刷新</el-button>
        </div>
      </div>
      <el-table :data="attendanceRows" style="width: 100%">
        <el-table-column prop="studentName" label="学员" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-select v-model="row.status" style="width: 100%">
              <el-option label="未签到" value="PENDING" />
              <el-option label="出勤" value="PRESENT" />
              <el-option label="迟到" value="LATE" />
              <el-option label="缺勤" value="ABSENT" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="备注">
          <template #default="{ row }">
            <el-input v-model="row.remark" />
          </template>
        </el-table-column>
        <el-table-column prop="checkedAt" label="更新时间" width="180" />
      </el-table>
      <div class="attendance-actions">
        <el-button type="primary" @click="saveAttendance">保存考勤</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="section-head">
        <strong>报名</strong>
        <div class="attendance-head">
          <el-select v-model="enrollmentClassGroupId" filterable style="width: 320px" @change="loadEnrollments">
            <el-option
              v-for="item in classOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-button @click="loadEnrollments">刷新</el-button>
        </div>
      </div>
      <div class="enroll-panel">
        <el-select v-model="enrollStudentIds" multiple filterable style="width: 100%" placeholder="选择学员报名">
          <el-option v-for="student in studentOptions" :key="student.id" :label="student.name" :value="student.id" />
        </el-select>
        <el-input v-model="enrollRemark" placeholder="报名备注" style="margin-top: 8px" />
        <div class="attendance-actions">
          <el-button type="primary" @click="saveEnrollments">保存报名</el-button>
        </div>
      </div>
      <el-table :data="enrollments" style="width: 100%; margin-top: 12px">
        <el-table-column prop="studentName" label="学员" />
        <el-table-column prop="classGroupName" label="班级" />
        <el-table-column prop="enrollStatus" label="状态" width="100" />
        <el-table-column prop="enrollDate" label="报名日期" width="120" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
      <div class="pager-row">
        <el-pagination v-model:current-page="enrollmentPager.page" v-model:page-size="enrollmentPager.pageSize" :total="enrollmentPager.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" background @current-change="loadEnrollments" @size-change="loadEnrollments" />
      </div>
    </div>
  </div>

  <el-dialog v-model="classDialogVisible" :title="classForm.id ? '编辑班级' : '新增班级'" width="640px">
    <el-form label-width="100px">
      <el-form-item label="课程产品">
        <el-select v-model="classForm.courseProductId" style="width: 100%">
          <el-option v-for="course in courseOptions" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="校区">
        <el-select v-model="classForm.campusId" clearable style="width: 100%">
          <el-option v-for="campus in campuses" :key="campus.id" :label="campus.name" :value="campus.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="班级名称">
        <el-input v-model="classForm.name" />
      </el-form-item>
      <el-form-item label="主讲老师ID">
        <el-input-number v-model="classForm.headTeacherId" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="容量">
        <el-input-number v-model="classForm.capacity" :min="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="classForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="classForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="classForm.status" style="width: 100%">
          <el-option label="开班中" value="OPEN" />
          <el-option label="已结课" value="CLOSED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="classDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveClass">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="lessonDialogVisible" :title="lessonForm.id ? '编辑课次' : '新增课次'" width="660px">
    <el-form label-width="100px">
      <el-form-item label="班级">
        <el-select v-model="lessonForm.classGroupId" style="width: 100%">
          <el-option v-for="item in classOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="老师ID">
        <el-input-number v-model="lessonForm.teacherId" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="教室ID">
        <el-input-number v-model="lessonForm.classroomId" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="线上教室">
        <el-input v-model="lessonForm.onlineRoomUrl" />
      </el-form-item>
      <el-form-item label="上课时间">
        <el-date-picker
          v-model="lessonForm.plannedStartAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="下课时间">
        <el-date-picker
          v-model="lessonForm.plannedEndAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="lessonForm.status" style="width: 100%">
          <el-option label="已排课" value="SCHEDULED" />
          <el-option label="上课中" value="IN_PROGRESS" />
          <el-option label="已结课" value="FINISHED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="lessonDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveLesson">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="studentDialogVisible" :title="studentForm.id ? '编辑学员' : '新增学员'" width="560px">
    <el-form label-width="100px">
      <el-form-item label="姓名">
        <el-input v-model="studentForm.name" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="studentForm.phone" />
      </el-form-item>
      <el-form-item label="家长姓名">
        <el-input v-model="studentForm.guardianName" />
      </el-form-item>
      <el-form-item label="家长电话">
        <el-input v-model="studentForm.guardianPhone" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="studentForm.source" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="studentForm.status" style="width: 100%">
          <el-option label="在读" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="studentDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveStudent">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost, apiPut } from '../api/http'

interface CourseProduct {
  id: number
  name: string
  categoryCode: string
  deliveryMode: string
  billingMode: string
  totalLessons: number
  listPrice: string
  status: string
}

interface CampusItem {
  id: number
  name: string
}

interface ClassGroupItem {
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

interface LessonItem {
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

interface StudentItem {
  id: number
  name: string
  phone: string
  guardianName: string
  guardianPhone: string
  source: string
  status: string
}

interface AttendanceRow {
  studentId: number
  studentName: string
  phone: string
  status: string
  remark: string
  checkedAt: string
}

interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  summary?: Record<string, unknown>
}

const courses = ref<CourseProduct[]>([])
const courseOptions = ref<CourseProduct[]>([])
const campuses = ref<CampusItem[]>([])
const classGroups = ref<ClassGroupItem[]>([])
const classOptions = ref<ClassGroupItem[]>([])
const lessons = ref<LessonItem[]>([])
const lessonOptions = ref<LessonItem[]>([])
const students = ref<StudentItem[]>([])
const studentOptions = ref<StudentItem[]>([])
const attendanceRows = ref<AttendanceRow[]>([])
const enrollments = ref<any[]>([])
const classDialogVisible = ref(false)
const lessonDialogVisible = ref(false)
const studentDialogVisible = ref(false)
const attendanceLessonId = ref(0)
const enrollmentClassGroupId = ref(0)
const enrollStudentIds = ref<number[]>([])
const enrollRemark = ref('演示报名')
const coursePager = reactive({ page: 1, pageSize: 10, total: 0 })
const classPager = reactive({ page: 1, pageSize: 10, total: 0 })
const lessonPager = reactive({ page: 1, pageSize: 10, total: 0 })
const studentPager = reactive({ page: 1, pageSize: 10, total: 0 })
const enrollmentPager = reactive({ page: 1, pageSize: 10, total: 0 })

const classForm = reactive({
  id: 0,
  courseProductId: 0,
  campusId: 0,
  name: '',
  headTeacherId: 0,
  capacity: 24,
  startDate: '',
  endDate: '',
  status: 'OPEN'
})

const lessonForm = reactive({
  id: 0,
  classGroupId: 0,
  teacherId: 0,
  classroomId: 0,
  onlineRoomUrl: '',
  plannedStartAt: '',
  plannedEndAt: '',
  status: 'SCHEDULED'
})

const studentForm = reactive({
  id: 0,
  name: '',
  phone: '',
  guardianName: '',
  guardianPhone: '',
  source: '',
  status: 'ACTIVE'
})

async function loadCourses() {
  const result = await apiGet<PageResult<CourseProduct>>(`/courses/products?page=${coursePager.page}&pageSize=${coursePager.pageSize}`)
  courses.value = result.records
  coursePager.total = result.total
}

async function loadCampuses() {
  campuses.value = await apiGet<CampusItem[]>('/courses/campuses')
}

async function loadClasses() {
  const result = await apiGet<PageResult<ClassGroupItem>>(`/courses/classes?page=${classPager.page}&pageSize=${classPager.pageSize}`)
  classGroups.value = result.records
  classPager.total = result.total
}

async function loadLessons() {
  const result = await apiGet<PageResult<LessonItem>>(`/courses/lessons?page=${lessonPager.page}&pageSize=${lessonPager.pageSize}`)
  lessons.value = result.records
  lessonPager.total = result.total
  if (!attendanceLessonId.value && lessons.value.length > 0) {
    attendanceLessonId.value = lessons.value[0].id
  }
}

async function loadStudents() {
  const result = await apiGet<PageResult<StudentItem>>(`/courses/students?page=${studentPager.page}&pageSize=${studentPager.pageSize}`)
  students.value = result.records
  studentPager.total = result.total
}

async function loadEnrollments() {
  const params = new URLSearchParams({
    page: String(enrollmentPager.page),
    pageSize: String(enrollmentPager.pageSize)
  })
  if (!enrollmentClassGroupId.value) {
    const result = await apiGet<PageResult<any>>(`/courses/enrollments?${params.toString()}`)
    enrollments.value = result.records
    enrollmentPager.total = result.total
    return
  }
  params.set('classGroupId', String(enrollmentClassGroupId.value))
  const result = await apiGet<PageResult<any>>(`/courses/enrollments?${params.toString()}`)
  enrollments.value = result.records
  enrollmentPager.total = result.total
}

async function loadCourseOptions() {
  const result = await apiGet<PageResult<CourseProduct>>('/courses/products?page=1&pageSize=100')
  courseOptions.value = result.records
}

async function loadClassOptions() {
  const result = await apiGet<PageResult<ClassGroupItem>>('/courses/classes?page=1&pageSize=100')
  classOptions.value = result.records
}

async function loadLessonOptions() {
  const result = await apiGet<PageResult<LessonItem>>('/courses/lessons?page=1&pageSize=100')
  lessonOptions.value = result.records
}

async function loadStudentOptions() {
  const result = await apiGet<PageResult<StudentItem>>('/courses/students?page=1&pageSize=100')
  studentOptions.value = result.records
}

async function createDemoCourse() {
  await apiPost<number>('/courses/products', {
    name: `编程体验课 ${new Date().toLocaleTimeString()}`,
    categoryCode: 'ONLINE',
    deliveryMode: 'LIVE',
    billingMode: 'PACKAGE',
    totalLessons: 8,
    listPrice: 999,
    extensionTemplateCode: 'online_course',
    status: 'ON_SALE'
  })
  ElMessage.success('课程已创建')
  await Promise.all([loadCourses(), loadCourseOptions()])
}

async function disableCourse(id: number) {
  await apiPut<void>(`/courses/products/${id}/disable`)
  ElMessage.success('课程已下架')
  await Promise.all([loadCourses(), loadCourseOptions()])
}

function openCreateClass() {
  resetClassForm()
  classDialogVisible.value = true
}

function openEditClass(item: ClassGroupItem) {
  classForm.id = item.id
  classForm.courseProductId = item.courseProductId
  classForm.campusId = item.campusId || 0
  classForm.name = item.name
  classForm.headTeacherId = item.headTeacherId || 0
  classForm.capacity = item.capacity
  classForm.startDate = item.startDate
  classForm.endDate = item.endDate
  classForm.status = item.status
  classDialogVisible.value = true
}

async function saveClass() {
  const payload = {
    courseProductId: classForm.courseProductId,
    campusId: classForm.campusId || null,
    name: classForm.name,
    headTeacherId: classForm.headTeacherId || null,
    capacity: classForm.capacity,
    startDate: classForm.startDate || null,
    endDate: classForm.endDate || null,
    status: classForm.status
  }
  if (classForm.id) {
    await apiPut<void>(`/courses/classes/${classForm.id}`, payload)
  } else {
    await apiPost<number>('/courses/classes', payload)
  }
  classDialogVisible.value = false
  ElMessage.success('班级已保存')
  await Promise.all([loadClasses(), loadClassOptions(), loadLessons(), loadLessonOptions()])
}

function openCreateLesson() {
  resetLessonForm()
  lessonDialogVisible.value = true
}

function openEditLesson(item: LessonItem) {
  lessonForm.id = item.id
  lessonForm.classGroupId = item.classGroupId
  lessonForm.teacherId = item.teacherId || 0
  lessonForm.classroomId = item.classroomId || 0
  lessonForm.onlineRoomUrl = item.onlineRoomUrl
  lessonForm.plannedStartAt = normalizeDateTime(item.plannedStartAt)
  lessonForm.plannedEndAt = normalizeDateTime(item.plannedEndAt)
  lessonForm.status = item.status
  lessonDialogVisible.value = true
}

async function saveLesson() {
  const payload = {
    classGroupId: lessonForm.classGroupId,
    teacherId: lessonForm.teacherId || null,
    classroomId: lessonForm.classroomId || null,
    onlineRoomUrl: lessonForm.onlineRoomUrl,
    plannedStartAt: lessonForm.plannedStartAt,
    plannedEndAt: lessonForm.plannedEndAt,
    status: lessonForm.status
  }
  if (lessonForm.id) {
    await apiPut<void>(`/courses/lessons/${lessonForm.id}`, payload)
  } else {
    await apiPost<number>('/courses/lessons', payload)
  }
  lessonDialogVisible.value = false
  ElMessage.success('课次已保存')
  await Promise.all([loadLessons(), loadLessonOptions()])
  if (attendanceLessonId.value === 0 && lessons.value.length > 0) {
    attendanceLessonId.value = lessons.value[0].id
  }
  await loadAttendance()
}

function resetClassForm() {
  classForm.id = 0
  classForm.courseProductId = courseOptions.value[0]?.id ?? courses.value[0]?.id ?? 0
  classForm.campusId = campuses.value[0]?.id ?? 0
  classForm.name = ''
  classForm.headTeacherId = 0
  classForm.capacity = 24
  classForm.startDate = ''
  classForm.endDate = ''
  classForm.status = 'OPEN'
}

function resetLessonForm() {
  const start = new Date()
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  const end = new Date(start)
  end.setHours(end.getHours() + 2)
  lessonForm.id = 0
  lessonForm.classGroupId = classOptions.value[0]?.id ?? classGroups.value[0]?.id ?? 0
  lessonForm.teacherId = 2001
  lessonForm.classroomId = 0
  lessonForm.onlineRoomUrl = ''
  lessonForm.plannedStartAt = formatLocalDateTime(start)
  lessonForm.plannedEndAt = formatLocalDateTime(end)
  lessonForm.status = 'SCHEDULED'
}

function openCreateStudent() {
  resetStudentForm()
  studentDialogVisible.value = true
}

function openEditStudent(item: StudentItem) {
  studentForm.id = item.id
  studentForm.name = item.name
  studentForm.phone = item.phone
  studentForm.guardianName = item.guardianName
  studentForm.guardianPhone = item.guardianPhone
  studentForm.source = item.source
  studentForm.status = item.status
  studentDialogVisible.value = true
}

async function saveStudent() {
  const payload = {
    name: studentForm.name,
    phone: studentForm.phone,
    guardianName: studentForm.guardianName,
    guardianPhone: studentForm.guardianPhone,
    source: studentForm.source,
    status: studentForm.status
  }
  if (studentForm.id) {
    await apiPut<void>(`/courses/students/${studentForm.id}`, payload)
  } else {
    await apiPost<number>('/courses/students', payload)
  }
  studentDialogVisible.value = false
  ElMessage.success('学员已保存')
  await Promise.all([loadStudents(), loadStudentOptions()])
  await loadAttendance()
}

function resetStudentForm() {
  studentForm.id = 0
  studentForm.name = ''
  studentForm.phone = ''
  studentForm.guardianName = ''
  studentForm.guardianPhone = ''
  studentForm.source = '试听'
  studentForm.status = 'ACTIVE'
}

async function loadAttendance() {
  if (!attendanceLessonId.value) {
    attendanceRows.value = []
    return
  }
  attendanceRows.value = await apiGet<AttendanceRow[]>(`/courses/attendance?lessonId=${attendanceLessonId.value}`)
}

async function saveAttendance() {
  if (!attendanceLessonId.value) {
    ElMessage.warning('请先选择课次')
    return
  }
  await apiPut<void>('/courses/attendance', {
    lessonId: attendanceLessonId.value,
    records: attendanceRows.value.map((row) => ({
      studentId: row.studentId,
      status: row.status,
      remark: row.remark
    }))
  })
  ElMessage.success('考勤已保存')
  await loadAttendance()
}

async function saveEnrollments() {
  if (!enrollmentClassGroupId.value) {
    ElMessage.warning('请先选择班级')
    return
  }
  if (!enrollStudentIds.value.length) {
    ElMessage.warning('请先选择学员')
    return
  }
  await apiPost<void>('/courses/enrollments', {
    classGroupId: enrollmentClassGroupId.value,
    studentIds: enrollStudentIds.value,
    remark: enrollRemark.value
  })
  ElMessage.success('报名已保存')
  enrollStudentIds.value = []
  await Promise.all([loadEnrollments(), loadStudentOptions()])
}

function formatLocalDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function normalizeDateTime(value: string) {
  return value ? value.replace(' ', 'T') : ''
}

onMounted(async () => {
  await Promise.all([loadCourses(), loadCampuses(), loadClasses(), loadStudents(), loadCourseOptions(), loadClassOptions(), loadStudentOptions()])
  await Promise.all([loadLessons(), loadLessonOptions()])
  enrollmentClassGroupId.value = classOptions.value[0]?.id ?? classGroups.value[0]?.id ?? 0
  await loadEnrollments()
  await loadAttendance()
})
</script>

<style scoped>
.stack {
  display: grid;
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.attendance-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.attendance-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.enroll-panel {
  display: grid;
  gap: 8px;
}
</style>
