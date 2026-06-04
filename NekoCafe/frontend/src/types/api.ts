export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
  timestamp: string
}

export interface PageResponse<T> {
  page: number
  size: number
  total: number
  records: T[]
}
