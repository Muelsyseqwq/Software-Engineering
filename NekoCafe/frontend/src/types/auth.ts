export type RoleCode = 'CUSTOMER' | 'STAFF' | 'STORE_MANAGER' | 'HQ_OPERATOR' | 'CAT_CARETAKER' | 'ADMIN'

export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  phone?: string
  email?: string
}

export interface AuthUser {
  id: number
  username: string
  nickname: string
  phone?: string | null
  email?: string | null
  roles: RoleCode[]
}

export interface AuthResponse {
  token: string
  tokenType: 'Bearer'
  expiresAt: string
  user: AuthUser
}
