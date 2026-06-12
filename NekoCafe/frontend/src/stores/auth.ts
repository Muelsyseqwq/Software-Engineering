import { defineStore } from 'pinia'
import type { AuthResponse, AuthUser, RoleCode } from '@/types/auth'

const TOKEN_KEY = 'nekocafe_token'
const USER_KEY = 'nekocafe_user'
const ROLES_KEY = 'nekocafe_roles'
const EXPIRES_AT_KEY = 'nekocafe_expires_at'

function readJson<T>(key: string, fallback: T): T {
  const raw = localStorage.getItem(key)
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    localStorage.removeItem(key)
    return fallback
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readJson<AuthUser | null>(USER_KEY, null),
    roles: readJson<RoleCode[]>(ROLES_KEY, []),
    expiresAt: localStorage.getItem(EXPIRES_AT_KEY) || '',
    profileSynced: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },
    setAuth(auth: AuthResponse) {
      this.token = auth.token
      this.user = auth.user
      this.roles = auth.user.roles
      this.expiresAt = auth.expiresAt
      this.profileSynced = true
      localStorage.setItem(TOKEN_KEY, auth.token)
      localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
      localStorage.setItem(ROLES_KEY, JSON.stringify(auth.user.roles))
      localStorage.setItem(EXPIRES_AT_KEY, auth.expiresAt)
    },
    async fetchMe() {
      const { getMeApi } = await import('@/api/auth')
      const user = await getMeApi()
      this.user = user
      this.roles = user.roles
      this.profileSynced = true
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      localStorage.setItem(ROLES_KEY, JSON.stringify(user.roles))
      return user
    },
    logout() {
      this.token = ''
      this.user = null
      this.roles = []
      this.expiresAt = ''
      this.profileSynced = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(ROLES_KEY)
      localStorage.removeItem(EXPIRES_AT_KEY)
    },
  },
})
