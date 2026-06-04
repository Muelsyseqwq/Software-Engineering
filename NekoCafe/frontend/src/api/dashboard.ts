import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface DashboardSummary {
  reservationCount: number
  orderCount: number
  revenue: number
  userCount: number
  storeCount: number
  catCount: number
}

export interface DashboardTrendPoint {
  label: string
  value: number
}

export async function fetchDashboardSummary() {
  const { data } = await http.get<ApiResult<DashboardSummary>>('/dashboard/summary')
  return data.data
}

export async function fetchDashboardRevenue() {
  const { data } = await http.get<ApiResult<DashboardTrendPoint[]>>('/dashboard/revenue')
  return data.data
}

export async function fetchDashboardReservations() {
  const { data } = await http.get<ApiResult<DashboardTrendPoint[]>>('/dashboard/reservations')
  return data.data
}
