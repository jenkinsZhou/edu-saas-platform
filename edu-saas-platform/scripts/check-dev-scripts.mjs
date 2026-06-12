import { accessSync, constants, readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const scripts = [
  {
    path: resolve(root, 'scripts/start-dev.sh'),
    required: [
      'mvn clean install -DskipTests',
      'mvn spring-boot:run',
      'npm install',
      'npm run dev -- --host 127.0.0.1',
      '--with-deps',
      'logs/dev',
      '.pids'
    ]
  },
  {
    path: resolve(root, 'scripts/stop-dev.sh'),
    required: [
      '.pids',
      'edu-api.pid',
      'frontend.pid'
    ]
  }
]

const failures = []

for (const script of scripts) {
  try {
    accessSync(script.path, constants.F_OK)
  } catch {
    failures.push(`Missing script: ${script.path}`)
    continue
  }

  try {
    accessSync(script.path, constants.X_OK)
  } catch {
    failures.push(`Script is not executable: ${script.path}`)
  }

  try {
    execFileSync('bash', ['-n', script.path])
  } catch (error) {
    failures.push(`Script has shell syntax errors: ${script.path}\n${error.stderr?.toString() ?? error.message}`)
  }

  const content = readFileSync(script.path, 'utf8')
  for (const needle of script.required) {
    if (!content.includes(needle)) {
      failures.push(`${script.path} is missing required content: ${needle}`)
    }
  }
}

if (failures.length > 0) {
  console.error(failures.join('\n'))
  process.exit(1)
}

console.log('Development scripts look ready.')
