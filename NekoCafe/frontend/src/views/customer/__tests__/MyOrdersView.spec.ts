import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'

const { mockOrders } = vi.hoisted(() => ({
  mockOrders: [
    {
      id: 1, orderNo: 'O001', storeId: 1, storeName: 'NekoCafe朝阳店',
      totalAmount: 50, status: 'CREATED', refundStatus: 'NONE',
      canPay: true, canRefund: false, canReview: false, canCancel: true, reviewed: false,
      items: [{ dishName: '猫爪蛋糕', quantity: 2 }],
      createdAt: '2026-06-13T10:00:00',
    },
    {
      id: 2, orderNo: 'O002', storeId: 1, storeName: 'NekoCafe朝阳店',
      totalAmount: 80, status: 'PAID', refundStatus: 'NONE',
      canPay: false, canRefund: true, canReview: false, canCancel: false, reviewed: false,
      items: [{ dishName: '拿铁', quantity: 1 }],
      createdAt: '2026-06-13T11:00:00', paidAt: '2026-06-13T11:05:00',
    },
    {
      id: 3, orderNo: 'O003', storeId: 1, storeName: 'NekoCafe朝阳店',
      totalAmount: 120, status: 'COMPLETED', refundStatus: 'NONE',
      canPay: false, canRefund: false, canReview: true, canCancel: false, reviewed: false,
      items: [{ dishName: '三文鱼定食', quantity: 1 }],
      createdAt: '2026-06-12T10:00:00', completedAt: '2026-06-12T11:00:00',
    },
    {
      id: 4, orderNo: 'O004', storeId: 1, storeName: 'NekoCafe朝阳店',
      totalAmount: 96, status: 'COMPLETED', refundStatus: 'NONE',
      canPay: false, canRefund: false, canReview: false, canCancel: false, reviewed: true,
      reviewRating: 4, reviewContent: '猫猫很亲人，拿铁很好喝', reviewCreatedAt: '2026-06-12T12:00:00',
      items: [{ dishName: '猫咪拿铁', quantity: 2 }],
      createdAt: '2026-06-12T09:00:00', completedAt: '2026-06-12T10:30:00',
    },
  ],
}))

vi.mock('@/api/order', () => ({
  fetchMyOrders: vi.fn().mockResolvedValue(mockOrders),
  cancelOrder: vi.fn().mockResolvedValue({ ...mockOrders[0], status: 'CANCELLED', canCancel: false }),
}))

vi.mock('@/api/payment', () => ({
  sandboxPay: vi.fn().mockResolvedValue({ status: 'SUCCESS' }),
}))

vi.mock('@/api/customer', () => ({
  applyRefund: vi.fn().mockResolvedValue({ status: 'APPLIED' }),
  createReview: vi.fn().mockResolvedValue({ status: 'VISIBLE' }),
  fetchMyRefunds: vi.fn().mockResolvedValue([]),
}))

import MyOrdersView from '../MyOrdersView.vue'

function mountOrders() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(MyOrdersView, {
    global: {
      plugins: [pinia, ElementPlus],
    },
  })
}

describe('MyOrdersView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders orders after mount', async () => {
    const wrapper = mountOrders()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('我的订单')
  })

  it('shows cancel button for CREATED orders', async () => {
    const wrapper = mountOrders()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('取消订单')
  })

  it('shows refund button for PAID orders', async () => {
    const wrapper = mountOrders()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('申请退款')
  })

  it('shows review button for COMPLETED orders', async () => {
    const wrapper = mountOrders()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('去评价')
  })

  it('shows submitted review details for already-reviewed orders', async () => {
    const wrapper = mountOrders()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    const reviewedOrder = wrapper.findAll('.order-card').find((card) => card.text().includes('O004'))
    expect(reviewedOrder?.text()).toContain('已评价')
    expect(reviewedOrder?.text()).toContain('我的评价')
    expect(reviewedOrder?.text()).toContain('猫猫很亲人，拿铁很好喝')
    expect(reviewedOrder?.text()).toContain('评价时间：2026-06-12 12:00')
    expect(reviewedOrder?.text()).not.toContain('去评价')
  })

  it('shows empty state when no orders', async () => {
    const { fetchMyOrders } = await import('@/api/order')
    vi.mocked(fetchMyOrders).mockResolvedValueOnce([])
    const wrapper = mountOrders()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('暂无订单')
  })
})
