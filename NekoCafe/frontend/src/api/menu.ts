import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface DishItem {
  id: number
  categoryId: number
  name: string
  price: number
  stock: number
  status: string
  description?: string
  imageUrl?: string
}

export interface MenuCategory {
  id: number
  name: string
  sortOrder: number
  dishes: DishItem[]
}

export async function fetchStoreMenu(storeId: number | string) {
  const { data } = await http.get<ApiResult<MenuCategory[]>>(`/menu/stores/${storeId}`)
  return data.data
}
