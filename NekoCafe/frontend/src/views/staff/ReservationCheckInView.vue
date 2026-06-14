<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 签到</p>
        <h1>预约签到</h1>
        <p>店员为顾客办理到店签到和入座；对一直未到店的预约，可手动取消释放名额。</p>
      </div>
      <el-button type="primary" @click="loadReservations">刷新预约</el-button>
    </header>

    <el-table :data="reservations" border empty-text="暂无今日预约">
      <el-table-column prop="reservationNo" label="预约号" min-width="140" />
      <el-table-column prop="customerName" label="顾客姓名" />
      <el-table-column prop="customerPhone" label="手机号" min-width="130" />
      <el-table-column prop="partySize" label="人数" width="80" />
      <el-table-column prop="tableNo" label="桌号" width="90" />
      <el-table-column prop="timeSlot" label="预约时段" min-width="130" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <template v-if="canOperate(row.status)">
            <el-button type="success" size="small" @click="handleCheckIn(row.id)">签到</el-button>
            <el-button type="danger" size="small" link :loading="cancellingId === row.id" @click="handleCancel(row)">取消</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelStaffReservation, checkInReservation, fetchTodayReservations, type StaffReservationRow } from '@/api/staff'

const reservations = ref<StaffReservationRow[]>([])
const cancellingId = ref<number>()

async function loadReservations() {
  try {
    reservations.value = await fetchTodayReservations()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '加载预约数据失败')
  }
}

function canOperate(status: string) {
  return !['已签到', '已完成', '已取消'].includes(status)
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

async function handleCancel(row: StaffReservationRow) {
  try {
    await ElMessageBox.confirm(`确认取消预约「${row.reservationNo}」吗？取消后将释放该预约名额，顾客不能再签到。`, '取消预约', { type: 'warning' })
    cancellingId.value = row.id
    await cancelStaffReservation(row.id)
    ElMessage.success('预约已取消')
    await loadReservations()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '取消预约失败')
    }
  } finally {
    cancellingId.value = undefined
  }
}

onMounted(loadReservations)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
</style>
