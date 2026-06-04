import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canAccessRoute, getDefaultHomeByRoles } from './permissions'

export function setupRouterGuards(router: Router) {
  router.beforeEach((to) => {
    const auth = useAuthStore()
    const isAuthPage = to.name === 'login' || to.name === 'register'

    if (isAuthPage && auth.isLoggedIn) {
      return getDefaultHomeByRoles(auth.roles)
    }

    if (to.meta.requiresAuth && !auth.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (auth.isLoggedIn && !canAccessRoute(auth.roles, to.meta.roles)) {
      return { name: 'forbidden' }
    }

    return true
  })
}
