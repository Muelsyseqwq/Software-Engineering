<template>
  <section class="page-card detail-page" v-loading="loading">
    <el-empty v-if="!loading && !store" description="门店不存在或暂时不可用" />

    <template v-else-if="store">
      <header class="hero-card">
        <div>
          <p class="eyebrow">STORE DETAIL</p>
          <h1>{{ store.name }}</h1>
          <p>{{ store.description || '一间适合慢慢喝咖啡、轻轻撸猫的 NekoCafé 门店。' }}</p>
          <div class="hero-meta">
            <el-tag type="success">{{ statusText(store.status) }}</el-tag>
            <span>{{ store.city }} · {{ store.address }}</span>
            <span>{{ formatTime(store.openingTime) }} - {{ formatTime(store.closingTime) }}</span>
          </div>
        </div>
      </header>

      <section class="action-panel">
        <div class="section-title">
          <h2>进店后的行动面板</h2>
          <span>已选定 {{ store.name }}，下面入口会自动携带当前门店</span>
        </div>
        <div class="action-grid">
          <button type="button" class="action-card" @click="go(`/menu?storeId=${store.id}`)"><span>🍰</span><strong>查看菜单</strong><small>先看看本店甜品饮品</small></button>
          <button type="button" class="action-card primary" @click="go(`/reservations/new?storeId=${store.id}`)"><span>🐾</span><strong>预约此门店</strong><small>锁定猫咪互动时段</small></button>
          <button type="button" class="action-card" @click="go(`/reservations/queue?storeId=${store.id}`)"><span>🎟️</span><strong>满座排队取号</strong><small>现场等位时领取号码</small></button>
          <button type="button" class="action-card" @click="go(`/orders/checkout?storeId=${store.id}`)"><span>🧾</span><strong>选好菜去结算</strong><small>确认订单并支付</small></button>
        </div>
      </section>

      <section>
        <div class="section-title">
          <h2>桌位概览</h2>
          <span>按区域挑选适合的猫咖角落</span>
        </div>
        <el-table :data="store.tables" border empty-text="暂无桌位数据">
          <el-table-column prop="tableNo" label="桌号" width="120" />
          <el-table-column prop="area" label="区域" />
          <el-table-column prop="capacity" label="容纳人数" width="120" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'info'">{{ tableStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStoreDetail, type StoreDetail } from '@/api/store'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const store = ref<StoreDetail>()

function formatTime(value: string) {
  return value?.slice(0, 5) || '--:--'
}

function statusText(status: string) {
  return status === 'OPEN' ? '营业中' : '休息中'
}

function tableStatusText(status: string) {
  return status === 'AVAILABLE' ? '可预约' : '暂不可用'
}

function go(path: string) {
  router.push(path)
}

async function loadDetail() {
  loading.value = true
  try {
    store.value = await fetchStoreDetail(String(route.params.id))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '门店详情加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page { display: grid; gap: 24px; }
.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  border-radius: 22px;
  padding: 26px;
  background: linear-gradient(135deg, #fff7ed, #fff);
  border: 1px solid #f5dfc5;
}
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.12em; }
.hero-card h1 { margin: 0 0 10px; color: #3b2618; }
.hero-card p { margin: 0; color: #74523a; line-height: 1.8; }
.hero-meta { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-top: 16px; color: #6b4d37; font-weight: 700; }
.action-panel { border: 1px solid #f5dfc5; border-radius: 24px; padding: 22px; background: #fffaf4; }
.action-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.action-card { cursor: pointer; border: 1px solid #f5dfc5; border-radius: 20px; padding: 18px; background: rgba(255,255,255,0.72); text-align: left; display: grid; gap: 8px; color: #3b2618; transition: transform 0.18s ease, box-shadow 0.18s ease; }
.action-card:hover { transform: translateY(-2px); box-shadow: 0 14px 30px rgba(146, 64, 14, 0.12); }
.action-card.primary { background: linear-gradient(135deg, #d97706, #92400e); color: #fff8f0; }
.action-card span { font-size: 30px; }
.action-card strong { font-size: 17px; }
.action-card small { color: inherit; opacity: 0.72; line-height: 1.5; }
.section-title { display: flex; justify-content: space-between; align-items: end; margin-bottom: 14px; }
.section-title h2 { margin: 0; color: #3b2618; }
.section-title span { color: #8a6a52; }
@media (max-width: 980px) { .action-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 720px) {
  .hero-card, .section-title { flex-direction: column; align-items: flex-start; }
  .action-grid { grid-template-columns: 1fr; }
}
</style>
