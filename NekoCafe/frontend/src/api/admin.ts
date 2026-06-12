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

export interface AdminStoreDetailRow {
  id: number
  name: string
  city: string
  address: string
  phone?: string
  openingTime: string
  closingTime: string
  status: string
  description?: string
  businessArea?: string
  latitude?: number
  longitude?: number
  coverUrl?: string
  areaSquareMeter?: number
  availableTableCount: number
}

/** @deprecated Use AdminStoreDetailRow instead */
export type AdminStoreRow = AdminStoreDetailRow

export interface CreateStoreRequest {
  name: string
  city: string
  address: string
  phone?: string
  openingTime?: string
  closingTime?: string
  status?: string
  description?: string
  businessArea?: string
  latitude?: number
  longitude?: number
  coverUrl?: string
  areaSquareMeter?: number
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
  const { data } = await http.get<ApiResult<AdminStoreDetailRow[]>>('/admin/stores')
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

// ---- store management ----

export async function fetchAdminStoreList() {
  const { data } = await http.get<ApiResult<AdminStoreDetailRow[]>>('/admin/stores')
  return data.data
}

export async function createStore(req: CreateStoreRequest) {
  const { data } = await http.post<ApiResult<AdminStoreDetailRow>>('/admin/stores', req)
  return data.data
}

export async function updateStore(id: number, req: CreateStoreRequest) {
  const { data } = await http.put<ApiResult<AdminStoreDetailRow>>(`/admin/stores/${id}`, req)
  return data.data
}

export async function deleteStore(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/admin/stores/${id}`)
  return data.data
}

export async function uploadStorePhoto(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ApiResult<{ url: string }>>('/admin/stores/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}
