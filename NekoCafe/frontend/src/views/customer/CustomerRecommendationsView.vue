<template>
  <section class="page-card recommendation-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">AI RECOMMENDATION</p>
        <h1>智能推荐猫咖体验</h1>
        <p>根据你的位置、会员偏好、历史行为、菜品、猫咪和活动数据，生成可解释的推荐理由。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="locating" @click="loadWithCurrentLocation">📍 结合当前位置</el-button>
        <el-button type="primary" :loading="loading" @click="loadRecommendations(lastLocation || undefined)">刷新推荐</el-button>
      </div>
    </header>

    <el-alert class="summary-card" type="success" show-icon :closable="false">
      <template #title>{{ feed?.summary || '正在为你生成智能推荐...' }}</template>
      <template #default>
        <span v-if="locationLabel">推荐已结合：{{ locationLabel }}</span>
        <span v-else>未授权定位时，也会根据偏好、菜品、猫咪和活动数据生成推荐。</span>
      </template>
    </el-alert>

    <div v-loading="loading" class="recommendation-grid">
      <el-card v-for="item in feed?.items || []" :key="item.storeId" class="recommendation-card" shadow="hover">
        <template #header>
          <div class="card-title">
            <div>
              <p class="rank">TOP {{ item.rank }}</p>
              <h2>{{ item.storeName }}</h2>
              <span>{{ item.businessArea || item.city }} · {{ item.address }}</span>
            </div>
            <div class="score-box">
              <strong>{{ item.score }}</strong>
              <span>推荐分</span>
            </div>
          </div>
        </template>

        <div class="tag-row">
          <el-tag v-for="tag in item.tags" :key="tag" round>{{ tag }}</el-tag>
          <el-tag :type="item.status === 'OPEN' ? 'success' : 'info'" round>{{ statusText(item.status) }}</el-tag>
          <el-tag v-if="item.distanceKm != null" type="warning" round>距你 {{ item.distanceKm }} km</el-tag>
        </div>

        <div class="reason-block">
          <h3>AI 风格推荐理由</h3>
          <ul>
            <li v-for="reason in item.reasons" :key="reason">{{ reason }}</li>
          </ul>
        </div>

        <div class="highlight-grid">
          <div class="highlight-section">
            <h3>推荐菜品</h3>
            <p v-if="item.dishHighlights.length === 0" class="muted">暂无菜品亮点</p>
            <span v-for="dish in item.dishHighlights" :key="dish.id" class="highlight-pill">🍰 {{ dish.name }}</span>
          </div>
          <div class="highlight-section">
            <h3>可互动猫咪</h3>
            <p v-if="item.catHighlights.length === 0" class="muted">暂无猫咪亮点</p>
            <span v-for="cat in item.catHighlights" :key="cat.id" class="highlight-pill">🐈 {{ cat.name }}</span>
          </div>
          <div class="highlight-section wide">
            <h3>活动提示</h3>
            <p v-if="item.activityHighlights.length === 0" class="muted">暂无活动亮点</p>
            <span v-for="activity in item.activityHighlights" :key="activity.id" class="highlight-pill">🎉 {{ activity.name }}</span>
          </div>
        </div>

        <div class="actions">
          <el-button @click="router.push(`/stores/${item.storeId}`)">查看门店</el-button>
          <el-button type="primary" :disabled="item.status !== 'OPEN'" @click="router.push(`/reservations/new?storeId=${item.storeId}`)">
            {{ item.primaryActionText }}
          </el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && (feed?.items.length || 0) === 0" description="暂时没有生成推荐，完善会员偏好后再试试" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchCustomerRecommendations, type RecommendationFeedResponse, type RecommendationParams } from '@/api/recommendation'

const router = useRouter()
const loading = ref(false)
const locating = ref(false)
const feed = ref<RecommendationFeedResponse | null>(null)
const locationLabel = ref('')
const lastLocation = ref<RecommendationParams | null>(null)

function statusText(status: string) {
  return status === 'OPEN' ? '营业中' : '暂未营业'
}

async function loadRecommendations(params?: RecommendationParams) {
  loading.value = true
  try {
    feed.value = await fetchCustomerRecommendations({ ...params, limit: 6 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能推荐加载失败')
  } finally {
    loading.value = false
  }
}

function loadWithCurrentLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位，将使用无位置推荐')
    void loadRecommendations()
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (position) => {
      locating.value = false
      const params = { lat: position.coords.latitude, lng: position.coords.longitude }
      lastLocation.value = params
      locationLabel.value = '浏览器当前位置'
      void loadRecommendations(params)
    },
    () => {
      locating.value = false
      lastLocation.value = null
      locationLabel.value = ''
      ElMessage.warning('定位未授权，已使用偏好与热度生成推荐')
      void loadRecommendations()
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 },
  )
}

onMounted(() => loadRecommendations())
</script>

<style scoped>
.recommendation-page { display: grid; gap: 20px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
.header-actions { display: flex; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.08em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.summary-card { border-radius: 14px; }
.recommendation-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 18px; min-height: 180px; }
.recommendation-card { border-radius: 20px; border-color: #f5dfc5; }
.card-title { display: flex; justify-content: space-between; gap: 14px; align-items: flex-start; }
.rank { margin: 0 0 6px; color: #d97706; font-weight: 900; letter-spacing: 0.06em; }
.card-title h2 { margin: 0 0 6px; color: #3b2618; font-size: 20px; }
.card-title span { color: #8a6a52; font-size: 13px; }
.score-box { min-width: 72px; padding: 10px 12px; border-radius: 16px; background: #fff7ed; text-align: center; color: #92400e; }
.score-box strong { display: block; font-size: 24px; line-height: 1; }
.score-box span { font-size: 12px; font-weight: 800; }
.tag-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.reason-block { padding: 14px 16px; border-radius: 16px; background: #fffaf3; color: #5d3922; }
.reason-block h3, .highlight-section h3 { margin: 0 0 10px; color: #3b2618; font-size: 15px; }
.reason-block ul { margin: 0; padding-left: 18px; line-height: 1.8; }
.highlight-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin: 16px 0; }
.highlight-section { display: flex; flex-direction: column; gap: 8px; padding: 12px; border: 1px solid #f5dfc5; border-radius: 16px; }
.highlight-section.wide { grid-column: 1 / -1; }
.highlight-pill { display: inline-flex; align-items: center; width: fit-content; max-width: 100%; padding: 6px 10px; border-radius: 999px; background: #fef3c7; color: #6b4d37; font-weight: 800; font-size: 13px; }
.muted { margin: 0; color: #9a7a60; font-size: 13px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 720px) {
  .page-header { flex-direction: column; }
  .highlight-grid { grid-template-columns: 1fr; }
  .actions { justify-content: flex-start; flex-wrap: wrap; }
}
</style>
