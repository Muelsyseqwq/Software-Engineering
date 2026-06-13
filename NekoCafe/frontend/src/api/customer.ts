import { http } from './http'
import type { ApiResult } from '@/types/api'
import type { OrderResponse } from './order'

export interface ActivityStoreResponse {
  storeId: number
  storeName: string
  city?: string
  address?: string
}

export interface CustomerActivityResponse {
  id: number
  title: string
  type: string
  typeText: string
  description?: string
  coverUrl?: string
  startAt: string
  endAt: string
  stores: ActivityStoreResponse[]
}

export interface PointsTransactionRow {
  id: number
  orderId?: number
  rewardRedemptionId?: number
  type: string
  points: number
  balanceAfter: number
  description?: string
  createdAt: string
}

export interface PointsSummaryResponse {
  memberAccountId: number
  levelCode: string
  points: number
  totalSpent: number
  transactions: PointsTransactionRow[]
}

export interface RewardCatalogResponse {
  id: number
  name: string
  description?: string
  pointsCost: number
  discountAmount?: number
  rewardType: 'COUPON' | 'SERVICE' | 'ITEM' | string
  coverUrl?: string
  stock?: number | null
  status: string
  validFrom?: string | null
  validTo?: string | null
}

export interface RewardRedemptionResponse {
  id: number
  redemptionNo: string
  rewardId: number
  orderId?: number
  rewardName: string
  pointsCost: number
  status: string
  redeemedAt: string
  usedAt?: string | null
  sourceType?: string | null
  sourceId?: number | null
}

export interface RedeemRewardResponse {
  redemption: RewardRedemptionResponse
  balanceAfter: number
  reward: RewardCatalogResponse
}

export interface PreferenceRequest {
  preferenceType: string
  preferenceValue: string
}

export interface PreferenceResponse extends PreferenceRequest {
  id: number
}

export interface HomeOrderStats {
  pendingPayment: number
  paid: number
  preparing: number
  completed: number
  refundable: number
  reviewable: number
}

export interface CustomerHomeResponse {
  points: PointsSummaryResponse
  orderStats: HomeOrderStats
  recentOrders: OrderResponse[]
  activities: CustomerActivityResponse[]
  preferences: PreferenceResponse[]
}

export interface ReviewRequest {
  rating: number
  content?: string
}

export interface ReviewResponse {
  id: number
  orderId: number
  orderNo?: string
  storeId: number
  storeName?: string
  rating: number
  content?: string
  status: string
  createdAt: string
}

export interface RefundRequestPayload {
  reason?: string
}

export interface RefundResponse {
  id: number
  refundNo: string
  orderId: number
  orderNo?: string
  storeId?: number
  amount: number
  reason?: string
  status: string
  orderStatus?: string
  reviewRemark?: string
  createdAt: string
  reviewedAt?: string
}

export async function fetchCustomerHome() {
  const { data } = await http.get<ApiResult<CustomerHomeResponse>>('/customer/home')
  return data.data
}

export async function fetchCustomerActivities(params?: { type?: string; storeId?: number }) {
  const { data } = await http.get<ApiResult<CustomerActivityResponse[]>>('/customer/activities', { params })
  return data.data
}

export async function fetchCustomerPoints() {
  const { data } = await http.get<ApiResult<PointsSummaryResponse>>('/customer/points')
  return data.data
}

export async function fetchRewardCatalog() {
  const { data } = await http.get<ApiResult<RewardCatalogResponse[]>>('/customer/rewards')
  return data.data
}

export async function redeemReward(rewardId: number) {
  const { data } = await http.post<ApiResult<RedeemRewardResponse>>(`/customer/rewards/${rewardId}/redeem`)
  return data.data
}

export async function fetchMyRedemptions() {
  const { data } = await http.get<ApiResult<RewardRedemptionResponse[]>>('/customer/redemptions/me')
  return data.data
}

export async function fetchCustomerPreferences() {
  const { data } = await http.get<ApiResult<PreferenceResponse[]>>('/customer/preferences')
  return data.data
}

export async function saveCustomerPreferences(payload: PreferenceRequest[]) {
  const { data } = await http.put<ApiResult<PreferenceResponse[]>>('/customer/preferences', payload)
  return data.data
}

export async function createReview(orderId: number, payload: ReviewRequest) {
  const { data } = await http.post<ApiResult<ReviewResponse>>(`/customer/orders/${orderId}/reviews`, payload)
  return data.data
}

export async function fetchMyReviews() {
  const { data } = await http.get<ApiResult<ReviewResponse[]>>('/customer/reviews/me')
  return data.data
}

export async function applyRefund(orderId: number, payload: RefundRequestPayload) {
  const { data } = await http.post<ApiResult<RefundResponse>>(`/customer/orders/${orderId}/refunds`, payload)
  return data.data
}

export async function fetchMyRefunds() {
  const { data } = await http.get<ApiResult<RefundResponse[]>>('/customer/refunds/me')
  return data.data
}
