<template>
  <section class="auth-scene register-scene">
    <div class="ribbon ribbon-one">MEOW MEMBER</div>
    <div class="ribbon ribbon-two">PAW POINTS</div>
    <div class="orb orb-milk" />
    <div class="orb orb-matcha" />

    <div class="register-shell">
      <section class="story-card">
        <span class="eyebrow">New paws arrive</span>
        <h1>领取你的<br />猫爪会员卡</h1>
        <p>创建顾客账号后，系统会自动初始化会员账户。下次预约、点单、签到都能累计专属猫爪积分。</p>
        <div class="member-ticket">
          <div>
            <small>NekoCafé Card</small>
            <strong>Cat Paw Club</strong>
          </div>
          <span>🐱</span>
        </div>
      </section>

      <section class="register-card">
        <div class="card-heading">
          <span class="mini-pill">3 分钟开启猫咖之旅</span>
          <h2>注册账号</h2>
          <p>填写基础信息，成为 NekoCafé 顾客会员。</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model.trim="form.username" size="large" placeholder="momo_cat" clearable />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model.trim="form.nickname" size="large" placeholder="猫爪布丁" clearable />
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="手机号（可选）" prop="phone">
              <el-input v-model.trim="form.phone" size="large" placeholder="13800000000" clearable />
            </el-form-item>
            <el-form-item label="邮箱（可选）" prop="email">
              <el-input v-model.trim="form.email" size="large" placeholder="momo@example.com" clearable />
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" size="large" type="password" placeholder="至少 6 位" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" size="large" type="password" placeholder="再输入一次" show-password />
            </el-form-item>
          </div>

          <section class="preference-panel">
            <div class="preference-heading">
              <span class="mini-pill">个性化推荐</span>
              <p>选择你的口味、座位和猫咪互动偏好，后续点单和活动推荐会优先参考。</p>
            </div>
            <div v-for="group in preferenceGroups" :key="group.type" class="preference-group">
              <strong>{{ group.label }}</strong>
              <el-checkbox-group v-model="selectedPreferences[group.type]">
                <el-checkbox-button v-for="item in group.items" :key="item" :label="item">
                  {{ item }}
                </el-checkbox-button>
              </el-checkbox-group>
            </div>
          </section>

          <el-form-item prop="agreement" class="agreement-item">
            <el-checkbox v-model="form.agreement">
              我同意开通猫爪会员账户，并遵守 NekoCafé 预约规则
            </el-checkbox>
          </el-form-item>

          <el-button class="submit-button" type="primary" size="large" :loading="submitting" @click="submit">
            开通猫爪会员卡
          </el-button>
        </el-form>

        <p class="switch-text">
          已经有账号？
          <router-link :to="{ name: 'login', query: route.query.redirect ? { redirect: route.query.redirect } : {} }">
            立即登录
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
import { registerApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { getDefaultHomeByRoles } from '@/router/permissions'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreement: false,
})

const preferenceGroups = [
  { type: 'TASTE', label: '口味偏好', items: ['少糖', '少冰', '咖啡', '甜品', '茶饮'] },
  { type: 'CAT', label: '猫咪互动', items: ['安静猫咪', '活泼猫咪', '短毛猫', '长毛猫'] },
  { type: 'SEAT', label: '座位偏好', items: ['靠窗', '安静角落', '适合拍照'] },
  { type: 'ALLERGY', label: '注意事项', items: ['猫毛敏感', '乳制品', '坚果'] },
]

const selectedPreferences = reactive<Record<string, string[]>>({ TASTE: [], CAT: [], SEAT: [], ALLERGY: [] })

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,32}$/, message: '用户名需为 3-32 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称不能超过 64 位', trigger: 'blur' },
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度需为 6-72 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次密码输入不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  agreement: [
    {
      validator: (_rule, value, callback) => {
        if (!value) callback(new Error('请先同意预约规则'))
        else callback()
      },
      trigger: 'change',
    },
  ],
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data = await registerApi({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        phone: form.phone || undefined,
        email: form.email || undefined,
        preferences: Object.entries(selectedPreferences).flatMap(([preferenceType, values]) =>
          values.map((preferenceValue) => ({ preferenceType, preferenceValue })),
        ),
      })
      auth.setAuth(data)
      ElMessage.success('猫爪会员卡已开通，欢迎加入 NekoCafé')
      const roleHome = getDefaultHomeByRoles(data.user.roles)
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      const target = redirect ? router.resolve(redirect) : null
      await router.push(target && target.name !== 'forbidden' && (!target.meta.roles || target.meta.roles.some((role) => data.user.roles.includes(role))) ? redirect : roleHome)
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '注册失败，请稍后重试')
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
    linear-gradient(120deg, rgba(255, 248, 240, 0.94), rgba(255, 230, 207, 0.96)),
    repeating-linear-gradient(45deg, rgba(217, 119, 6, 0.06) 0 1px, transparent 1px 18px);
  padding: 44px;
}
.register-shell {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(260px, 0.86fr) minmax(420px, 1.14fr);
  gap: 42px;
  align-items: center;
  min-height: 700px;
}
.story-card {
  border-radius: 34px;
  padding: 42px;
  background: #3b2618;
  color: #fff8f0;
  box-shadow: 0 28px 70px rgba(59, 38, 24, 0.26);
  animation: rise-in 0.7s ease both;
}
.eyebrow,
.mini-pill {
  display: inline-flex;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}
.eyebrow {
  background: rgba(255, 255, 255, 0.11);
  color: #ffcf9e;
}
.story-card h1 {
  margin: 28px 0 18px;
  font-size: clamp(40px, 6vw, 68px);
  line-height: 0.95;
  letter-spacing: -0.06em;
}
.story-card p {
  color: rgba(255, 248, 240, 0.78);
  font-size: 17px;
  line-height: 1.8;
}
.member-ticket {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 34px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 26px;
  padding: 22px;
  background: linear-gradient(135deg, rgba(255, 183, 161, 0.22), rgba(217, 119, 6, 0.2));
}
.member-ticket small {
  display: block;
  margin-bottom: 8px;
  color: rgba(255, 248, 240, 0.62);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.member-ticket strong {
  font-size: 24px;
}
.member-ticket span {
  font-size: 42px;
}
.register-card {
  border: 1px solid rgba(255, 255, 255, 0.76);
  border-radius: 34px;
  padding: 36px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 30px 80px rgba(100, 54, 16, 0.16);
  backdrop-filter: blur(22px);
  animation: card-pop 0.7s ease 0.06s both;
}
.mini-pill {
  background: #fff0dc;
  color: #d97706;
}
.card-heading h2 {
  margin: 14px 0 8px;
  color: #3b2618;
  font-size: 34px;
  letter-spacing: -0.04em;
}
.card-heading p {
  margin: 0 0 24px;
  color: rgba(59, 38, 24, 0.62);
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.auth-form :deep(.el-input__wrapper) {
  border-radius: 16px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px rgba(217, 119, 6, 0.12) inset;
}
.preference-panel {
  margin: 6px 0 18px;
  border-radius: 22px;
  padding: 18px;
  background: rgba(255, 240, 220, 0.62);
}
.preference-heading p {
  margin: 10px 0 16px;
  color: rgba(59, 38, 24, 0.62);
  line-height: 1.7;
}
.preference-group {
  margin-top: 14px;
}
.preference-group strong {
  display: block;
  margin-bottom: 10px;
  color: #3b2618;
}
.preference-group :deep(.el-checkbox-button__inner) {
  border-radius: 999px;
  margin: 0 8px 8px 0;
  border-left: 1px solid var(--el-border-color);
}
.agreement-item {
  margin-top: 4px;
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
.switch-text {
  margin: 22px 0 0;
  text-align: center;
  color: rgba(59, 38, 24, 0.66);
}
a {
  color: #d97706;
  font-weight: 900;
  text-decoration: none;
}
.ribbon {
  position: absolute;
  z-index: 1;
  border-radius: 999px;
  padding: 10px 18px;
  background: rgba(255, 255, 255, 0.5);
  color: rgba(59, 38, 24, 0.34);
  font-weight: 900;
  letter-spacing: 0.16em;
}
.ribbon-one { left: 8%; top: 8%; transform: rotate(-10deg); }
.ribbon-two { right: 8%; bottom: 10%; transform: rotate(8deg); }
.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(4px);
  animation: floaty 7s ease-in-out infinite;
}
.orb-milk {
  right: -110px;
  top: -70px;
  width: 300px;
  height: 300px;
  background: rgba(255, 183, 161, 0.36);
}
.orb-matcha {
  left: 42%;
  bottom: -120px;
  width: 260px;
  height: 260px;
  background: rgba(143, 191, 135, 0.22);
  animation-delay: -3s;
}
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
  50% { transform: translateY(-18px); }
}
@media (max-width: 980px) {
  .auth-scene { padding: 28px; }
  .register-shell { grid-template-columns: 1fr; min-height: auto; }
}
@media (max-width: 640px) {
  .auth-scene { padding: 18px; border-radius: 24px; }
  .story-card,
  .register-card { padding: 28px 22px; }
  .form-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
