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
        <div class="hero-actions">
          <el-button @click="router.push(`/menu?storeId=${store.id}`)">查看菜单</el-button>
          <el-button type="primary" @click="router.push(`/reservations/new?storeId=${store.id}`)">预约此门店</el-button>
        </div>
      </header>

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
.hero-actions { display: flex; align-items: flex-start; gap: 10px; }
.section-title { display: flex; justify-content: space-between; align-items: end; margin-bottom: 14px; }
.section-title h2 { margin: 0; color: #3b2618; }
.section-title span { color: #8a6a52; }
@media (max-width: 720px) {
  .hero-card, .section-title { flex-direction: column; align-items: flex-start; }
  .hero-actions { flex-wrap: wrap; }
}
</style>
