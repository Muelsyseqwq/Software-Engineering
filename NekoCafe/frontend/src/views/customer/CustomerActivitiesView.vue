<template>
  <section class="page-card activities-page">
    <header class="page-header">
      <div><p class="eyebrow">EVENTS & MOMENTS</p><h1>顾客活动中心</h1><p>查看运营发布、门店已接受的优惠活动与娱乐活动。</p></div>
      <div class="filters">
        <el-select v-model="type" placeholder="活动类型" clearable size="large" @change="loadActivities">
          <el-option label="优惠活动" value="PROMOTION" />
          <el-option label="娱乐活动" value="ENTERTAINMENT" />
        </el-select>
        <el-select v-model="storeId" placeholder="适用门店" clearable size="large" @change="loadActivities">
          <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
        </el-select>
      </div>
    </header>

    <section v-loading="loading" class="section-card">
      <el-empty v-if="!loading && activities.length === 0" description="暂无符合条件的活动" />
      <div class="activity-wall">
        <article v-for="activity in activities" :key="activity.id" class="event-card">
          <div class="event-cover">{{ activity.type === 'ENTERTAINMENT' ? '🎪' : '🎁' }}</div>
          <div class="event-body">
            <div class="event-meta"><el-tag effect="plain">{{ activity.typeText }}</el-tag><span>{{ formatDate(activity.startAt) }} - {{ formatDate(activity.endAt) }}</span></div>
            <h2>{{ activity.title }}</h2>
            <p>{{ activity.description || '欢迎到店参与活动，和猫咪一起度过治愈时光。' }}</p>
            <div class="store-chips">
              <el-tag v-for="store in activity.stores" :key="store.storeId" type="warning" effect="light">{{ store.storeName }}</el-tag>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCustomerActivities, type CustomerActivityResponse } from '@/api/customer'
import { fetchStores, type StoreSummary } from '@/api/store'

const loading = ref(false)
const activities = ref<CustomerActivityResponse[]>([])
const stores = ref<StoreSummary[]>([])
const type = ref<string>()
const storeId = ref<number>()

function formatDate(value: string) { return value ? value.slice(0, 10) : '待定' }
async function loadActivities() {
  loading.value = true
  try { activities.value = await fetchCustomerActivities({ type: type.value, storeId: storeId.value }) } catch (error) { ElMessage.error(error instanceof Error ? error.message : '活动加载失败') } finally { loading.value = false }
}
onMounted(async () => {
  try { stores.value = await fetchStores(); await loadActivities() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '页面加载失败') }
})
</script>

<style scoped>
.filters { display: flex; gap: 12px; flex-wrap: wrap; justify-content: flex-end; }
.filters .el-select { width: 180px; }
.activity-wall { display: grid; grid-template-columns: repeat(auto-fit, minmax(290px, 1fr)); gap: 18px; }
.event-card { overflow: hidden; border-radius: 28px; border: 1px solid rgba(217,119,6,0.12); background: rgba(255,255,255,0.78); box-shadow: 0 18px 42px rgba(100,54,16,0.1); }
.event-cover { display: grid; place-items: center; min-height: 130px; background: radial-gradient(circle at 20% 20%, #fff7ed, #fed7aa 48%, #f59e0b); font-size: 52px; }
.event-body { padding: 22px; }
.event-meta { display: flex; align-items: center; justify-content: space-between; gap: 10px; color: rgba(59,38,24,0.55); font-size: 13px; }
.event-body h2 { margin: 16px 0 8px; color: #3b2618; }
.event-body p { color: rgba(59,38,24,0.68); line-height: 1.8; }
.store-chips { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px; }
</style>
