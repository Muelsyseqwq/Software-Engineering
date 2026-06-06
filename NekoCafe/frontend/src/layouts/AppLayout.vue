<template>
  <el-container class="app-layout">
    <el-aside width="286px" class="aside">
      <div class="brand-card">
        <div class="brand-mark">🐾</div>
        <div>
          <strong>NekoCafé</strong>
          <span>猫咪晨光工作台</span>
        </div>
      </div>

      <div class="user-card">
        <span class="user-avatar">{{ avatarText }}</span>
        <div>
          <strong>{{ auth.user?.nickname || '游客猫爪' }}</strong>
          <p>{{ roleSummary }}</p>
        </div>
      </div>

      <el-menu router :default-active="route.path" class="nav-menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.hint }}</small>
          </span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <el-button v-if="auth.isLoggedIn" round class="logout-button" @click="handleLogout">退出登录</el-button>
        <router-link v-else to="/login" class="login-link">登录角色账号</router-link>
      </div>
    </el-aside>
    <el-main class="main-stage">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { filterMenusByRoles } from '@/router/permissions'
import type { RoleCode } from '@/types/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menus = computed(() => filterMenusByRoles(auth.roles))

const roleLabels: Record<RoleCode, string> = {
  CUSTOMER: '猫爪会员',
  STAFF: '前台店员',
  CAT_CARETAKER: '猫咪管家',
  STORE_MANAGER: '门店店长',
  HQ_OPERATOR: '总部运营',
  ADMIN: '总部运营',
}

const roleSummary = computed(() => {
  if (!auth.roles.length) return '访客可浏览公开门店与菜单'
  return auth.roles.map((role) => roleLabels[role] ?? role).join(' / ')
})

const avatarText = computed(() => auth.user?.nickname?.slice(0, 1) || '猫')

async function handleLogout() {
  auth.logout()
  await router.push('/login')
}
</script>

<style scoped>
.app-layout {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 14%, rgba(255, 183, 161, 0.36), transparent 30%),
    radial-gradient(circle at 92% 10%, rgba(143, 191, 135, 0.22), transparent 26%),
    linear-gradient(135deg, #fff8f0 0%, #ffe8d1 48%, #fff4e8 100%);
}
.app-layout::before {
  content: '';
  position: absolute;
  right: -90px;
  bottom: 10%;
  width: 320px;
  height: 320px;
  border-radius: 999px;
  background: rgba(217, 119, 6, 0.14);
  filter: blur(4px);
}
.aside {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: 18px 0 18px 18px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 30px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: 0 30px 80px rgba(100, 54, 16, 0.16);
  backdrop-filter: blur(22px);
}
.brand-card,
.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 22px;
  padding: 14px;
  background: rgba(255, 250, 244, 0.7);
  box-shadow: 0 12px 28px rgba(122, 73, 28, 0.08);
}
.brand-mark,
.user-avatar {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 17px;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff7ed;
  font-size: 20px;
  font-weight: 900;
  box-shadow: 0 12px 26px rgba(217, 119, 6, 0.24);
}
.user-avatar {
  background: linear-gradient(135deg, #6f945d, #a7c78f);
}
.brand-card strong,
.user-card strong {
  display: block;
  color: #3b2618;
  font-size: 18px;
  letter-spacing: -0.02em;
}
.brand-card span,
.user-card p {
  display: block;
  margin: 3px 0 0;
  color: #7c5f4a;
  font-size: 12px;
  line-height: 1.5;
}
.nav-menu {
  flex: 1;
  border: none;
  background: transparent;
}
.nav-menu :deep(.el-menu-item) {
  height: auto;
  min-height: 62px;
  align-items: center;
  gap: 12px;
  margin: 8px 0;
  border-radius: 18px;
  padding: 10px 12px !important;
  color: #5d3922;
  transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}
.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-menu-item.is-active) {
  transform: translateX(3px);
  background:
    radial-gradient(circle at 92% 18%, rgba(245, 158, 11, 0.18), transparent 30%),
    rgba(255, 255, 255, 0.78);
  box-shadow: 0 14px 34px rgba(122, 73, 28, 0.12);
  color: #d97706;
}
.menu-icon {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: rgba(217, 119, 6, 0.1);
  font-size: 16px;
}
.menu-copy {
  display: grid;
  gap: 2px;
  line-height: 1.2;
}
.menu-copy strong {
  color: #3b2618;
  font-size: 14px;
}
.nav-menu :deep(.el-menu-item.is-active) .menu-copy strong {
  color: #d97706;
}
.menu-copy small {
  color: #8b6c55;
  font-size: 11px;
}
.aside-footer {
  display: grid;
  gap: 10px;
}
.logout-button,
.login-link {
  width: 100%;
  border: none;
  background: rgba(59, 38, 24, 0.08);
  color: #5d3922;
  font-weight: 900;
}
.login-link {
  display: grid;
  place-items: center;
  height: 38px;
  border-radius: 999px;
}
.main-stage {
  position: relative;
  z-index: 1;
  padding: 18px 24px 24px;
}
@media (max-width: 860px) {
  .app-layout {
    display: block;
  }
  .aside {
    width: auto !important;
    margin: 12px;
  }
  .nav-menu {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 8px;
  }
  .nav-menu :deep(.el-menu-item) {
    margin: 0;
  }
  .main-stage {
    padding: 0 12px 16px;
  }
}
</style>
