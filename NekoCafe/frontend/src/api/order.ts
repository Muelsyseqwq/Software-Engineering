import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface CreateOrderItemRequest {
  dishId: number
  quantity: number
}

export interface CreateOrderRequest {
  storeId: number
  reservationId?: number
  items: CreateOrderItemRequest[]
  remark?: string
}

export type OrderStatus = 'CREATED' | 'PAID' | 'PREPARING' | 'COMPLETED'

export interface OrderItemResponse {
  id: number
  dishId: number
  dishName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface OrderResponse {
  id: number
  orderNo: string
  storeId: number
  storeName: string
  reservationId?: number
  totalAmount: number
  status: OrderStatus | string
  remark?: string
  createdAt: string
  items: OrderItemResponse[]
}

export async function createOrder(payload: CreateOrderRequest) {
  const { data } = await http.post<ApiResult<OrderResponse>>('/order', payload)
  return data.data
}

export async function fetchMyOrders() {
  const { data } = await http.get<ApiResult<OrderResponse[]>>('/order/me')
  return data.data
}
