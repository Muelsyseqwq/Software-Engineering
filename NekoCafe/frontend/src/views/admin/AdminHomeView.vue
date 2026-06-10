<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 运营总览</p>
        <h1>运营总览</h1>
        <p>查看各门店营收对比与今日平台概览数据。</p>
      </div>
      <el-button type="primary" @click="loadAll">刷新</el-button>
    </header>

    <!-- Global summary cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in statItems" :key="item.label" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </el-card>
      </el-col>
    </el-row>

    <!-- Per-store revenue chart -->
    <el-card header="各门店今日营收对比">
      <div ref="storeChartRef" class="chart-box" />
    </el-card>

    <!-- Per-store detail table -->
    <el-card header="各门店今日数据明细">
      <el-table :data="storeSummaries" border empty-text="暂无门店营收数据">
        <el-table-column prop="storeName" label="门店" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : row.status === 'PREPARING' ? 'warning' : 'info'">
              {{ row.status === 'OPEN' ? '营业中' : row.status === 'PREPARING' ? '筹备中' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="今日营收" width="120">
          <template #default="{ row }">¥{{ row.revenue }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="今日订单" width="100" />
        <el-table-column prop="reservationCount" label="今日预约" width="100" />
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchDashboardSummary, fetchStoreSummaries, type DashboardSummary, type StoreSummaryRow } from '@/api/dashboard'

const summary = ref<DashboardSummary>({ reservationCount: 0, orderCount: 0, revenue: 0, userCount: 0, storeCount: 0, catCount: 0 })
const storeSummaries = ref<StoreSummaryRow[]>([])
const storeChartRef = ref<HTMLDivElement | null>(null)
let storeChart: echarts.ECharts | null = null

const statItems = computed(() => [
  { label: '今日预约数', value: summary.value.reservationCount },
  { label: '今日订单数', value: summary.value.orderCount },
  { label: '今日营业额', value: `¥${summary.value.revenue}` },
  { label: '注册用户数', value: summary.value.userCount },
  { label: '门店数量', value: summary.value.storeCount },
  { label: '猫咪数量', value: summary.value.catCount },
])

async function loadAll() {
  try {
    summary.value = await fetchDashboardSummary()
    storeSummaries.value = await fetchStoreSummaries()
    await nextTick()
    renderChart()
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载运营数据失败')
  }
}

function renderChart() {
  if (!storeChartRef.value) return
  if (!storeChart) storeChart = echarts.init(storeChartRef.value)
  storeChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 32, right: 20, top: 20, bottom: 28 },
    xAxis: {
      type: 'category',
      data: storeSummaries.value.map(s => s.storeName),
      axisLabel: { rotate: 20 },
    },
    yAxis: { type: 'value' },
    series: [{
      name: '今日营收',
      type: 'bar',
      data: storeSummaries.value.map(s => s.revenue),
      itemStyle: { color: '#d97706' },
      barMaxWidth: 40,
    }],
  })
}

function resizeChart() { storeChart?.resize() }

onMounted(async () => {
  await loadAll()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  storeChart?.dispose()
  storeChart = null
})
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.stat-row strong { font-size: 26px; color: #3b2618; }
.stat-row span { color: #7c6554; }
.chart-box { width: 100%; height: 300px; }
</style>
