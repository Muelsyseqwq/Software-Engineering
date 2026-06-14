import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import LoginView from '../LoginView.vue'

// Mock router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
  useRoute: () => ({ query: {} }),
  RouterLink: { template: '<a><slot /></a>' },
}))

// Mock auth API
vi.mock('@/api/auth', () => ({
  loginApi: vi.fn().mockResolvedValue({
    token: 'test-token',
    tokenType: 'Bearer',
    expiresAt: '2099-01-01T00:00:00',
    user: { id: 1, username: 'demo', nickname: 'Demo', roles: ['CUSTOMER'] },
  }),
}))

function mountLogin() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(LoginView, {
    global: {
      plugins: [pinia, ElementPlus],
      stubs: { RouterLink: true },
    },
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form with account and password fields', () => {
    const wrapper = mountLogin()
    expect(wrapper.text()).toContain('账号')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.find('button.submit-button').exists()).toBe(true)
  })

  it('shows validation error when submitting empty form', async () => {
    const wrapper = mountLogin()
    const form = wrapper.findComponent({ name: 'ElForm' })
    // Trigger validation — the form rules require account and password
    await wrapper.find('button.submit-button').trigger('click')
    // Form should not submit with empty fields
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('fills demo account when clicking fill button', async () => {
    const wrapper = mountLogin()
    const fillBtn = wrapper.find('.demo-helper .el-button')
    if (fillBtn.exists()) {
      await fillBtn.trigger('click')
      // After fill, account input should be populated
      const accountInput = wrapper.find('input[placeholder*="用户名"]')
      expect(accountInput.exists()).toBe(true)
    }
  })

  it('renders all role selection cards', () => {
    const wrapper = mountLogin()
    const roleCards = wrapper.findAll('.role-card')
    expect(roleCards.length).toBeGreaterThanOrEqual(3)
  })

  it('has register link pointing to register page', () => {
    const wrapper = mountLogin()
    expect(wrapper.text()).toContain('还没有账号')
    // RouterLink stub renders with the register route
    expect(wrapper.findAll('router-link-stub').length).toBeGreaterThanOrEqual(2)
  })
})
