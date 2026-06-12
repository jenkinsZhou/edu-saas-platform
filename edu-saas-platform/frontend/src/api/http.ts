import axios, { AxiosError } from 'axios'

interface ApiResult<T> {
  code: number
  message: string
  data: T
  requestId?: string
}

export interface LoginPayload {
  organizationCode?: string
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: {
    id: number
    accountId: number
    name: string
    tenantId: number
    organizationName?: string
    organizationCode?: string
    roles: string[]
    permissions: string[]
    campusIds: number[]
  }
}

const TOKEN_KEY = 'edu_saas_access_token'
const REFRESH_TOKEN_KEY = 'edu_saas_refresh_token'
const USER_KEY = 'edu_saas_login_user'

let accessToken = localStorage.getItem(TOKEN_KEY) ?? ''
let refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
let refreshPromise: Promise<string> | null = null

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  config.headers.set('X-Request-Id', createRequestId())
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const originalRequest = error.config as (typeof error.config & { _retry?: boolean }) | undefined
    const url = originalRequest?.url ?? ''
    if (status === 401 || status === 403) {
      if (status === 401 && refreshToken && originalRequest && !originalRequest._retry && !url.includes('/auth/login') && !url.includes('/auth/refresh')) {
        originalRequest._retry = true
        try {
          const newAccessToken = await refreshAccessToken()
          originalRequest.headers?.set?.('Authorization', `Bearer ${newAccessToken}`)
          return http(originalRequest)
        } catch {
          clearAuth()
        }
      }
      clearAuth()
      if (!window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.assign(`/login?redirect=${redirect}`)
      }
    }
    return Promise.reject(error)
  }
)

export async function login(payload: LoginPayload): Promise<LoginResult> {
  try {
    const response = await http.post<ApiResult<LoginResult>>('/auth/login', payload)
    if (response.data.code !== 0) {
      throw new Error(`${response.data.message} requestId=${response.data.requestId ?? ''}`)
    }
    setAuth(response.data.data)
    return response.data.data
  } catch (error) {
    if (axios.isAxiosError<ApiResult<unknown>>(error) && error.response?.data?.message) {
      const requestId = error.response.data.requestId ? ` requestId=${error.response.data.requestId}` : ''
      throw new Error(`${error.response.data.message}${requestId}`)
    }
    throw error
  }
}

export async function logout() {
  try {
    await http.post('/auth/logout', { refreshToken })
  } finally {
    clearAuth()
  }
}

export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  requireAuth()
  const response = await http.get<ApiResult<T>>(url, { params })
  return unwrap(response.data)
}

export async function apiPost<T>(url: string, data?: unknown): Promise<T> {
  requireAuth()
  const response = await http.post<ApiResult<T>>(url, data)
  return unwrap(response.data)
}

export async function apiPut<T>(url: string, data?: unknown): Promise<T> {
  requireAuth()
  const response = await http.put<ApiResult<T>>(url, data)
  return unwrap(response.data)
}

export async function apiDelete<T>(url: string): Promise<T> {
  requireAuth()
  const response = await http.delete<ApiResult<T>>(url)
  return unwrap(response.data)
}

export function isAuthenticated() {
  return Boolean(accessToken)
}

export function getCurrentAccessToken() {
  return accessToken
}

export function getLoginUser(): LoginResult['user'] | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as LoginResult['user']
  } catch {
    return null
  }
}

function setAuth(result: LoginResult) {
  accessToken = result.accessToken
  refreshToken = result.refreshToken
  localStorage.setItem(TOKEN_KEY, result.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(result.user))
}

function clearAuth() {
  accessToken = ''
  refreshToken = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function refreshAccessToken() {
  if (refreshPromise) {
    return refreshPromise
  }
  refreshPromise = doRefreshAccessToken()
  try {
    return await refreshPromise
  } finally {
    refreshPromise = null
  }
}

async function doRefreshAccessToken() {
  const response = await http.post<ApiResult<LoginResult>>('/auth/refresh', { refreshToken })
  if (response.data.code !== 0) {
    throw new Error(response.data.message)
  }
  setAuth(response.data.data)
  return response.data.data.accessToken
}

function requireAuth() {
  if (!accessToken) {
    throw new Error('未登录')
  }
}

function unwrap<T>(result: ApiResult<T>): T {
  if (result.code !== 0) {
    throw new Error(`${result.message} requestId=${result.requestId ?? ''}`)
  }
  return result.data
}

function createRequestId() {
  return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}
