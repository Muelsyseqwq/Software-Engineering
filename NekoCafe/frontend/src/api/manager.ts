import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface ManagerStoreInfo {
  id: number
  name: string
  city: string
  address: string
  phone?: string
  status: string
}

export interface ManagerTableRow {
  id?: number
  tableNo: string
  capacity: number
  area?: string
  status: string
}

export interface ManagerReservationRow {
  id: number
  reservationNo: string
  customerName: string
  partySize: number
  slotTime: string
  status: string
}

export async function fetchManagerStore() {
  const { data } = await http.get<ApiResult<ManagerStoreInfo>>('/manager/store')
  return data.data
}

export async function updateManagerStoreStatus(status: string) {
  const { data } = await http.put<ApiResult<void>>('/manager/store/status', { status })
  return data.data
}

export async function fetchManagerTables() {
  const { data } = await http.get<ApiResult<ManagerTableRow[]>>('/manager/tables')
  return data.data
}

export async function createManagerTable(payload: ManagerTableRow) {
  const { data } = await http.post<ApiResult<ManagerTableRow>>('/manager/tables', payload)
  return data.data
}

export async function updateManagerTable(id: number, payload: ManagerTableRow) {
  const { data } = await http.put<ApiResult<ManagerTableRow>>(`/manager/tables/${id}`, payload)
  return data.data
}

export async function fetchManagerReservations() {
  const { data } = await http.get<ApiResult<ManagerReservationRow[]>>('/manager/reservations')
  return data.data
}
