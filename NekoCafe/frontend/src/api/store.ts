import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface StoreSummary {
  id: number
  name: string
  city: string
  address: string
  phone?: string
  openingTime: string
  closingTime: string
  status: string
  description?: string
  availableTableCount: number
}

export interface TableSummary {
  id: number
  tableNo: string
  capacity: number
  area?: string
  status: string
}

export interface StoreDetail extends Omit<StoreSummary, 'availableTableCount'> {
  tables: TableSummary[]
}

export async function fetchStores() {
  const { data } = await http.get<ApiResult<StoreSummary[]>>('/store')
  return data.data
}

export async function fetchStoreDetail(id: number | string) {
  const { data } = await http.get<ApiResult<StoreDetail>>(`/store/${id}`)
  return data.data
}
