import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

interface ApiErrorPayload {
  code?: number
  message?: string
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiErrorPayload | undefined
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiErrorPayload>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      const path = window.location.pathname
      if (!path.includes('/login') && !path.includes('/register')) {
        ElMessage.warning('登录已过期，请重新登录')
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `${window.location.origin}/login?redirect=${redirect}`
      }
    }
    if (status === 403) {
      ElMessage.warning('当前账号无访问权限')
      if (!window.location.pathname.includes('/403')) {
        window.location.href = `${window.location.origin}/403`
      }
    }
    return Promise.reject(new Error(message))
  },
)
