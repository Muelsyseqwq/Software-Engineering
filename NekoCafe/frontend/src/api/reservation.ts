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
