import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface CreateOrderItemRequest {
  dishId: number
  quantity: number
}

export interface CreateOrderRequest {
  storeId: number
  reservationId?: number
  queueTicketId?: number
  items: CreateOrderItemRequest[]
  remark?: string
  rewardRedemptionId?: number
}

export type OrderStatus = 'CREATED' | 'PAID' | 'PREPARING' | 'COMPLETED' | 'CANCELLED'
export type RefundStatus = 'NONE' | 'APPLIED' | 'APPROVED' | 'REJECTED' | 'REFUNDED'

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
  tableId?: number
  tableNo?: string
  reservationId?: number
  queueTicketId?: number
  totalAmount: number
  rewardRedemptionId?: number
  couponName?: string
  couponDiscountAmount?: number
  payableAmount?: number
  status: OrderStatus | string
  refundStatus?: RefundStatus | string
  remark?: string
  paidAt?: string
  completedAt?: string
  cancelledAt?: string
  createdAt: string
  canPay?: boolean
  canRefund?: boolean
  canReview?: boolean
  reviewed?: boolean
  reviewRating?: number
  reviewContent?: string
  reviewCreatedAt?: string
  canCancel?: boolean
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

export async function cancelOrder(id: number) {
  const { data } = await http.post<ApiResult<OrderResponse>>(`/order/${id}/cancel`)
  return data.data
}
