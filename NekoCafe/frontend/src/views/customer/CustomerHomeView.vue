<template>
  <section v-loading="loading" class="page-card customer-home">
    <header class="page-header hero-header">
      <div>
        <p class="eyebrow">CUSTOMER CLUB</p>
        <h1>今天也来猫咖充个电吧</h1>
        <p>这里汇总你的会员积分、订单进度和近期活动，方便快速进入顾客流程。</p>
      </div>
      <div class="points-badge">
        <span>猫爪积分</span>
        <strong>{{ home?.points.points ?? 0 }}</strong>
        <small>{{ levelText(home?.points.levelCode) }}</small>
      </div>
    </header>

    <div class="stat-grid">
      <article class="stat-card"><span>待支付</span><strong>{{ home?.orderStats.pendingPayment ?? 0 }}</strong><small>还未完成付款</small></article>
      <article class="stat-card"><span>制作中</span><strong>{{ home?.orderStats.preparing ?? 0 }}</strong><small>店员正在准备</small></article>
      <article class="stat-card"><span>可评价</span><strong>{{ home?.orderStats.reviewable ?? 0 }}</strong><small>留下猫爪反馈</small></article>
      <article class="stat-card"><span>可退款</span><strong>{{ home?.orderStats.refundable ?? 0 }}</strong><small>支持申请售后</small></article>
    </div>

    <div class="home-grid">
      <section class="section-card">
        <div class="section-heading">
          <div><p class="eyebrow">QUICK PAWS</p><h2>快捷入口</h2></div>
        </div>
        <div class="quick-actions">
          <router-link to="/stores">☕ 门店浏览</router-link>
          <router-link to="/reservations/new">🐾 创建预约</router-link>
          <router-link to="/orders/checkout">🧾 点单结算</router-link>
          <router-link to="/customer/orders">📦 我的订单</router-link>
          <router-link to="/customer/profile">🎫 会员偏好</router-link>
        </div>
      </section>

      <section class="section-card">
        <div class="section-heading">
          <div><p class="eyebrow">RECENT ORDERS</p><h2>最近订单</h2></div>
          <el-button round @click="$router.push('/customer/orders')">查看全部</el-button>
        </div>
        <el-empty v-if="!home?.recentOrders.length" description="暂无订单，先去点单吧" />
        <div v-else class="order-stack">
          <article v-for="order in home.recentOrders" :key="order.id" class="mini-order">
            <div><strong>{{ order.orderNo }}</strong><span>{{ order.storeName }} · ¥{{ Number(order.totalAmount).toFixed(2) }}</span></div>
            <el-tag :type="statusTag(order.status)">{{ statusText(order.status, order.refundStatus) }}</el-tag>
          </article>
        </div>
      </section>
    </div>

    <section class="section-card activity-strip">
      <div class="section-heading">
        <div><p class="eyebrow">CAFE EVENTS</p><h2>首页活动</h2></div>
        <el-button type="primary" round @click="$router.push('/customer/activities')">活动中心</el-button>
      </div>
      <el-empty v-if="!home?.activities.length" description="暂无已发布活动" />
      <div v-else class="activity-grid">
        <article v-for="activity in home.activities" :key="activity.id" class="activity-card">
          <div class="activity-cover">{{ activity.type === 'ENTERTAINMENT' ? '🎪' : '🎁' }}</div>
          <div>
            <el-tag effect="plain">{{ activity.typeText }}</el-tag>
            <h3>{{ activity.title }}</h3>
            <p>{{ activity.description || '到店体验更多猫咖惊喜。' }}</p>
            <small>{{ activity.stores.map((store) => store.storeName).join(' / ') }}</small>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCustomerHome, type CustomerHomeResponse } from '@/api/customer'

const loading = ref(false)
const home = ref<CustomerHomeResponse>()

function statusText(status: string, refundStatus?: string) {
  if (refundStatus && refundStatus !== 'NONE') return refundStatus === 'APPLIED' ? '退款申请中' : '退款处理中'
  return ({ CREATED: '待支付', PAID: '已支付，待制作', PREPARING: '制作中', COMPLETED: '已完成', CANCELLED: '已取消' } as Record<string, string>)[status] || status
}
function statusTag(status: string) {
  return ({ CREATED: 'warning', PAID: 'primary', PREPARING: 'warning', COMPLETED: 'success', CANCELLED: 'info' } as Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'>)[status] || 'info'
}
function levelText(level?: string) {
  return level === 'VIP' ? 'VIP 会员' : 'NORMAL 会员'
}
async function loadHome() {
  loading.value = true
  try { home.value = await fetchCustomerHome() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '顾客首页加载失败') } finally { loading.value = false }
}
onMounted(loadHome)
</script>

<style scoped>
.customer-home { position: relative; overflow: hidden; }
.hero-header { align-items: center; }
.points-badge { min-width: 160px; border-radius: 28px; padding: 22px; background: linear-gradient(135deg, #3b2618, #8a4b17); color: #fff8f0; text-align: center; box-shadow: 0 22px 46px rgba(59, 38, 24, 0.2); }
.points-badge span, .points-badge small { display: block; opacity: 0.72; font-weight: 800; }
.points-badge strong { display: block; font-size: 42px; line-height: 1.1; }
.home-grid { display: grid; grid-template-columns: 0.9fr 1.1fr; gap: 22px; margin-top: 22px; }
.quick-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.quick-actions a { border-radius: 18px; padding: 18px; background: rgba(255, 240, 220, 0.75); color: #7c3f12; font-weight: 900; text-decoration: none; }
.order-stack { display: grid; gap: 12px; }
.mini-order { display: flex; justify-content: space-between; gap: 14px; align-items: center; border-radius: 18px; padding: 14px 16px; background: rgba(255,255,255,0.68); }
.mini-order span { display: block; margin-top: 4px; color: rgba(59,38,24,0.58); }
.activity-strip { margin-top: 22px; }
.activity-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.activity-card { display: grid; grid-template-columns: 74px 1fr; gap: 14px; border-radius: 22px; padding: 18px; background: linear-gradient(135deg, rgba(255,255,255,0.82), rgba(255,240,220,0.62)); }
.activity-cover { display: grid; place-items: center; border-radius: 22px; background: #fff0dc; font-size: 34px; }
.activity-card h3 { margin: 10px 0 6px; color: #3b2618; }
.activity-card p { margin: 0 0 8px; color: rgba(59,38,24,0.62); line-height: 1.7; }
.activity-card small { color: #d97706; font-weight: 800; }
@media (max-width: 900px) { .home-grid { grid-template-columns: 1fr; } .quick-actions { grid-template-columns: 1fr; } .hero-header { align-items: flex-start; } }
</style>
