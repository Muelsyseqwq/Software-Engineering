<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">总部运营 / 猫咖总控台</p>
        <h1>运营罗盘</h1>
        <p>展示预约量、订单量、营业额和平台概览，支持总部统一查看跨店经营状态。</p>
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

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card header="近 7 日营业额">
          <div ref="revenueChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card header="近 7 日预约数">
          <div ref="reservationChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchDashboardReservations, fetchDashboardRevenue, fetchDashboardSummary, type DashboardSummary, type DashboardTrendPoint } from '@/api/dashboard'

const summary = ref<DashboardSummary>({
  reservationCount: 0,
  orderCount: 0,
  revenue: 0,
  userCount: 0,
  storeCount: 0,
  catCount: 0,
})

const revenueData = ref<DashboardTrendPoint[]>([])
const reservationData = ref<DashboardTrendPoint[]>([])
const revenueChartRef = ref<HTMLDivElement | null>(null)
const reservationChartRef = ref<HTMLDivElement | null>(null)
let revenueChart: echarts.ECharts | null = null
let reservationChart: echarts.ECharts | null = null

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

async function loadTrends() {
  try {
    const [revenue, reservations] = await Promise.all([
      fetchDashboardRevenue(),
      fetchDashboardReservations(),
    ])
    revenueData.value = revenue
    reservationData.value = reservations
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '图表数据接口待接入')
  }
}

function initCharts() {
  if (revenueChartRef.value && !revenueChart) {
    revenueChart = echarts.init(revenueChartRef.value)
  }
  if (reservationChartRef.value && !reservationChart) {
    reservationChart = echarts.init(reservationChartRef.value)
  }
}

function renderCharts() {
  initCharts()
  if (revenueChart) {
    revenueChart.setOption({
      grid: { left: 32, right: 20, top: 20, bottom: 28 },
      xAxis: { type: 'category', data: revenueData.value.map((item) => item.label) },
      yAxis: { type: 'value' },
      tooltip: { trigger: 'axis' },
      series: [
        {
          name: '营业额',
          type: 'line',
          smooth: true,
          data: revenueData.value.map((item) => item.value),
          areaStyle: { color: 'rgba(212, 116, 63, 0.2)' },
          lineStyle: { color: '#d97706' },
          itemStyle: { color: '#d97706' },
        },
      ],
    })
  }
  if (reservationChart) {
    reservationChart.setOption({
      grid: { left: 32, right: 20, top: 20, bottom: 28 },
      xAxis: { type: 'category', data: reservationData.value.map((item) => item.label) },
      yAxis: { type: 'value' },
      tooltip: { trigger: 'axis' },
      series: [
        {
          name: '预约数',
          type: 'bar',
          data: reservationData.value.map((item) => item.value),
          itemStyle: { color: '#9a3412' },
          barMaxWidth: 28,
        },
      ],
    })
  }
}

function resizeCharts() {
  revenueChart?.resize()
  reservationChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initCharts()
  loadSummary()
  loadTrends()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  revenueChart?.dispose()
  reservationChart?.dispose()
  revenueChart = null
  reservationChart = null
})
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.stat-row strong { font-size: 26px; color: #3b2618; }
.stat-row span { color: #7c6554; }
.chart-row .el-card { height: 100%; }
.chart-box { width: 100%; height: 280px; }
</style>
