import { readdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const viewsDir = resolve(__dirname, '../src/views')
const courseView = readFileSync(resolve(__dirname, '../src/views/CourseView.vue'), 'utf8')
const views = readdirSync(viewsDir)
  .filter((file) => file.endsWith('.vue'))
  .map((file) => [file, readFileSync(resolve(viewsDir, file), 'utf8')])

const forbidden = [
  '新增示例课程',
  '功能开发中',
  'createDemoCourse'
]

const required = [
  "'/courses/students'",
  "'/courses/products'",
  "'/courses/classes'",
  "'/courses/lessons'",
  'apiPost',
  'apiPut',
  'openCreateStudent',
  'openEditStudent',
  'openCreateCourse',
  'openEditCourse',
  'openCreateClass',
  'openEditClass',
  'openCreateLesson',
  'openEditLesson'
]

const failures = [
  ...views
    .filter(([, content]) => content.includes('功能开发中'))
    .map(([file]) => `${file} still contains placeholder "功能开发中"`),
  ...forbidden
    .filter((needle) => courseView.includes(needle))
    .map((needle) => `CourseView still contains placeholder "${needle}"`),
  ...required
    .filter((needle) => !courseView.includes(needle))
    .map((needle) => `CourseView is missing required contract "${needle}"`)
]

if (failures.length > 0) {
  console.error(failures.join('\n'))
  process.exit(1)
}

console.log('Course UI contract looks wired to real CRUD flows.')
