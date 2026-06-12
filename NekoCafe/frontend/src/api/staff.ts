import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface StaffReservationRow {
  id: number
  reservationNo: string
  customerName: string
  customerPhone: string
  partySize: number
  timeSlot: string
  remark: string
  status: string
}

export interface StaffOrderRow {
  id: number
  orderNo: string
  summary: string
  amount: number
  tableNo: string
  status: string
  createdAt: string
}

export interface DiningTable {
  id: number
  storeId: number
  tableNo: string
  capacity: number
  area: string
  status: string
}

export interface Cat {
  id: number
  storeId: number
  name: string
  breed: string
  age: number
  gender: string
  personality: string
  healthStatus: string
  status: string
}

export interface StaffReviewRow {
  id: number
  customerName: string
  orderNo: string
  rating: number
  content: string
  status: string
  createdAt: string
}

export interface StaffQueueTicket {
  id: number
  queueNumber: number
  partySize: number
  status: string
  contactName: string
  contactPhone: string
  calledAt?: string
  seatedAt?: string
  expiredAt?: string
  createdAt?: string
}

export interface StaffQueueStatus {
  storeId: number
  queueDate: string
  currentNumber: number
  nextNumber: number
  waitingCount: number
  calledTicket?: StaffQueueTicket
  tickets: StaffQueueTicket[]
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

export async function fetchHandledOrders() {
  const { data } = await http.get<ApiResult<StaffOrderRow[]>>('/staff/orders/handled')
  return data.data
}

export async function fetchTables(status?: string, capacity?: number) {
  const { data } = await http.get<ApiResult<DiningTable[]>>('/staff/tables', { params: { status, capacity } })
  return data.data
}

export async function fetchCats(status?: string) {
  const { data } = await http.get<ApiResult<Cat[]>>('/staff/cats', { params: { status } })
  return data.data
}

export async function fetchOrderReview(orderId: number) {
  const { data } = await http.get<ApiResult<StaffReviewRow>>(`/staff/orders/${orderId}/review`)
  return data.data
}

export async function updateTableStatus(id: number, status: string, reason?: string) {
  const { data } = await http.put<ApiResult<void>>(`/staff/tables/${id}/status`, null, {
    params: { status, reason }
  })
  return data.data
}

export async function fetchStaffQueueStatus(storeId: number | string) {
  const { data } = await http.get<ApiResult<StaffQueueStatus>>('/staff/queues/status', { params: { storeId } })
  return data.data
}

export async function callNextQueueNumber(storeId: number | string) {
  const { data } = await http.post<ApiResult<StaffQueueStatus>>(`/staff/queues/${storeId}/next`)
  return data.data
}

export async function markQueueTicketSeated(ticketId: number | string) {
  const { data } = await http.post<ApiResult<void>>(`/staff/queues/tickets/${ticketId}/seat`)
  return data.data
}

export async function resetQueue(storeId: number | string) {
  const { data } = await http.post<ApiResult<StaffQueueStatus>>(`/staff/queues/${storeId}/reset`)
  return data.data
}
