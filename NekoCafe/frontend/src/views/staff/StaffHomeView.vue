<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 工作台</p>
        <h1>店员后台</h1>
        <p>查看今日预约、桌位状态和待处理订单。此页面由店员角色负责人继续完善。</p>
      </div>
      <el-button type="primary" @click="loadData">刷新数据</el-button>
    </header>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8"><el-card><strong>{{ reservations.length }}</strong><span>今日预约</span></el-card></el-col>
      <el-col :span="8"><el-card><strong>{{ orders.length }}</strong><span>待处理订单</span></el-card></el-col>
      <el-col :span="8"><el-card><strong>待接入</strong><span>桌位状态</span></el-card></el-col>
    </el-row>

    <el-card class="section-card" header="今日预约预览">
      <el-table :data="reservations" empty-text="暂无预约数据，组员可在 staff.ts 接口接入后端">
        <el-table-column prop="reservationNo" label="预约号" />
        <el-table-column prop="customerName" label="顾客" />
        <el-table-column prop="customerPhone" label="手机号" />
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
    ElMessage.warning(error instanceof Error ? error.message : '店员数据待后端接入')
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
</style>
