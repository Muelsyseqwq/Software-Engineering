import { http } from './http'
import type { ApiResult } from '@/types/api'

export interface ManagerStoreInfo {
  id: number
  name: string
  city: string
  address: string
  phone?: string
  openingTime: string
  closingTime: string
  status: string
  description?: string
}

export interface UpdateManagerStorePayload {
  name: string
  city: string
  address: string
  phone?: string
  openingTime: string
  closingTime: string
  description?: string
}

export interface ManagerMetricsSummary {
  storeId: number
  from: string
  to: string
  revenue: number
  paidOrderCount: number
  completedOrderCount: number
  reservationCount: number
  checkedInReservationCount: number
  tableCount: number
  tableTurnoverRate: number
  areaSquareMeter?: number
  revenuePerSquareMeter: number
  averageOrderValue: number
}

export interface ManagerDateRangeQuery {
  from?: string
  to?: string
}

export interface ManagerTableRow {
  id?: number
  tableNo: string
  capacity: number
  area?: string
  status: string
}

export interface ManagerReservationRow {
  id: number
  reservationNo: string
  customerName: string
  contactPhone?: string
  partySize: number
  tableNo?: string
  slotTime: string
  status: string
  remark?: string
  createdAt?: string
}

export interface ManagerReservationQuery {
  status?: string
  date?: string
}

export interface ManagerOrderQuery extends ManagerDateRangeQuery {
  status?: string
}

export interface ManagerOrderRow {
  id: number
  orderNo: string
  customerName?: string
  tableNo?: string
  reservationNo?: string
  totalAmount: number
  status: string
  refundStatus?: string
  paidAt?: string
  completedAt?: string
  createdAt?: string
  itemSummary?: string
}

export interface ManagerOrderItemRow {
  id: number
  dishId?: number
  dishName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface ManagerOrderDetail extends ManagerOrderRow {
  remark?: string
  items: ManagerOrderItemRow[]
}

export interface ManagerCatStatusRow {
  id: number
  name: string
  breed?: string
  age?: number
  gender?: string
  healthStatus?: string
  status: string
  photoUrl?: string
  description?: string
}

export interface ManagerStaffQuery {
  status?: string
  roleCode?: string
}

export interface ManagerStaffRow {
  userStoreRoleId: number
  userId: number
  username?: string
  nickname?: string
  phone?: string
  email?: string
  roleCode: string
  status: string
  todayShiftDate?: string
  todayShiftStartTime?: string
  todayShiftEndTime?: string
  todayShiftStatus?: string
  activeLeaveStatus?: string
  activeLeaveType?: string
  activeLeaveStartDate?: string
  activeLeaveEndDate?: string
  activeLeaveReason?: string
}

export interface HireStaffPayload {
  username: string
  password: string
  nickname: string
  phone?: string
  email?: string
  roleCode: 'STAFF' | 'CAT_CARETAKER'
}

export interface GrantLeavePayload {
  leaveType: string
  startDate: string
  endDate: string
  reason?: string
}

export interface ManagerShiftQuery extends ManagerDateRangeQuery {
  userId?: number
}

export interface ManagerShiftRow {
  id: number
  userId: number
  username?: string
  nickname?: string
  roleCode: string
  shiftDate: string
  startTime: string
  endTime: string
  status: string
  remark?: string
}

export interface ManagerShiftPayload {
  userId: number
  roleCode?: string
  shiftDate: string
  startTime: string
  endTime: string
  status: string
  remark?: string
}

export interface ManagerActivityRow {
  activityStoreId: number
  activityId: number
  title: string
  type: string
  description?: string
  coverUrl?: string
  startAt: string
  endAt: string
  activityStatus: string
  acceptStatus: string
  handledAt?: string
  handleRemark?: string
}

export interface ActivityDecisionPayload {
  acceptStatus: string
  remark?: string
}

export interface ManagerDishQuery {
  categoryId?: number
  status?: string
}

export interface ManagerDishRow {
  id: number
  categoryId?: number
  name: string
  price: number
  stock?: number
  status: string
  description?: string
  imageUrl?: string
}

export interface UpdateDishPricePayload {
  newPrice: number
  reason?: string
}

export interface DishPriceHistoryRow {
  id: number
  dishId: number
  oldPrice: number
  newPrice: number
  changedBy?: number
  reason?: string
  createdAt?: string
}

export async function fetchManagerStore() {
  const { data } = await http.get<ApiResult<ManagerStoreInfo>>('/manager/store')
  return data.data
}

export async function updateManagerStore(payload: UpdateManagerStorePayload) {
  const { data } = await http.put<ApiResult<ManagerStoreInfo>>('/manager/store', payload)
  return data.data
}

export async function updateManagerStoreStatus(status: string) {
  const { data } = await http.put<ApiResult<void>>('/manager/store/status', { status })
  return data.data
}

export async function fetchManagerMetrics(params?: ManagerDateRangeQuery) {
  const { data } = await http.get<ApiResult<ManagerMetricsSummary>>('/manager/metrics', { params })
  return data.data
}

export async function fetchManagerTables() {
  const { data } = await http.get<ApiResult<ManagerTableRow[]>>('/manager/tables')
  return data.data
}

export async function createManagerTable(payload: ManagerTableRow) {
  const { data } = await http.post<ApiResult<ManagerTableRow>>('/manager/tables', payload)
  return data.data
}

export async function updateManagerTable(id: number, payload: ManagerTableRow) {
  const { data } = await http.put<ApiResult<ManagerTableRow>>(`/manager/tables/${id}`, payload)
  return data.data
}

export async function fetchManagerReservations(params?: ManagerReservationQuery) {
  const { data } = await http.get<ApiResult<ManagerReservationRow[]>>('/manager/reservations', { params })
  return data.data
}

export async function updateManagerReservationStatus(id: number, status: string) {
  const { data } = await http.put<ApiResult<ManagerReservationRow>>(`/manager/reservations/${id}/status`, { status })
  return data.data
}

export async function fetchManagerOrders(params?: ManagerOrderQuery) {
  const { data } = await http.get<ApiResult<ManagerOrderRow[]>>('/manager/orders', { params })
  return data.data
}

export async function fetchManagerOrderDetail(id: number) {
  const { data } = await http.get<ApiResult<ManagerOrderDetail>>(`/manager/orders/${id}`)
  return data.data
}

export async function fetchManagerCats(params?: { status?: string }) {
  const { data } = await http.get<ApiResult<ManagerCatStatusRow[]>>('/manager/cats', { params })
  return data.data
}

export async function fetchManagerStaff(params?: ManagerStaffQuery) {
  const { data } = await http.get<ApiResult<ManagerStaffRow[]>>('/manager/staff', { params })
  return data.data
}

export async function hireManagerStaff(payload: HireStaffPayload) {
  const { data } = await http.post<ApiResult<ManagerStaffRow>>('/manager/staff', payload)
  return data.data
}

export async function dismissManagerStaff(userStoreRoleId: number, reason?: string) {
  const { data } = await http.put<ApiResult<void>>(`/manager/staff/${userStoreRoleId}/dismiss`, { reason })
  return data.data
}

export async function grantManagerStaffLeave(userId: number, payload: GrantLeavePayload) {
  const { data } = await http.post<ApiResult<void>>(`/manager/staff/${userId}/leave`, payload)
  return data.data
}

export async function fetchManagerShifts(params?: ManagerShiftQuery) {
  const { data } = await http.get<ApiResult<ManagerShiftRow[]>>('/manager/shifts', { params })
  return data.data
}

export async function createManagerShift(payload: ManagerShiftPayload) {
  const { data } = await http.post<ApiResult<ManagerShiftRow>>('/manager/shifts', payload)
  return data.data
}

export async function updateManagerShift(id: number, payload: ManagerShiftPayload) {
  const { data } = await http.put<ApiResult<ManagerShiftRow>>(`/manager/shifts/${id}`, payload)
  return data.data
}

export async function fetchManagerActivities(params?: { status?: string }) {
  const { data } = await http.get<ApiResult<ManagerActivityRow[]>>('/manager/activities', { params })
  return data.data
}

export async function decideManagerActivity(activityStoreId: number, payload: ActivityDecisionPayload) {
  const { data } = await http.put<ApiResult<ManagerActivityRow>>(`/manager/activities/${activityStoreId}/decision`, payload)
  return data.data
}

export async function fetchManagerDishes(params?: ManagerDishQuery) {
  const { data } = await http.get<ApiResult<ManagerDishRow[]>>('/manager/dishes', { params })
  return data.data
}

export async function updateManagerDishPrice(id: number, payload: UpdateDishPricePayload) {
  const { data } = await http.put<ApiResult<ManagerDishRow>>(`/manager/dishes/${id}/price`, payload)
  return data.data
}

export async function fetchManagerDishPriceHistory(id: number) {
  const { data } = await http.get<ApiResult<DishPriceHistoryRow[]>>(`/manager/dishes/${id}/price-history`)
  return data.data
}
