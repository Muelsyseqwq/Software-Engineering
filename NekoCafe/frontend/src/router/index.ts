import { createRouter, createWebHistory } from 'vue-router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import AppLayout from '@/layouts/AppLayout.vue'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: PublicLayout,
      children: [
        { path: '', redirect: '/login' },
        { path: 'login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
        { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterView.vue') },
      ],
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: 'customer', name: 'customer-home', component: () => import('@/views/customer/CustomerHomeView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'customer/activities', name: 'customer-activities', component: () => import('@/views/customer/CustomerActivitiesView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'customer/orders', name: 'customer-orders', component: () => import('@/views/customer/MyOrdersView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'customer/profile', name: 'customer-profile', component: () => import('@/views/customer/CustomerProfileView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'stores', name: 'stores', component: () => import('@/views/store/StoreListView.vue') },
        { path: 'stores/:id', name: 'store-detail', component: () => import('@/views/store/StoreDetailView.vue') },
        { path: 'reservations/new', name: 'reservation-create', component: () => import('@/views/reservation/ReservationCreateView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'reservations/queue', name: 'queue-apply', component: () => import('@/views/reservation/QueueApplyView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'reservations/me', name: 'my-reservations', component: () => import('@/views/reservation/MyReservationsView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'menu', name: 'menu', component: () => import('@/views/menu/MenuView.vue') },
        { path: 'orders/checkout', name: 'checkout', component: () => import('@/views/order/CheckoutView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'staff', name: 'staff-home', component: () => import('@/views/staff/StaffHomeView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'staff/queue', name: 'staff-queue', component: () => import('@/views/staff/QueueManageView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'staff/check-in', name: 'staff-check-in', component: () => import('@/views/staff/ReservationCheckInView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'staff/orders', name: 'staff-orders', component: () => import('@/views/staff/OrderFulfillmentView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'staff/table-cat-status', name: 'staff-table-cat', component: () => import('@/views/staff/TableCatStatusView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'cats', name: 'cat-manage', component: () => import('@/views/cat/CatManageView.vue'), meta: { requiresAuth: true, roles: ['CAT_CARETAKER'] } },
        { path: 'manager', name: 'store-manager', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'overview' } },
        { path: 'manager/tables', name: 'store-manager-tables', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'tables' } },
        { path: 'manager/orders', name: 'store-manager-orders', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'orders' } },
        { path: 'manager/cats', name: 'store-manager-cats', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'cats' } },
        { path: 'manager/staff', name: 'store-manager-staff', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'staff' } },
        { path: 'manager/activities', name: 'store-manager-activities', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'activities' } },
        { path: 'manager/dishes', name: 'store-manager-dishes', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'], managerTab: 'dishes' } },
        // HQ_OPERATOR pages
        { path: 'dashboard', redirect: '/admin' },
        { path: 'admin', name: 'admin-overview', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR'] } },
        { path: 'admin/activities', name: 'admin-activities', component: () => import('@/views/admin/ActivityManageView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR'] } },
        { path: 'admin/store-managers', name: 'admin-store-managers', component: () => import('@/views/admin/StoreManagerManageView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR'] } },
        { path: 'admin/stores', name: 'admin-stores', component: () => import('@/views/admin/StoreManageView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR'] } },
        { path: 'admin/users', name: 'admin-users', component: () => import('@/views/admin/UserRoleView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR'] } },
        { path: '403', name: 'forbidden', component: () => import('@/views/error/ForbiddenView.vue') },
      ],
    },
  ],
})

setupRouterGuards(router)

export default router
