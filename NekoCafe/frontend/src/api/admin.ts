import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface AdminUserRow {
  id: number
  username: string
  nickname: string
  phone?: string
  email?: string
  status: string
  roles: string[]
}

export interface AdminRoleRow {
  id: number
  code: string
  name: string
  description?: string
}

export interface AdminStoreRow {
  id: number
  name: string
  city: string
  address: string
  status: string
}

export async function fetchAdminUsers() {
  const { data } = await http.get<ApiResult<AdminUserRow[]>>('/admin/users')
  return data.data
}

export async function fetchAdminRoles() {
  const { data } = await http.get<ApiResult<AdminRoleRow[]>>('/admin/roles')
  return data.data
}

export async function fetchAdminStores() {
  const { data } = await http.get<ApiResult<AdminStoreRow[]>>('/admin/stores')
  return data.data
}
