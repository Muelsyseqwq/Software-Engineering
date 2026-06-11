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

export interface StoreManagerRow {
  id: number
  userId: number
  username: string
  nickname: string
  storeId: number
  storeName: string
  status: string
  createdAt: string
}

export async function fetchAdminUsers() {
  const { data } = await http.get<ApiResult<AdminUserRow[]>>('/admin/users')
  return data.data
}

export async function updateUserStatus(id: number, status: string) {
  const { data } = await http.put<ApiResult<null>>(`/admin/users/${id}/status`, { status })
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

export async function fetchStoreManagers() {
  const { data } = await http.get<ApiResult<StoreManagerRow[]>>('/admin/store-managers')
  return data.data
}

export async function assignStoreManager(userId: number, storeId: number, createdBy?: number) {
  const { data } = await http.post<ApiResult<null>>('/admin/store-managers', { userId, storeId, createdBy })
  return data.data
}

export async function removeStoreManager(userId: number, storeId: number, dismissedBy?: number) {
  const { data } = await http.delete<ApiResult<null>>('/admin/store-managers', {
    data: { userId, storeId, dismissedBy },
  })
  return data.data
}

export interface CreateStoreManagerWithUserRequest {
  username: string
  password: string
  nickname: string
  phone?: string
  email?: string
  storeId: number
}

export async function createStoreManagerWithUser(req: CreateStoreManagerWithUserRequest) {
  const { data } = await http.post<ApiResult<AdminUserRow>>('/admin/store-managers/with-user', req)
  return data.data
}
