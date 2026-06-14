import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'

const { mockFeed } = vi.hoisted(() => ({
  mockFeed: {
    generatedAt: '2026-06-13T10:00:00',
    summary: '根据你的偏好，为你推荐了 2 家猫咖门店',
    items: [
      {
        rank: 1, storeId: 1, storeName: 'NekoCafe朝阳店', city: '北京',
        businessArea: '国贸', address: '建国门外大街12号', status: 'OPEN',
        distanceKm: 1.2, score: 95,
        tags: ['人气最高', '甜品丰富'], reasons: ['距离最近', '偏好匹配：甜品'],
        dishHighlights: [{ id: 1, name: '猫爪蛋糕' }],
        catHighlights: [{ id: 1, name: '布偶猫奶盖' }],
        activityHighlights: [{ id: 1, name: '满100减20' }],
        primaryActionText: '立即预约',
      },
      {
        rank: 2, storeId: 2, storeName: 'NekoCafe三里屯店', city: '北京',
        businessArea: '三里屯', address: '绒球街6号', status: 'OPEN',
        distanceKm: 3.5, score: 82,
        tags: ['夜间模式'], reasons: ['偏好匹配：猫咪互动'],
        dishHighlights: [], catHighlights: [], activityHighlights: [],
        primaryActionText: '立即预约',
      },
    ],
  },
}))

vi.mock('@/api/recommendation', () => ({
  fetchCustomerRecommendations: vi.fn().mockResolvedValue(mockFeed),
  fetchCustomerCatRecommendations: vi.fn().mockResolvedValue({
    generatedAt: '2026-06-13T10:00:00',
    summary: '为你推荐了 3 只猫咪',
    items: [],
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
  RouterLink: { template: '<a><slot /></a>' },
}))

import CustomerRecommendationsView from '../CustomerRecommendationsView.vue'

function mountRecommendations() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(CustomerRecommendationsView, {
    global: {
      plugins: [pinia, ElementPlus],
      stubs: { RouterLink: true },
    },
  })
}

describe('CustomerRecommendationsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the recommendation page header', () => {
    const wrapper = mountRecommendations()
    expect(wrapper.text()).toContain('智能推荐猫咖体验')
  })

  it('shows summary alert when feed is loaded', async () => {
    const wrapper = mountRecommendations()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('根据你的偏好')
  })

  it('shows store cards with rank and score', async () => {
    const wrapper = mountRecommendations()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    const text = wrapper.text()
    expect(text).toContain('TOP 1')
    expect(text).toContain('NekoCafe朝阳店')
    expect(text).toContain('95')
  })

  it('shows empty state when no items', async () => {
    const { fetchCustomerRecommendations } = await import('@/api/recommendation')
    vi.mocked(fetchCustomerRecommendations).mockResolvedValueOnce({
      generatedAt: '', summary: '', items: [],
    })
    const wrapper = mountRecommendations()
    await new Promise(r => setTimeout(r, 100))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('暂时没有生成推荐')
  })

  it('shows cat recommendation tab', () => {
    const wrapper = mountRecommendations()
    expect(wrapper.text()).toContain('推荐猫咪')
  })
})
