import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/utils/auth'
import type { ApiResponse } from '@/types/api'

const WHITE_LIST = ['/admin/login', '/admin/register']

let loadingCount = 0
let loadingInstance: ReturnType<typeof ElLoading.service> | null = null

function startLoading(): void {
  if (loadingCount === 0) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '加载中...',
      background: 'rgba(255, 255, 255, 0.6)',
    })
  }
  loadingCount += 1
}

function stopLoading(): void {
  loadingCount = Math.max(loadingCount - 1, 0)
  if (loadingCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

function shouldSkipAuth(url = ''): boolean {
  return WHITE_LIST.some((path) => url.includes(path))
}

async function redirectToLogin(): Promise<void> {
  clearToken()
  const { default: router } = await import('@/router')
  if (router.currentRoute.value.path !== '/login') {
    await router.push({ path: '/login' })
  }
}

export const http: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 60_000,
})

http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (!config.skipLoading) {
      startLoading()
    }

    const token = getToken()
    if (token && !config.skipAuth && !shouldSkipAuth(config.url)) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  (error: unknown) => {
    stopLoading()
    return Promise.reject(error)
  },
)

http.interceptors.response.use(
  (response) => {
    if (!response.config.skipLoading) {
      stopLoading()
    }

    if (response.config.responseType === 'blob' || response.config.raw) {
      return response.data
    }

    const payload = response.data as ApiResponse
    if (payload.code !== 200) {
      ElMessage.error(payload.msg || '请求失败')
      if (payload.code === 401) {
        void redirectToLogin()
      }
      return Promise.reject(payload)
    }

    return payload.data
  },
  (error: unknown) => {
    const axiosError = error as {
      config?: { skipLoading?: boolean }
      message?: string
      response?: { status?: number; data?: { msg?: string } }
    }

    if (!axiosError.config?.skipLoading) {
      stopLoading()
    }

    ElMessage.error(axiosError.response?.data?.msg || axiosError.message || '网络异常')
    if (axiosError.response?.status === 401) {
      void redirectToLogin()
    }
    return Promise.reject(error)
  },
)