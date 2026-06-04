<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR 数据看板</p>
        <h1>数据看板</h1>
        <p>展示预约量、订单量、营业额和平台概览。组员可继续接入图表和统计接口。</p>
      </div>
      <el-button type="primary" @click="loadSummary">刷新</el-button>
    </header>

    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in statItems" :key="item.label" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="图表区域预留">
      <el-empty description="可在这里接入 ECharts：近 7 日预约数、门店订单量、营业额趋势" />
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDashboardSummary, type DashboardSummary } from '@/api/dashboard'

const summary = ref<DashboardSummary>({
  reservationCount: 0,
  orderCount: 0,
  revenue: 0,
  userCount: 0,
  storeCount: 0,
  catCount: 0,
})

const statItems = computed(() => [
  { label: '今日预约数', value: summary.value.reservationCount },
  { label: '今日订单数', value: summary.value.orderCount },
  { label: '今日营业额', value: `¥${summary.value.revenue}` },
  { label: '注册用户数', value: summary.value.userCount },
  { label: '门店数量', value: summary.value.storeCount },
  { label: '猫咪数量', value: summary.value.catCount },
])

async function loadSummary() {
  try {
    summary.value = await fetchDashboardSummary()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '数据看板接口待接入')
  }
}

onMounted(loadSummary)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.stat-row strong { font-size: 26px; color: #3b2618; }
.stat-row span { color: #7c6554; }
</style>
