<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 签到</p>
        <h1>预约签到</h1>
        <p>店员为顾客办理到店签到和入座。可查看预约备注和时段信息。</p>
      </div>
      <el-button type="primary" @click="loadReservations">刷新预约</el-button>
    </header>

    <el-table :data="reservations" border empty-text="暂无今日预约">
      <el-table-column prop="reservationNo" label="预约号" min-width="140" />
      <el-table-column prop="customerName" label="顾客姓名" />
      <el-table-column prop="customerPhone" label="手机号" min-width="130" />
      <el-table-column prop="partySize" label="人数" width="80" />
      <el-table-column prop="timeSlot" label="预约时段" min-width="130" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status !== '已签到' && row.status !== '已完成' && row.status !== '已取消'" type="success" size="small" @click="handleCheckIn(row.id)">签到</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkInReservation, fetchTodayReservations, type StaffReservationRow } from '@/api/staff'

const reservations = ref<StaffReservationRow[]>([])

async function loadReservations() {
  try {
    reservations.value = await fetchTodayReservations()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '加载预约数据失败')
  }
}

async function handleCheckIn(id: number) {
  try {
    await checkInReservation(id)
    ElMessage.success('签到成功')
    await loadReservations()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '签到失败')
  }
}

onMounted(loadReservations)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
</style>
