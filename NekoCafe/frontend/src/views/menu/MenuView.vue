<template>
  <section class="page-card menu-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">MENU BROWSE</p>
        <h1>猫咖菜单浏览</h1>
        <p>先看今日甜品与饮品，点单结算功能由订单模块后续接入。</p>
      </div>
      <el-select v-model="selectedStoreId" class="store-select" placeholder="选择门店" size="large" @change="loadMenu">
        <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
      </el-select>
    </header>

    <div v-loading="loading" class="menu-board">
      <el-empty v-if="!loading && categories.length === 0" description="请选择门店或等待菜单上新" />
      <section v-for="category in categories" :key="category.id" class="category-block">
        <div class="category-title">
          <h2>{{ category.name }}</h2>
          <span>{{ category.dishes.length }} 款</span>
        </div>
        <div class="dish-grid">
          <el-card v-for="dish in category.dishes" :key="dish.id" class="dish-card" shadow="hover">
            <div class="dish-cover">{{ dish.name.slice(0, 1) }}</div>
            <div class="dish-info">
              <h3>{{ dish.name }}</h3>
              <p>{{ dish.description || '今日猫咖限定小食，适合搭配预约时段慢慢享用。' }}</p>
              <div class="dish-meta">
                <strong>¥{{ Number(dish.price).toFixed(2) }}</strong>
                <el-tag size="small" :type="dish.stock > 0 ? 'success' : 'info'">库存 {{ dish.stock }}</el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStoreMenu, type MenuCategory } from '@/api/menu'
import { fetchStores, type StoreSummary } from '@/api/store'

const route = useRoute()
const loading = ref(false)
const stores = ref<StoreSummary[]>([])
const categories = ref<MenuCategory[]>([])
const selectedStoreId = ref<number>()

async function loadStores() {
  stores.value = await fetchStores()
  const queryStoreId = Number(route.query.storeId)
  selectedStoreId.value = stores.value.some((store) => store.id === queryStoreId) ? queryStoreId : stores.value[0]?.id
}

async function loadMenu() {
  if (!selectedStoreId.value) return
  loading.value = true
  try {
    categories.value = await fetchStoreMenu(selectedStoreId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '菜单加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await loadStores()
    await loadMenu()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '门店加载失败')
  }
})
</script>

<style scoped>
.menu-page { display: grid; gap: 22px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.1em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.store-select { width: 300px; }
.menu-board { min-height: 220px; display: grid; gap: 24px; }
.category-block { display: grid; gap: 14px; }
.category-title { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #f0cfaa; padding-bottom: 10px; }
.category-title h2 { margin: 0; color: #3b2618; }
.category-title span { color: #d97706; font-weight: 900; }
.dish-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.dish-card :deep(.el-card__body) { display: flex; gap: 14px; height: 100%; }
.dish-cover { flex: 0 0 62px; height: 62px; display: grid; place-items: center; border-radius: 18px; background: linear-gradient(135deg, #fed7aa, #fff7ed); color: #9a3412; font-size: 26px; font-weight: 900; }
.dish-info { display: grid; gap: 8px; flex: 1; }
.dish-info h3 { margin: 0; color: #3b2618; }
.dish-info p { margin: 0; color: #7c5f4a; line-height: 1.6; font-size: 13px; }
.dish-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.dish-meta strong { color: #d97706; font-size: 18px; }
@media (max-width: 720px) {
  .page-header { flex-direction: column; }
  .store-select { width: 100%; }
}
</style>
