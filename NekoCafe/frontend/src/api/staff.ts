import { http } from './http'
import type { ApiResult } from '@/types/api'

export type OrderStatus = 'CREATED' | 'PAID' | 'PREPARING' | 'COMPLETED'

export interface StaffReservationRow {
  id: number
  reservationNo: string
  customerName: string
  customerPhone: string
  partySize: number
  reservedTime: string
  status: string
}

export interface StaffOrderRow {
  id: number
  orderNo: string
  summary: string
  amount: number
  status: OrderStatus | string
  createdAt: string
}

export async function fetchTodayReservations() {
  const { data } = await http.get<ApiResult<StaffReservationRow[]>>('/staff/reservations/today')
  return data.data
}

export async function checkInReservation(id: number) {
  const { data } = await http.post<ApiResult<void>>(`/staff/reservations/${id}/check-in`)
  return data.data
}

export async function fetchPendingOrders() {
  const { data } = await http.get<ApiResult<StaffOrderRow[]>>('/staff/orders/pending')
  return data.data
}

export async function startOrder(id: number) {
  const { data } = await http.post<ApiResult<void>>(`/staff/orders/${id}/start`)
  return data.data
}

export async function completeOrder(id: number) {
  const { data } = await http.post<ApiResult<void>>(`/staff/orders/${id}/complete`)
  return data.data
}
