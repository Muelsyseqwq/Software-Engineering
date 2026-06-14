import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface ActivityRow {
  id: number
  title: string
  type: string
  description?: string
  coverUrl?: string
  startAt: string
  endAt: string
  status: string
  createdBy?: number
  createdAt: string
  updatedAt: string
  rewardId?: number
  rewardName?: string
}

export interface CreateActivityRequest {
  title: string
  type?: string
  description?: string
  coverUrl?: string
  startAt?: string
  endAt?: string
  status?: string
  createdBy?: number
  rewardId?: number
}

export interface StoreAcceptanceRow {
  id: number
  storeId: number
  storeName: string
  acceptStatus: string
  handledBy?: number
  handlerName?: string
  handledAt?: string
  handleRemark?: string
}

export async function fetchActivities(type?: string, status?: string) {
  const params: Record<string, string> = {}
  if (type) params.type = type
  if (status) params.status = status
  const { data } = await http.get<ApiResult<ActivityRow[]>>('/activity', { params })
  return data.data
}

export async function createActivity(request: CreateActivityRequest) {
  const { data } = await http.post<ApiResult<ActivityRow>>('/activity', request)
  return data.data
}

export async function updateActivity(id: number, request: CreateActivityRequest) {
  const { data } = await http.put<ApiResult<ActivityRow>>(`/activity/${id}`, request)
  return data.data
}

export async function deleteActivity(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/activity/${id}`)
  return data.data
}

export async function publishActivity(id: number, storeIds: number[]) {
  const { data } = await http.post<ApiResult<null>>(`/activity/${id}/publish`, { storeIds })
  return data.data
}

export async function fetchActivityStores(id: number) {
  const { data } = await http.get<ApiResult<StoreAcceptanceRow[]>>(`/activity/${id}/stores`)
  return data.data
}

export interface RewardOption {
  id: number
  name: string
  rewardType: string
}

export async function fetchActivityRewards() {
  const { data } = await http.get<ApiResult<RewardOption[]>>('/activity/rewards')
  return data.data
}

export interface CreateRewardRequest {
  name: string
  description?: string
  discountAmount?: number
  pointsCost?: number
  stock?: number
  validFrom?: string
  validTo?: string
  coverUrl?: string
}

export async function createActivityReward(request: CreateRewardRequest) {
  const { data } = await http.post<ApiResult<RewardOption>>('/activity/rewards', request)
  return data.data
}
