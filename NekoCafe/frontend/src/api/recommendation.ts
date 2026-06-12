import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface RecommendationHighlight {
  id: number
  name: string
  description?: string
}

export interface RecommendationStoreItem {
  rank: number
  storeId: number
  storeName: string
  city: string
  businessArea?: string
  address: string
  status: string
  distanceKm?: number | null
  score: number
  tags: string[]
  reasons: string[]
  dishHighlights: RecommendationHighlight[]
  catHighlights: RecommendationHighlight[]
  activityHighlights: RecommendationHighlight[]
  primaryActionText: string
}

export interface RecommendationFeedResponse {
  generatedAt: string
  summary: string
  items: RecommendationStoreItem[]
}

export interface RecommendationParams {
  lat?: number
  lng?: number
  limit?: number
}

export async function fetchCustomerRecommendations(params?: RecommendationParams) {
  const { data } = await http.get<ApiResult<RecommendationFeedResponse>>('/recommend/customer', { params })
  return data.data
}
