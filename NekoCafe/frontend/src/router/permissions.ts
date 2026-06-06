import type { RoleCode } from '@/types/auth'

export interface AppMenuItem {
  path: string
  label: string
  icon: string
  hint: string
  roles?: RoleCode[]
  guest?: boolean
}

export const ROLE_HOME_MAP: Record<RoleCode, string> = {
  CUSTOMER: '/stores',
  STAFF: '/staff',
  CAT_CARETAKER: '/cats',
  STORE_MANAGER: '/manager',
  HQ_OPERATOR: '/dashboard',
  ADMIN: '/admin',
}

const ROLE_PRIORITY: RoleCode[] = ['ADMIN', 'HQ_OPERATOR', 'STORE_MANAGER', 'STAFF', 'CAT_CARETAKER', 'CUSTOMER']

export const APP_MENU_ITEMS: AppMenuItem[] = [
  { path: '/stores', label: '门店浏览', icon: '☕', hint: '寻找今日猫咖座位', roles: ['CUSTOMER'], guest: true },
  { path: '/reservations/new', label: '创建预约', icon: '🐾', hint: '锁定互动时段', roles: ['CUSTOMER'] },
  { path: '/reservations/me', label: '我的预约', icon: '📒', hint: '查看猫爪行程', roles: ['CUSTOMER'] },
  { path: '/menu', label: '菜品点单', icon: '🍰', hint: '甜品饮品提前选', roles: ['CUSTOMER'], guest: true },
  { path: '/orders/checkout', label: '订单结算', icon: '🧾', hint: '确认订单与支付', roles: ['CUSTOMER'] },
  { path: '/staff', label: '店员后台', icon: '🛎️', hint: '前台值班总览', roles: ['STAFF'] },
  { path: '/staff/check-in', label: '预约签到', icon: '✅', hint: '核销到店顾客', roles: ['STAFF'] },
  { path: '/staff/orders', label: '订单履约', icon: '🥐', hint: '推进制作状态', roles: ['STAFF'] },
  { path: '/cats', label: '猫咪档案', icon: '🐈', hint: '健康与互动记录', roles: ['CAT_CARETAKER'] },
  { path: '/manager', label: '店长管理', icon: '🏠', hint: '门店与桌位运营', roles: ['STORE_MANAGER'] },
  { path: '/dashboard', label: '运营罗盘', icon: '🧭', hint: '跨店经营总览', roles: ['HQ_OPERATOR', 'ADMIN'] },
  { path: '/admin', label: '总控后台', icon: '🔑', hint: '用户角色与平台', roles: ['HQ_OPERATOR', 'ADMIN'] },
]

export function hasAnyRole(userRoles: RoleCode[], requiredRoles?: RoleCode[]) {
  if (!requiredRoles || requiredRoles.length === 0) return true
  return requiredRoles.some((role) => userRoles.includes(role))
}

export function getDefaultHomeByRoles(userRoles: RoleCode[]) {
  const matchedRole = ROLE_PRIORITY.find((role) => userRoles.includes(role))
  return matchedRole ? ROLE_HOME_MAP[matchedRole] : '/stores'
}

export function canAccessRoute(userRoles: RoleCode[], requiredRoles?: RoleCode[]) {
  return hasAnyRole(userRoles, requiredRoles)
}

export function filterMenusByRoles(userRoles: RoleCode[]) {
  if (userRoles.length === 0) {
    return APP_MENU_ITEMS.filter((item) => item.guest)
  }
  return APP_MENU_ITEMS.filter((item) => hasAnyRole(userRoles, item.roles))
}
