import { http } from './http'
import type { ApiResult } from '@/types/api'
import type { AuthResponse, AuthUser, LoginRequest, RegisterRequest } from '@/types/auth'

export async function loginApi(payload: LoginRequest) {
  const { data } = await http.post<ApiResult<AuthResponse>>('/auth/login', payload)
  return data.data
}

export async function registerApi(payload: RegisterRequest) {
  const { data } = await http.post<ApiResult<AuthResponse>>('/auth/register', payload)
  return data.data
}

export async function getMeApi() {
  const { data } = await http.get<ApiResult<AuthUser>>('/auth/me')
  return data.data
}
