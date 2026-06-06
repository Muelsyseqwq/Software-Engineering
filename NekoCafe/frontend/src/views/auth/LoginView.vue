<template>
  <section class="auth-scene login-scene">
    <div class="glow glow-caramel" />
    <div class="glow glow-peach" />
    <div class="paw paw-one"><span /><span /><span /><span /></div>
    <div class="paw paw-two"><span /><span /><span /><span /></div>

    <div class="auth-shell">
      <aside class="brand-panel">
        <div class="brand-badge">NekoCafé</div>
        <h1>欢迎回到<br />猫咪晨光餐桌</h1>
        <p>登录后继续预约猫咪互动座位、提前点单，并同步你的猫爪会员积分。</p>
        <div class="feature-list">
          <span>🐾 猫咪互动区预约</span>
          <span>☕ 甜品饮品提前点单</span>
          <span>🎁 会员积分自动累计</span>
        </div>
      </aside>

      <section class="auth-card cat-card">
        <div class="cat-ear left" />
        <div class="cat-ear right" />
        <div class="card-heading">
          <span class="eyebrow">Sign in with paws</span>
          <h2>按角色进入 NekoCafé</h2>
          <p>先挑一块猫咖值班牌，再用账号密码进入真实权限对应的工作台。</p>
        </div>

        <div class="role-dock" aria-label="选择登录角色入口">
          <button
            v-for="role in roleOptions"
            :key="role.code"
            class="role-card"
            :class="{ 'is-active': selectedRole === role.code }"
            type="button"
            @click="selectRole(role.code)"
          >
            <span class="role-mark">{{ role.badge }}</span>
            <strong>{{ role.name }}</strong>
            <small>{{ role.code }} · {{ role.homePath }}</small>
            <span>{{ role.description }}</span>
            <em>{{ role.demoAccount }}</em>
          </button>
        </div>

        <div class="demo-helper">
          <div>
            <strong>{{ selectedRoleInfo.name }}演示入口</strong>
            <span>{{ selectedRoleInfo.capability }}，账号 {{ selectedRoleInfo.demoAccount }}</span>
          </div>
          <el-button size="small" round @click="fillDemoAccount">填入账号</el-button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @keyup.enter="submit">
          <el-form-item label="账号" prop="account">
            <el-input v-model.trim="form.account" size="large" placeholder="用户名 / 手机号 / 邮箱" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" placeholder="请输入密码" show-password />
          </el-form-item>

          <div class="form-row">
            <el-checkbox v-model="rememberMe">记住这只猫爪</el-checkbox>
            <router-link to="/stores">游客先逛逛</router-link>
          </div>

          <el-button class="submit-button" type="primary" size="large" :loading="submitting" @click="submit">
            登录并预约
          </el-button>
        </el-form>

        <p class="switch-text">
          还没有账号？
          <router-link :to="{ name: 'register', query: route.query.redirect ? { redirect: route.query.redirect } : {} }">
            注册猫爪会员卡
          </router-link>
        </p>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { ROLE_HOME_MAP, canAccessRoute, getDefaultHomeByRoles } from '@/router/permissions'
import type { RoleCode } from '@/types/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const rememberMe = ref(true)
const selectedRole = ref<RoleCode>('CUSTOMER')

interface LoginRoleOption {
  code: RoleCode
  name: string
  badge: string
  description: string
  capability: string
  demoAccount: string
  homePath: string
}

const roleOptions: LoginRoleOption[] = [
  { code: 'CUSTOMER', name: '猫爪会员', badge: '🐾', description: '预约猫咪互动座位', capability: '浏览门店、预约座位、点单结算', demoAccount: 'demo_customer', homePath: '/stores' },
  { code: 'STAFF', name: '前台铃铛', badge: '🛎️', description: '处理签到与订单履约', capability: '查看今日预约、办理签到、推进订单', demoAccount: 'demo_staff', homePath: '/staff' },
  { code: 'CAT_CARETAKER', name: '猫咪健康簿', badge: '🐈', description: '维护猫咪档案', capability: '维护猫咪档案与健康状态', demoAccount: 'demo_cat', homePath: '/cats' },
  { code: 'STORE_MANAGER', name: '门店掌柜', badge: '🏠', description: '管理门店运营', capability: '查看桌位、门店状态和本店预约', demoAccount: 'demo_manager', homePath: '/manager' },
  { code: 'HQ_OPERATOR', name: '猫咖总控台', badge: '🧭', description: '总部运营与系统管理', capability: '查看跨店经营数据、活动运营、用户角色与平台配置', demoAccount: 'demo_hq', homePath: '/dashboard' },
]

const roleDisplayNames: Record<RoleCode, string> = {
  CUSTOMER: '猫爪会员',
  STAFF: '前台铃铛',
  CAT_CARETAKER: '猫咪健康簿',
  STORE_MANAGER: '门店掌柜',
  HQ_OPERATOR: '猫咖总控台',
  ADMIN: '猫咖总控台',
}

const selectedRoleInfo = computed(() => roleOptions.find((item) => item.code === selectedRole.value) ?? roleOptions[0])

const form = reactive({
  account: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  account: [{ required: true, message: '请输入用户名、手机号或邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

function selectRole(role: RoleCode) {
  selectedRole.value = role
}

function fillDemoAccount() {
  form.account = selectedRoleInfo.value.demoAccount
  ElMessage.info('已填入演示账号，演示密码请查看运行说明或向组长获取')
}

function getRoleName(role: RoleCode) {
  return roleDisplayNames[role] ?? role
}

function getLoginTarget(userRoles: RoleCode[]) {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  const target = redirect ? router.resolve(redirect) : null
  const canUseRedirect = Boolean(target && target.name !== 'forbidden' && canAccessRoute(userRoles, target.meta.roles))
  if (canUseRedirect) return redirect

  if (userRoles.includes(selectedRole.value)) {
    return ROLE_HOME_MAP[selectedRole.value]
  }

  return getDefaultHomeByRoles(userRoles)
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data = await loginApi(form)
      auth.setAuth(data)
      const selectedRoleName = getRoleName(selectedRole.value)
      const matchedSelectedRole = data.user.roles.includes(selectedRole.value)
      if (matchedSelectedRole) {
        ElMessage.success(`欢迎回到 NekoCafé，${data.user.nickname}，已进入「${selectedRoleName}」入口`)
      } else {
        const actualRoles = data.user.roles.map(getRoleName).join('、') || '未配置角色'
        ElMessage.warning(`当前账号实际角色为「${actualRoles}」，不具备「${selectedRoleName}」入口，已跳转至账号默认工作台`)
      }
      await router.push(getLoginTarget(data.user.roles))
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '登录失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.auth-scene {
  position: relative;
  min-height: calc(100vh - 64px);
  overflow: hidden;
  border-radius: 32px;
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 183, 161, 0.5), transparent 28%),
    radial-gradient(circle at 84% 12%, rgba(143, 191, 135, 0.28), transparent 26%),
    linear-gradient(135deg, #fff8f0 0%, #ffe8d1 48%, #fff4e8 100%);
  padding: 48px;
}
.auth-shell {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(360px, 440px);
  gap: 48px;
  align-items: center;
  min-height: 680px;
}
.brand-panel {
  color: #3b2618;
  animation: rise-in 0.7s ease both;
}
.brand-badge {
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(59, 38, 24, 0.12);
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.45);
  font-weight: 800;
  letter-spacing: 0.08em;
}
.brand-panel h1 {
  margin: 28px 0 18px;
  font-size: clamp(42px, 7vw, 76px);
  line-height: 0.95;
  letter-spacing: -0.06em;
}
.brand-panel p {
  max-width: 540px;
  color: rgba(59, 38, 24, 0.72);
  font-size: 18px;
  line-height: 1.8;
}
.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}
.feature-list span {
  border-radius: 18px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 14px 34px rgba(122, 73, 28, 0.1);
  color: #5d3922;
  font-weight: 700;
}
.auth-card {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 34px;
  padding: 38px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 30px 80px rgba(100, 54, 16, 0.18);
  backdrop-filter: blur(22px);
  animation: card-pop 0.7s ease 0.08s both;
}
.cat-ear {
  position: absolute;
  top: -18px;
  width: 58px;
  height: 58px;
  border-radius: 18px 18px 10px 18px;
  background: linear-gradient(135deg, #fff, #ffd7c3);
  transform: rotate(45deg);
  box-shadow: -8px -8px 20px rgba(118, 65, 22, 0.08);
}
.cat-ear.left { left: 54px; }
.cat-ear.right { right: 54px; transform: rotate(45deg) scaleX(-1); }
.card-heading .eyebrow {
  color: #d97706;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
.card-heading h2 {
  margin: 10px 0 8px;
  color: #3b2618;
  font-size: 32px;
  letter-spacing: -0.04em;
}
.card-heading p {
  margin: 0 0 24px;
  color: rgba(59, 38, 24, 0.62);
}
.role-dock {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 24px;
}
.role-card {
  position: relative;
  display: grid;
  gap: 4px;
  min-height: 104px;
  border: 1px solid rgba(217, 119, 6, 0.12);
  border-radius: 20px;
  padding: 14px 14px 12px;
  overflow: hidden;
  background:
    radial-gradient(circle at 88% 16%, rgba(255, 255, 255, 0.78), transparent 28%),
    rgba(255, 248, 240, 0.72);
  box-shadow: 0 12px 30px rgba(122, 73, 28, 0.08);
  color: #4a2d1b;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}
.role-card::after {
  content: '当前入口';
  position: absolute;
  right: 10px;
  top: 10px;
  border-radius: 999px;
  padding: 3px 8px;
  background: #d97706;
  color: #fff7ed;
  font-size: 10px;
  font-weight: 900;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.role-card:hover,
.role-card.is-active {
  transform: translateY(-2px);
  border-color: rgba(217, 119, 6, 0.46);
  background:
    radial-gradient(circle at 88% 16%, rgba(245, 158, 11, 0.2), transparent 32%),
    rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 38px rgba(217, 119, 6, 0.16);
}
.role-card.is-active::after {
  opacity: 1;
  transform: translateY(0);
}
.role-mark {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(217, 119, 6, 0.1);
  font-size: 18px;
}
.role-card strong {
  color: #3b2618;
  font-size: 15px;
}
.role-card small {
  color: #d97706;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
}
.role-card span:nth-of-type(2) {
  color: rgba(59, 38, 24, 0.58);
  font-size: 12px;
  line-height: 1.45;
}
.role-card em {
  width: fit-content;
  border-radius: 999px;
  padding: 3px 8px;
  background: rgba(143, 191, 135, 0.14);
  color: #537245;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
}
.demo-helper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -8px 0 22px;
  border: 1px solid rgba(143, 191, 135, 0.28);
  border-radius: 18px;
  padding: 12px 14px;
  background:
    radial-gradient(circle at 94% 20%, rgba(143, 191, 135, 0.18), transparent 36%),
    rgba(255, 255, 255, 0.62);
}
.demo-helper div {
  display: grid;
  gap: 4px;
}
.demo-helper strong {
  color: #3b2618;
  font-size: 14px;
}
.demo-helper span {
  color: rgba(59, 38, 24, 0.62);
  font-size: 12px;
  line-height: 1.5;
}
.auth-form :deep(.el-input__wrapper) {
  border-radius: 16px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px rgba(217, 119, 6, 0.12) inset;
}
.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 22px;
  font-size: 14px;
}
a {
  color: #d97706;
  font-weight: 800;
  text-decoration: none;
}
.submit-button {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 18px 34px rgba(217, 119, 6, 0.28);
  font-weight: 900;
}
.submit-button:hover {
  transform: translateY(-1px);
}
.switch-text {
  margin: 24px 0 0;
  text-align: center;
  color: rgba(59, 38, 24, 0.66);
}
.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(4px);
  opacity: 0.8;
  animation: floaty 6s ease-in-out infinite;
}
.glow-caramel {
  right: -120px;
  bottom: 80px;
  width: 320px;
  height: 320px;
  background: rgba(217, 119, 6, 0.18);
}
.glow-peach {
  left: 42%;
  top: -80px;
  width: 240px;
  height: 240px;
  background: rgba(255, 183, 161, 0.32);
  animation-delay: -2s;
}
.paw {
  position: absolute;
  width: 90px;
  height: 80px;
  opacity: 0.28;
  transform: rotate(-18deg);
}
.paw::after,
.paw span {
  content: '';
  position: absolute;
  border-radius: 999px;
  background: #8b5a2b;
}
.paw::after {
  left: 28px;
  bottom: 6px;
  width: 42px;
  height: 34px;
}
.paw span {
  width: 18px;
  height: 22px;
}
.paw span:nth-child(1) { left: 8px; top: 24px; }
.paw span:nth-child(2) { left: 28px; top: 4px; }
.paw span:nth-child(3) { left: 54px; top: 4px; }
.paw span:nth-child(4) { left: 74px; top: 24px; }
.paw-one { left: 8%; bottom: 12%; }
.paw-two { right: 8%; top: 16%; transform: rotate(22deg) scale(0.82); }
@keyframes rise-in {
  from { opacity: 0; transform: translateY(22px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes card-pop {
  from { opacity: 0; transform: translateY(28px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-16px); }
}
@media (max-width: 920px) {
  .auth-scene { padding: 28px; border-radius: 24px; }
  .auth-shell { grid-template-columns: 1fr; min-height: auto; gap: 28px; }
  .brand-panel h1 { font-size: 44px; }
}
@media (max-width: 560px) {
  .auth-scene { padding: 18px; }
  .auth-card { padding: 30px 22px; }
  .role-dock { grid-template-columns: 1fr; }
  .role-card { min-height: 92px; }
  .form-row { align-items: flex-start; flex-direction: column; gap: 8px; }
}
</style>
