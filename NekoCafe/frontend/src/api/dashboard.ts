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

export interface StoreSummaryRow {
  storeId: number
  storeName: string
  city: string
  status: string
  revenue: number
  orderCount: number
  reservationCount: number
}

export interface DashboardPeriodSummary {
  start: string
  end: string
  revenue: number
  orderCount: number
  paidOrderCount: number
  reservationCount: number
  checkedInReservationCount: number
  tableCount: number
  area: number
  revenuePerSqm: number
  turnoverRate: number
  repurchaseRate: number
  averageOrderValue: number
}

export interface StoreMetrics {
  storeId: number
  storeName: string
  revenue: number
  orderCount: number
  reservationCount: number
  areaSquareMeter: number
  revenuePerSqm: number
  turnoverRate: number
  repurchaseRate: number
  periodStart: string
  periodEnd: string
}

export interface CrossStoreRow {
  storeId: number
  storeName: string
  city: string
  status: string
  revenue: number
  orderCount: number
  reservationCount: number
  revenuePerSqm: number
  turnoverRate: number
  repurchaseRate: number
  areaSquareMeter: number
}

// ---- legacy ----

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

export async function fetchStoreSummaries() {
  const { data } = await http.get<ApiResult<StoreSummaryRow[]>>('/dashboard/store-summary')
  return data.data
}

export async function fetchStoreRevenue(storeId: number) {
  const { data } = await http.get<ApiResult<DashboardTrendPoint[]>>(`/dashboard/store/${storeId}/revenue`)
  return data.data
}

// ---- new: period-aware overview ----

export async function fetchOverview(period: string, storeId?: number) {
  const params: Record<string, string | number> = { period }
  if (storeId) params.storeId = storeId
  const { data } = await http.get<ApiResult<DashboardPeriodSummary>>('/dashboard/overview', { params })
  return data.data
}

// ---- new: store metrics (坪效/翻台率/复购率) ----

export async function fetchStoreMetrics(storeId: number, period: string) {
  const { data } = await http.get<ApiResult<StoreMetrics>>(`/dashboard/store/${storeId}/metrics`, {
    params: { period },
  })
  return data.data
}

// ---- new: cross-store comparison ----

export async function fetchCrossStore(period: string) {
  const { data } = await http.get<ApiResult<CrossStoreRow[]>>('/dashboard/cross-store', { params: { period } })
  return data.data
}

// ---- new: trend for operator charts ----

export async function fetchOperatorTrend(period: string, metric: string, storeId?: number) {
  const params: Record<string, string | number> = { period, metric }
  if (storeId) params.storeId = storeId
  const { data } = await http.get<ApiResult<DashboardTrendPoint[]>>('/dashboard/trend', { params })
  return data.data
}
