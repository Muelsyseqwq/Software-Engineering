import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface ReservationSlotQuery {
  storeId: number | string
  date: string
  partySize: number
}

export interface ReservationSlot {
  id: number
  storeId: number
  tableId: number
  tableNo: string
  capacity: number
  area?: string
  slotDate: string
  startTime: string
  endTime: string
  availableCount: number
  status: string
}

export interface CreateReservationPayload {
  storeId: number
  tableId: number
  slotId: number
  partySize: number
  contactName: string
  contactPhone: string
  remark?: string
}

export interface ReservationRow {
  id: number
  reservationNo: string
  storeId: number
  storeName: string
  tableId: number
  tableNo: string
  area?: string
  slotId: number
  partySize: number
  slotDate: string
  startTime: string
  endTime: string
  status: string
  contactName: string
  contactPhone: string
  remark?: string
  createdAt: string
}

export interface ApplyQueuePayload {
  storeId: number
  partySize: number
  contactName: string
  contactPhone: string
}

export interface QueueTicket {
  id: number
  storeId: number
  queueDate: string
  queueNumber: number
  partySize: number
  tableId?: number
  tableNo?: string
  area?: string
  status: string
  contactName: string
  contactPhone: string
  calledAt?: string
  seatedAt?: string
  cancelledAt?: string
  expiredAt?: string
  createdAt?: string
}

export interface QueueStatus {
  storeId: number
  queueDate: string
  currentNumber: number
  nextNumber: number
  waitingCount: number
  myTicket?: QueueTicket
  canApply: boolean
}

export async function fetchReservationSlots(params: ReservationSlotQuery) {
  const { data } = await http.get<ApiResult<ReservationSlot[]>>('/reservation/slots', { params })
  return data.data
}

export async function createReservation(payload: CreateReservationPayload) {
  const { data } = await http.post<ApiResult<ReservationRow>>('/reservation', payload)
  return data.data
}

export async function fetchMyReservations() {
  const { data } = await http.get<ApiResult<ReservationRow[]>>('/reservation/me')
  return data.data
}

export async function cancelReservation(id: number | string) {
  const { data } = await http.post<ApiResult<ReservationRow>>(`/reservation/${id}/cancel`)
  return data.data
}

export async function applyQueue(payload: ApplyQueuePayload) {
  const { data } = await http.post<ApiResult<QueueTicket>>('/reservation/queue', payload)
  return data.data
}

export async function fetchQueueStatus(storeId: number | string, partySize?: number) {
  const { data } = await http.get<ApiResult<QueueStatus>>('/reservation/queue/status', { params: { storeId, partySize } })
  return data.data
}

export async function cancelQueueTicket(id: number | string) {
  const { data } = await http.post<ApiResult<QueueTicket>>(`/reservation/queue/${id}/cancel`)
  return data.data
}
