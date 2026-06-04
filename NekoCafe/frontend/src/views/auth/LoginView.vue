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
          <h2>登录 NekoCafé</h2>
          <p>输入用户名、手机号或邮箱，开启今日猫咖预约。</p>
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
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { getDefaultHomeByRoles } from '@/router/permissions'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const rememberMe = ref(true)

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

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data = await loginApi(form)
      auth.setAuth(data)
      ElMessage.success(`欢迎回到 NekoCafé，${data.user.nickname}`)
      const roleHome = getDefaultHomeByRoles(data.user.roles)
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      const target = redirect ? router.resolve(redirect) : null
      await router.push(target && target.name !== 'forbidden' && (!target.meta.roles || target.meta.roles.some((role) => data.user.roles.includes(role))) ? redirect : roleHome)
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
  margin: 0 0 26px;
  color: rgba(59, 38, 24, 0.62);
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
  .form-row { align-items: flex-start; flex-direction: column; gap: 8px; }
}
</style>
