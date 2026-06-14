export type RoleCode = 'CUSTOMER' | 'STAFF' | 'STORE_MANAGER' | 'HQ_OPERATOR' | 'CAT_CARETAKER'

export interface LoginRequest {
  account: string
  password: string
}

export interface PreferenceRequest {
  preferenceType: string
  preferenceValue: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  phone?: string
  email?: string
  preferences?: PreferenceRequest[]
}

export interface AuthAssignedStore {
  id: number
  name: string
  city?: string | null
  address?: string | null
}

export interface AuthUser {
  id: number
  username: string
  nickname: string
  phone?: string | null
  email?: string | null
  roles: RoleCode[]
  storeId?: number | null
  storeName?: string | null
  storeNames?: string[] | null
  stores?: AuthAssignedStore[] | null
}

export interface AuthResponse {
  token: string
  tokenType: 'Bearer'
  expiresAt: string
  user: AuthUser
}
