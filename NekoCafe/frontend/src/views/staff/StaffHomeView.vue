<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 工作台</p>
        <h1>店员后台</h1>
        <p>查看今日预约、待处理订单、桌位与猫咪状态。</p>
      </div>
      <el-button type="primary" @click="loadData">刷新数据</el-button>
    </header>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card class="stat-card" @click="$router.push('/staff/check-in')">
          <strong>{{ reservations.length }}</strong>
          <span>今日预约</span>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" @click="$router.push('/staff/orders')">
          <strong>{{ orders.length }}</strong>
          <span>待处理订单</span>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" @click="$router.push('/staff/table-cat-status')">
          <strong>查看</strong>
          <span>桌位 / 猫咪</span>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" header="今日预约预览">
      <el-table :data="reservations" empty-text="暂无今日预约">
        <el-table-column prop="reservationNo" label="预约号" />
        <el-table-column prop="customerName" label="顾客" />
        <el-table-column prop="customerPhone" label="手机号" />
        <el-table-column prop="timeSlot" label="时段" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </el-card>

    <el-card class="section-card" header="待处理订单预览">
      <el-table :data="orders" empty-text="暂无待处理订单">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="summary" label="菜品摘要" />
        <el-table-column prop="tableNo" label="桌号" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchPendingOrders, fetchTodayReservations, type StaffOrderRow, type StaffReservationRow } from '@/api/staff'

const reservations = ref<StaffReservationRow[]>([])
const orders = ref<StaffOrderRow[]>([])

async function loadData() {
  try {
    reservations.value = await fetchTodayReservations()
    orders.value = await fetchPendingOrders()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '数据加载失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.stat-row strong { font-size: 28px; color: #3b2618; }
.stat-row span { color: #7c6554; }
.stat-card { cursor: pointer; transition: transform 0.15s; }
.stat-card:hover { transform: translateY(-2px); }
</style>
