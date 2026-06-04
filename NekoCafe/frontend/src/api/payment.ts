import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface PaymentResponse {
  id: number
  paymentNo: string
  orderId: number
  amount: number
  channel: string
  status: string
  paidAt: string
}

export async function sandboxPay(orderId: number) {
  const { data } = await http.post<ApiResult<PaymentResponse>>('/payment/sandbox', {
    orderId,
    idempotencyKey: `sandbox-${orderId}`,
  })
  return data.data
}
