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
  CUSTOMER: '/customer',
  STAFF: '/staff',
  CAT_CARETAKER: '/cats',
  STORE_MANAGER: '/manager',
  HQ_OPERATOR: '/admin',
  ADMIN: '/admin',
}

const ROLE_PRIORITY: RoleCode[] = ['ADMIN', 'HQ_OPERATOR', 'STORE_MANAGER', 'STAFF', 'CAT_CARETAKER', 'CUSTOMER']

export const APP_MENU_ITEMS: AppMenuItem[] = [
  // Customer
  { path: '/customer', label: '顾客首页', icon: '🐱', hint: '会员积分与活动总览', roles: ['CUSTOMER'] },
  { path: '/stores', label: '门店浏览', icon: '☕', hint: '寻找今日猫咖座位', roles: ['CUSTOMER'], guest: true },
  { path: '/customer/activities', label: '活动中心', icon: '🎉', hint: '优惠与娱乐活动', roles: ['CUSTOMER'] },
  { path: '/reservations/new', label: '创建预约', icon: '🐾', hint: '锁定互动时段', roles: ['CUSTOMER'] },
  { path: '/reservations/me', label: '我的预约', icon: '📒', hint: '查看猫爪行程', roles: ['CUSTOMER'] },
  { path: '/menu', label: '菜品点单', icon: '🍰', hint: '甜品饮品提前选', roles: ['CUSTOMER'], guest: true },
  { path: '/orders/checkout', label: '订单结算', icon: '🧾', hint: '确认订单与支付', roles: ['CUSTOMER'] },
  { path: '/customer/orders', label: '我的订单', icon: '📦', hint: '支付、退款与评价', roles: ['CUSTOMER'] },
  { path: '/customer/profile', label: '会员偏好', icon: '🎫', hint: '积分流水与喜好标签', roles: ['CUSTOMER'] },
  // Staff
  { path: '/staff', label: '店员后台', icon: '🛎️', hint: '前台值班总览', roles: ['STAFF'] },
  { path: '/staff/check-in', label: '预约签到', icon: '✅', hint: '核销到店顾客', roles: ['STAFF'] },
  { path: '/staff/orders', label: '订单履约', icon: '🥐', hint: '推进制作状态', roles: ['STAFF'] },
  { path: '/staff/table-cat-status', label: '桌位猫咪', icon: '🪑', hint: '桌位与猫咪状态', roles: ['STAFF'] },
  // Cat caretaker
  { path: '/cats', label: '猫咪档案', icon: '🐈', hint: '健康与互动记录', roles: ['CAT_CARETAKER'] },
  // Store manager
  { path: '/manager', label: '店长后台', icon: '🏠', hint: '门店与桌位运营', roles: ['STORE_MANAGER'] },
  // HQ_OPERATOR / ADMIN — separate pages in sidebar
  { path: '/admin', label: '运营罗盘', icon: '📊', hint: '门店营收、坪效、翻台率与复购率', roles: ['HQ_OPERATOR', 'ADMIN'] },
  { path: '/admin/activities', label: '活动管理', icon: '🎉', hint: '创建活动并发布给门店', roles: ['HQ_OPERATOR', 'ADMIN'] },
  { path: '/admin/store-managers', label: '店长分配', icon: '👤', hint: '管理各门店的店长', roles: ['HQ_OPERATOR', 'ADMIN'] },
  { path: '/admin/users', label: '用户与角色', icon: '⚙️', hint: '用户、角色与门店管理', roles: ['HQ_OPERATOR', 'ADMIN'] },
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
