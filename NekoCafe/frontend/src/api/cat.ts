import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface CatProfile {
  id?: number
  name: string
  breed?: string
  age?: number
  gender?: string
  personality?: string
  healthStatus?: string
  photoUrl?: string
  description?: string
  status?: string
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

export async function deleteCat(id: number) {
  const { data } = await http.delete<ApiResult<void>>(`/cats/${id}`)
  return data.data
}
