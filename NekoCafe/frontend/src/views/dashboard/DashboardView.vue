<template>
  <section class="page-card ops-dashboard">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 运营数据看板</p>
        <h1>运营罗盘</h1>
        <p>查看各门店营收、坪效、翻台率与会员复购率，支持跨门店分析。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedStoreId" placeholder="全部门店" clearable style="width:200px" @change="onStoreChange">
          <el-option :value="undefined" label="🌐 全部门店" />
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- period tabs -->
    <div class="period-bar">
      <el-radio-group v-model="period" @change="onPeriodChange" size="default">
        <el-radio-button value="WEEK">近一周</el-radio-button>
        <el-radio-button value="MONTH">近一月</el-radio-button>
        <el-radio-button value="YEAR">近一年</el-radio-button>
        <el-radio-button value="ALL">开店以来</el-radio-button>
      </el-radio-group>
      <span class="period-hint">{{ periodLabel }}</span>
    </div>

    <!-- KPI cards -->
    <el-row :gutter="16" class="kpi-row">
      <el-col v-for="kpi in kpiCards" :key="kpi.label" :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">{{ kpi.value }}</div>
          <div class="kpi-unit" v-if="kpi.unit">{{ kpi.unit }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- charts row 1: 营收趋势 & 坪效趋势 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card header="营收趋势">
          <div ref="revenueChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card header="坪效趋势 (元/㎡)">
          <div ref="perSqmChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <!-- charts row 2: 翻台率 & 会员复购率 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card header="翻台率趋势">
          <div ref="turnoverChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card header="会员复购率趋势">
          <div ref="repurchaseChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <!-- cross-store comparison -->
    <el-card header="跨门店对比分析" class="cross-store-card">
      <el-table :data="crossStoreData" border stripe v-loading="crossLoading" empty-text="暂无数据">
        <el-table-column prop="storeName" label="门店" min-width="120" fixed />
        <el-table-column prop="city" label="城市" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="营收(元)" width="120" sortable prop="revenue">
          <template #default="{ row }">¥{{ fmtNum(row.revenue) }}</template>
        </el-table-column>
        <el-table-column label="订单数" width="90" sortable prop="orderCount" />
        <el-table-column label="预约数" width="90" sortable prop="reservationCount" />
        <el-table-column label="坪效(元/㎡)" width="120" sortable prop="revenuePerSqm">
          <template #default="{ row }">¥{{ fmtNum(row.revenuePerSqm) }}</template>
        </el-table-column>
        <el-table-column label="翻台率" width="100" sortable prop="turnoverRate">
          <template #default="{ row }">{{ fmtPct(row.turnoverRate) }}</template>
        </el-table-column>
        <el-table-column label="会员复购率" width="120" sortable prop="repurchaseRate">
          <template #default="{ row }">{{ fmtPct(row.repurchaseRate) }}</template>
        </el-table-column>
        <el-table-column label="面积(㎡)" width="90" prop="areaSquareMeter">
          <template #default="{ row }">{{ fmtNum(row.areaSquareMeter) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="drillToStore(row.storeId)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  fetchOverview,
  fetchCrossStore,
  fetchOperatorTrend,
  type DashboardPeriodSummary,
  type CrossStoreRow,
  type DashboardTrendPoint,
  type StoreSummaryRow,
  fetchStoreSummaries,
} from '@/api/dashboard'
import type { AdminStoreRow } from '@/api/admin'
import { fetchAdminStores } from '@/api/admin'

// ---- state ----
const period = ref('WEEK')
const selectedStoreId = ref<number | undefined>(undefined)
const overview = ref<DashboardPeriodSummary | null>(null)
const crossStoreData = ref<CrossStoreRow[]>([])
const crossLoading = ref(false)
const stores = ref<AdminStoreRow[]>([])

// ---- charts ----
const revenueChartRef = ref<HTMLDivElement | null>(null)
const perSqmChartRef = ref<HTMLDivElement | null>(null)
const turnoverChartRef = ref<HTMLDivElement | null>(null)
const repurchaseChartRef = ref<HTMLDivElement | null>(null)

let revenueChart: echarts.ECharts | null = null
let perSqmChart: echarts.ECharts | null = null
let turnoverChart: echarts.ECharts | null = null
let repurchaseChart: echarts.ECharts | null = null

const periodLabel = computed(() => {
  if (overview.value) {
    return `${overview.value.start} 至 ${overview.value.end}`
  }
  return ''
})

const kpiCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { label: '营收', value: `¥${fmtNum(o.revenue)}`, unit: '' },
    { label: '订单数', value: String(o.orderCount), unit: '' },
    { label: '预约数', value: String(o.reservationCount), unit: '' },
    { label: '客单价', value: `¥${fmtNum(o.averageOrderValue)}`, unit: '' },
    { label: '坪效', value: `¥${fmtNum(o.revenuePerSqm)}`, unit: '/㎡' },
    { label: '翻台率', value: fmtPct(o.turnoverRate), unit: '' },
    { label: '会员复购率', value: fmtPct(o.repurchaseRate), unit: '' },
  ]
})

// ---- lifecycle ----
onMounted(async () => {
  await loadStores()
  await loadAll()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})

// ---- data loading ----
async function loadStores() {
  try {
    stores.value = await fetchAdminStores()
  } catch { /* silent */ }
}

async function loadAll() {
  await Promise.all([loadOverview(), loadCrossStore(), loadTrends()])
}

async function loadOverview() {
  try {
    overview.value = await fetchOverview(period.value, selectedStoreId.value)
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载概览失败')
  }
}

async function loadCrossStore() {
  crossLoading.value = true
  try {
    crossStoreData.value = await fetchCrossStore(period.value)
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载跨门店数据失败')
  } finally {
    crossLoading.value = false
  }
}

async function loadTrends() {
  const storeId = selectedStoreId.value
  const p = period.value
  try {
    const [revenue, perSqm, turnover, repurchase] = await Promise.all([
      fetchOperatorTrend(p, 'REVENUE', storeId),
      fetchOperatorTrend(p, 'REVENUE_PER_SQM', storeId),
      fetchOperatorTrend(p, 'TURNOVER_RATE', storeId),
      fetchOperatorTrend(p, 'REPURCHASE_RATE', storeId),
    ])
    await nextTick()
    renderRevenueChart(revenue)
    renderPerSqmChart(perSqm)
    renderTurnoverChart(turnover)
    renderRepurchaseChart(repurchase)
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载趋势数据失败')
  }
}

// ---- chart rendering ----
function initChart(refEl: HTMLDivElement | null, existing: echarts.ECharts | null): echarts.ECharts | null {
  if (!refEl) return null
  if (existing) {
    existing.dispose()
  }
  return echarts.init(refEl)
}

function makeLineOption(data: DashboardTrendPoint[], name: string, color: string) {
  return {
    grid: { left: 40, right: 20, top: 20, bottom: 28 },
    xAxis: { type: 'category', data: data.map(d => d.label) },
    yAxis: { type: 'value' },
    tooltip: { trigger: 'axis' },
    series: [{
      name,
      type: 'line',
      smooth: true,
      data: data.map(d => d.value),
      areaStyle: { color: color.replace('1)', '0.15)').replace('rgb', 'rgba') },
      lineStyle: { color },
      itemStyle: { color },
    }],
  }
}

function makeBarOption(data: DashboardTrendPoint[], name: string, color: string) {
  return {
    grid: { left: 40, right: 20, top: 20, bottom: 28 },
    xAxis: { type: 'category', data: data.map(d => d.label) },
    yAxis: { type: 'value' },
    tooltip: { trigger: 'axis' },
    series: [{
      name,
      type: 'bar',
      data: data.map(d => d.value),
      itemStyle: { color, borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 28,
    }],
  }
}

function renderRevenueChart(data: DashboardTrendPoint[]) {
  revenueChart = initChart(revenueChartRef.value, revenueChart)
  if (revenueChart) {
    revenueChart.setOption(makeLineOption(data, '营收(元)', '#d97706'))
  }
}

function renderPerSqmChart(data: DashboardTrendPoint[]) {
  perSqmChart = initChart(perSqmChartRef.value, perSqmChart)
  if (perSqmChart) {
    perSqmChart.setOption(makeLineOption(data, '坪效(元/㎡)', '#9a3412'))
  }
}

function renderTurnoverChart(data: DashboardTrendPoint[]) {
  turnoverChart = initChart(turnoverChartRef.value, turnoverChart)
  if (turnoverChart) {
    turnoverChart.setOption(makeBarOption(data, '翻台率(%)', '#f59e0b'))
  }
}

function renderRepurchaseChart(data: DashboardTrendPoint[]) {
  repurchaseChart = initChart(repurchaseChartRef.value, repurchaseChart)
  if (repurchaseChart) {
    repurchaseChart.setOption(makeLineOption(data, '复购率(%)', '#6f945d'))
  }
}

function resizeCharts() {
  revenueChart?.resize()
  perSqmChart?.resize()
  turnoverChart?.resize()
  repurchaseChart?.resize()
}

function disposeCharts() {
  revenueChart?.dispose()
  perSqmChart?.dispose()
  turnoverChart?.dispose()
  repurchaseChart?.dispose()
  revenueChart = null
  perSqmChart = null
  turnoverChart = null
  repurchaseChart = null
}

// ---- event handlers ----
function onPeriodChange() {
  loadAll()
}

function onStoreChange() {
  loadAll()
}

function drillToStore(storeId: number) {
  selectedStoreId.value = storeId
  loadAll()
}

// ---- helpers ----
function fmtNum(v?: number | string): string {
  if (v === undefined || v === null) return '0'
  const n = typeof v === 'string' ? parseFloat(v) : v
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function fmtPct(v?: number | string): string {
  if (v === undefined || v === null) return '0%'
  const n = typeof v === 'string' ? parseFloat(v) : v
  if (isNaN(n)) return '0%'
  return `${Math.round(n * 100)}%`
}

function statusLabel(s: string): string {
  const map: Record<string, string> = {
    OPEN: '营业中',
    CLOSED: '歇业',
    PREPARING: '筹备中',
  }
  return map[s] || s
}
</script>

<style scoped>
.ops-dashboard { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.header-actions { display: flex; gap: 10px; align-items: center; }

.period-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}
.period-hint { color: #7c6554; font-size: 13px; }

.kpi-row { margin-top: 4px; }
.kpi-card { text-align: center; }
.kpi-card :deep(.el-card__body) { padding: 18px 12px; }
.kpi-label { font-size: 13px; color: #7c6554; margin-bottom: 6px; }
.kpi-value { font-size: 24px; font-weight: 800; color: #3b2618; }
.kpi-unit { font-size: 12px; color: #8b6c55; margin-top: 2px; }

.chart-row .el-card { height: 100%; }
.chart-box { width: 100%; height: 300px; }
.cross-store-card { overflow-x: auto; }
</style>
