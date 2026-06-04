<template>
  <section class="page-card forbidden-page">
    <div class="status-code">403</div>
    <h1>无访问权限</h1>
    <p>当前账号没有访问该页面的权限，请返回自己的角色首页，或退出后切换账号。</p>
    <div class="actions">
      <el-button type="primary" @click="goHome">返回我的首页</el-button>
      <el-button @click="logout">退出登录</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getDefaultHomeByRoles } from '@/router/permissions'

const router = useRouter()
const auth = useAuthStore()

function goHome() {
  router.push(getDefaultHomeByRoles(auth.roles))
}

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.forbidden-page {
  display: grid;
  justify-items: center;
  gap: 16px;
  padding: 72px 24px;
  text-align: center;
}
.status-code {
  color: #d97706;
  font-size: 72px;
  font-weight: 900;
  letter-spacing: -0.08em;
}
h1 {
  margin: 0;
  color: #3b2618;
  font-size: 32px;
}
p {
  max-width: 520px;
  margin: 0;
  color: #7c6554;
  line-height: 1.8;
}
.actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
</style>
