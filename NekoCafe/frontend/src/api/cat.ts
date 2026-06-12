import { http } from './http'
import type { ApiResult } from '@/types/api'

export type CatStatus = 'AVAILABLE' | 'RESTING' | 'ADOPTED'
export type CatHealthStatus = '健康' | '观察中' | '治疗中' | '恢复中'
export type CatGender = 'MALE' | 'FEMALE' | 'UNKNOWN'

export interface CatProfile {
  id?: number
  name: string
  breed?: string
  age?: number
  weight?: number
  gender?: CatGender | string
  personality?: string
  interact?: string
  healthStatus?: CatHealthStatus | string
  vaccinium?: string
  photoUrl?: string
  description?: string
  status?: CatStatus | string
}

export interface CatPhotoUploadResponse {
  photoUrl: string
}

export async function fetchCats() {
  const { data } = await http.get<ApiResult<CatProfile[]>>('/cats')
  return data.data
}

export async function fetchCat(id: number) {
  const { data } = await http.get<ApiResult<CatProfile>>(`/cats/${id}`)
  return data.data
}

export async function createCat(payload: CatProfile) {
  const { data } = await http.post<ApiResult<CatProfile>>('/cats', payload)
  return data.data
}

export async function updateCat(id: number, payload: CatProfile) {
  const { data } = await http.put<ApiResult<CatProfile>>(`/cats/${id}`, payload)
  return data.data
}

export async function updateCatHealthStatus(id: number, healthStatus: string) {
  const { data } = await http.patch<ApiResult<CatProfile>>(`/cats/${id}/health-status`, { healthStatus })
  return data.data
}

export async function updateCatStatus(id: number, status: string) {
  const { data } = await http.patch<ApiResult<CatProfile>>(`/cats/${id}/status`, { status })
  return data.data
}

export async function deleteCat(id: number) {
  const { data } = await http.delete<ApiResult<void>>(`/cats/${id}`)
  return data.data
}

export async function uploadCatPhoto(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ApiResult<CatPhotoUploadResponse>>('/cats/photos', formData)
  return data.data
}
