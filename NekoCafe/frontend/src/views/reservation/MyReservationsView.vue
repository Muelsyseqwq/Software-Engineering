<template>
  <section class="page-card reservation-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">MY RESERVATIONS</p>
        <h1>我的预约</h1>
        <p>查看猫咖座位、到店时间和预约状态，也可以取消还未完成的预约。</p>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/reservations/new')">新建预约</el-button>
        <el-button type="primary" :loading="loading" @click="loadReservations">刷新</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="reservations" border empty-text="暂无预约，先去挑一家猫咖吧">
      <el-table-column prop="reservationNo" label="预约号" min-width="150" />
      <el-table-column label="门店 / 桌位" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.storeName }}</strong>
          <p class="muted">{{ row.area || '座位区' }} · {{ row.tableNo }}</p>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="180">
        <template #default="{ row }">
          {{ row.slotDate }} {{ formatTime(row.startTime) }}-{{ formatTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="partySize" label="人数" width="80" />
      <el-table-column label="联系人" min-width="140">
        <template #default="{ row }">
          {{ row.contactName }}
          <p class="muted">{{ row.contactPhone }}</p>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canOrder(row.status)" link type="primary" @click="goCheckout(row)">去点单</el-button>
          <el-button v-if="canCancel(row.status)" link type="danger" :loading="cancellingId === row.id" @click="handleCancel(row.id)">取消</el-button>
          <span v-if="!canOrder(row.status) && !canCancel(row.status)" class="muted">不可操作</span>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelReservation, fetchMyReservations, type ReservationRow } from '@/api/reservation'

const router = useRouter()
const loading = ref(false)
const cancellingId = ref<number>()
const reservations = ref<ReservationRow[]>([])

function formatTime(value: string) {
  return value?.slice(0, 5) || '--:--'
}

function statusText(status: string) {
  const map: Record<string, string> = {
    RESERVED: '已预约',
    CHECKED_IN: '已签到',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    NO_SHOW: '未到店',
  }
  return map[status] || status
}

function statusType(status: string) {
  if (status === 'CANCELLED') return 'info'
  if (status === 'CHECKED_IN' || status === 'COMPLETED') return 'success'
  return 'warning'
}

function canCancel(status: string) {
  return !['CANCELLED', 'CHECKED_IN', 'COMPLETED'].includes(status)
}

function canOrder(status: string) {
  return ['RESERVED', 'CHECKED_IN'].includes(status)
}

function goCheckout(row: ReservationRow) {
  router.push({ path: '/orders/checkout', query: { storeId: row.storeId, reservationId: row.id } })
}

async function loadReservations() {
  loading.value = true
  try {
    reservations.value = await fetchMyReservations()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约列表加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确认取消该预约吗？若该预约有关联的未支付订单，系统会一并取消；如已有已支付订单，将无法取消。', '取消预约', { type: 'warning' })
    cancellingId.value = id
    await cancelReservation(id)
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
.reservation-page { display: grid; gap: 20px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.1em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.header-actions { display: flex; gap: 10px; }
.muted { margin: 4px 0 0; color: #8a6a52; font-size: 12px; }
@media (max-width: 720px) {
  .page-header { flex-direction: column; }
  .header-actions { flex-wrap: wrap; }
}
</style>
