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
        { path: '', redirect: '/stores' },
        { path: 'login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
        { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterView.vue') },
      ],
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: 'stores', name: 'stores', component: () => import('@/views/store/StoreListView.vue') },
        { path: 'stores/:id', name: 'store-detail', component: () => import('@/views/store/StoreDetailView.vue') },
        { path: 'reservations/new', name: 'reservation-create', component: () => import('@/views/reservation/ReservationCreateView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'reservations/me', name: 'my-reservations', component: () => import('@/views/reservation/MyReservationsView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'menu', name: 'menu', component: () => import('@/views/menu/MenuView.vue') },
        { path: 'orders/checkout', name: 'checkout', component: () => import('@/views/order/CheckoutView.vue'), meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
        { path: 'staff', name: 'staff-home', component: () => import('@/views/staff/StaffHomeView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'staff/check-in', name: 'staff-check-in', component: () => import('@/views/staff/ReservationCheckInView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'staff/orders', name: 'staff-orders', component: () => import('@/views/staff/OrderFulfillmentView.vue'), meta: { requiresAuth: true, roles: ['STAFF'] } },
        { path: 'cats', name: 'cat-manage', component: () => import('@/views/cat/CatManageView.vue'), meta: { requiresAuth: true, roles: ['CAT_CARETAKER'] } },
        { path: 'manager', name: 'store-manager', component: () => import('@/views/manager/StoreManagerView.vue'), meta: { requiresAuth: true, roles: ['STORE_MANAGER'] } },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { requiresAuth: true, roles: ['HQ_OPERATOR', 'ADMIN'] } },
        { path: 'admin', name: 'admin', component: () => import('@/views/admin/AdminHomeView.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
        { path: '403', name: 'forbidden', component: () => import('@/views/error/ForbiddenView.vue') },
      ],
    },
  ],
})

setupRouterGuards(router)

export default router
