import type { RoleCode } from './auth'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: RoleCode[]
  }
}
