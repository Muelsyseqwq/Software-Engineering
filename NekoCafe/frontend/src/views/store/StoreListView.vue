<template>
  <section class="page-card store-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">CUSTOMER 门店浏览</p>
        <h1>选择今日猫咖据点</h1>
        <p>先看看营业时间、桌位余量和门店氛围，再挑一个适合撸猫的时段。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadStores">刷新门店</el-button>
    </header>

    <el-input v-model.trim="keyword" class="search-box" size="large" placeholder="搜索城市、门店或地址" clearable />

    <div v-loading="loading" class="store-grid">
      <el-card v-for="store in filteredStores" :key="store.id" class="store-card" shadow="hover">
        <template #header>
          <div class="store-title">
            <div>
              <h2>{{ store.name }}</h2>
              <span>{{ store.city }} · {{ store.address }}</span>
            </div>
            <el-tag :type="store.status === 'OPEN' ? 'success' : 'info'">{{ statusText(store.status) }}</el-tag>
          </div>
        </template>
        <p class="description">{{ store.description || '这家店还没有填写介绍，但猫咪已经在等你。' }}</p>
        <div class="meta-list">
          <span>🕙 {{ formatTime(store.openingTime) }} - {{ formatTime(store.closingTime) }}</span>
          <span>📞 {{ store.phone || '暂无电话' }}</span>
          <span>🪑 可预约桌位 {{ store.availableTableCount }} 张</span>
        </div>
        <div class="actions">
          <el-button @click="router.push(`/stores/${store.id}`)">查看详情</el-button>
          <el-button type="primary" @click="router.push(`/reservations/new?storeId=${store.id}`)">立即预约</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && filteredStores.length === 0" description="暂时没有匹配的门店" />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStores, type StoreSummary } from '@/api/store'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const stores = ref<StoreSummary[]>([])

const filteredStores = computed(() => {
  const key = keyword.value.toLowerCase()
  if (!key) return stores.value
  return stores.value.filter((store) => [store.name, store.city, store.address].some((item) => item.toLowerCase().includes(key)))
})

function formatTime(value: string) {
  return value?.slice(0, 5) || '--:--'
}

function statusText(status: string) {
  return status === 'OPEN' ? '营业中' : '休息中'
}

async function loadStores() {
  loading.value = true
  try {
    stores.value = await fetchStores()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '门店列表加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStores)
</script>

<style scoped>
.store-page { display: grid; gap: 20px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.08em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.search-box { max-width: 520px; }
.store-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 18px; min-height: 160px; }
.store-card { border-radius: 18px; border-color: #f5dfc5; }
.store-title { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.store-title h2 { margin: 0 0 6px; color: #3b2618; font-size: 20px; }
.store-title span { color: #8a6a52; font-size: 13px; }
.description { min-height: 44px; color: #6b4d37; line-height: 1.7; }
.meta-list { display: grid; gap: 8px; margin: 16px 0; color: #5d3922; font-weight: 700; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 640px) {
  .page-header { flex-direction: column; }
  .actions { justify-content: flex-start; flex-wrap: wrap; }
}
</style>
