<template>
  <section class="page-card staff-queue-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF QUEUE</p>
        <h1>当前门店值班台</h1>
        <p>管理自己所属门店的现场候位号码，支持叫下一号、确认入座和重置流水号。</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!selectedStoreId" :loading="loading" @click="loadStatus">刷新</el-button>
        <el-button type="danger" :disabled="!selectedStoreId" :loading="submitting" @click="resetSerial">重置流水号</el-button>
      </div>
    </header>

    <div class="filter-bar">
      <el-alert v-if="!assignedStores.length" type="warning" show-icon :closable="false" title="当前账号暂未分配门店，无法操作排队叫号" />
      <article v-else-if="assignedStores.length === 1" class="store-card">
        <span>值班门店</span>
        <strong>{{ assignedStores[0].name }}</strong>
        <small>{{ [assignedStores[0].city, assignedStores[0].address].filter(Boolean).join(' · ') || '门店地址未填写' }}</small>
      </article>
      <el-select v-else v-model="selectedStoreId" placeholder="选择授权门店" style="width: 280px" @change="loadStatus">
        <el-option v-for="store in assignedStores" :key="store.id" :label="store.name" :value="store.id" />
      </el-select>
      <el-button type="primary" :disabled="!selectedStoreId" :loading="submitting" @click="callNext">下一号</el-button>
    </div>

    <section class="summary-grid" v-loading="loading">
      <article class="summary-card current">
        <span>当前叫号</span>
        <strong>{{ queueStatus?.currentNumber ? queueStatus.currentNumber : '--' }}</strong>
        <small>等待 {{ queueStatus?.waitingCount ?? 0 }} 组</small>
      </article>
      <article class="summary-card">
        <span>下一流水号</span>
        <strong>{{ queueStatus?.nextNumber ?? 1 }}</strong>
        <small>{{ queueStatus?.queueDate || '今日' }}</small>
      </article>
      <article class="called-card">
        <div class="card-title">
          <span>当前顾客</span>
          <el-tag v-if="queueStatus?.calledTicket" type="danger">已叫号</el-tag>
        </div>
        <template v-if="queueStatus?.calledTicket">
          <strong>{{ queueStatus.calledTicket.contactName }} · {{ queueStatus.calledTicket.partySize }} 人</strong>
          <p>{{ queueStatus.calledTicket.contactPhone }}</p>
          <small>叫号时间：{{ formatTime(queueStatus.calledTicket.calledAt) }}</small>
          <el-button type="success" :loading="submitting" @click="seatCalledTicket">确认入座</el-button>
        </template>
        <el-empty v-else description="暂无已叫号顾客" />
      </article>
    </section>

    <div class="table-wrap">
      <el-table :data="queueStatus?.tickets || []" border v-loading="loading" empty-text="暂无排队顾客">
        <el-table-column prop="queueNumber" label="号码" width="90" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="手机号" width="150" />
        <el-table-column prop="partySize" label="人数" width="90" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="ticketTagType(row.status)">{{ ticketStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="取号时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { callNextQueueNumber, fetchStaffQueueStatus, markQueueTicketSeated, resetQueue, type StaffQueueStatus } from '@/api/staff'
import type { AuthAssignedStore } from '@/types/auth'

const auth = useAuthStore()
const selectedStoreId = ref<number>()
const queueStatus = ref<StaffQueueStatus>()
const loading = ref(false)
const submitting = ref(false)
let timer: number | undefined

const assignedStores = computed<AuthAssignedStore[]>(() => {
  const stores = auth.user?.stores?.filter((store) => store.id) || []
  if (stores.length > 0) return stores
  if (auth.user?.storeId && auth.user.storeName) {
    return [{ id: auth.user.storeId, name: auth.user.storeName }]
  }
  return []
})

async function syncAssignedStores() {
  if (!auth.profileSynced || auth.user?.stores === undefined) {
    await auth.fetchMe()
  }
  selectedStoreId.value = assignedStores.value[0]?.id
}

async function loadStatus() {
  if (!selectedStoreId.value) {
    queueStatus.value = undefined
    return
  }
  loading.value = true
  try {
    queueStatus.value = await fetchStaffQueueStatus(selectedStoreId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排队状态加载失败')
  } finally {
    loading.value = false
  }
}

async function callNext() {
  if (!selectedStoreId.value) return
  if (queueStatus.value?.calledTicket) {
    await ElMessageBox.confirm('当前叫号未确认入座，继续叫下一号会将其标记为过号。确定继续吗？', '叫下一号', { type: 'warning' })
  }
  submitting.value = true
  try {
    queueStatus.value = await callNextQueueNumber(selectedStoreId.value)
    if (queueStatus.value.calledTicket) {
      ElMessage.success(`已叫到 ${queueStatus.value.calledTicket.queueNumber} 号`)
    } else {
      ElMessage.info('暂无等待顾客')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '叫号失败')
  } finally {
    submitting.value = false
  }
}

async function seatCalledTicket() {
  const ticket = queueStatus.value?.calledTicket
  if (!ticket) return
  submitting.value = true
  try {
    await markQueueTicketSeated(ticket.id)
    ElMessage.success('已确认顾客入座')
    await loadStatus()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认入座失败')
  } finally {
    submitting.value = false
  }
}

async function resetSerial() {
  if (!selectedStoreId.value) return
  await ElMessageBox.confirm('重置后当前等待和已叫号顾客都将过号，需要重新取号。确定重置吗？', '重置流水号', { type: 'warning' })
  submitting.value = true
  try {
    queueStatus.value = await resetQueue(selectedStoreId.value)
    ElMessage.success('流水号已重置')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重置失败')
  } finally {
    submitting.value = false
  }
}

function ticketStatusText(value: string) {
  const map: Record<string, string> = {
    WAITING: '排队中',
    CALLED: '已叫号',
    SEATED: '已入座',
    EXPIRED: '已过号',
    CANCELLED: '已取消',
  }
  return map[value] || value
}

function ticketTagType(value: string) {
  switch (value) {
    case 'WAITING': return 'warning'
    case 'CALLED': return 'danger'
    case 'SEATED': return 'success'
    case 'EXPIRED': return 'info'
    case 'CANCELLED': return 'info'
    default: return ''
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(async () => {
  try {
    await syncAssignedStores()
    await loadStatus()
    timer = window.setInterval(loadStatus, 10000)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排队叫号页面初始化失败')
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.staff-queue-page { display: grid; gap: 18px; min-width: 0; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.header-actions, .filter-bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.store-card { min-width: 280px; border: 1px solid #f5dfc5; border-radius: 18px; padding: 14px 16px; background: #fffaf4; display: grid; gap: 4px; }
.store-card span { color: #8a6a52; font-weight: 800; font-size: 12px; }
.store-card strong { color: #3b2618; }
.store-card small { color: #7c5f4a; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.summary-card, .called-card { border: 1px solid #f5dfc5; border-radius: 20px; padding: 18px; background: #fffaf4; min-height: 160px; display: grid; gap: 8px; align-content: center; }
.summary-card span, .card-title span { color: #8a6a52; font-weight: 800; }
.summary-card strong { font-size: 52px; color: #d97706; line-height: 1; }
.called-card strong { color: #3b2618; font-size: 20px; }
.called-card p, .called-card small, .summary-card small { color: #7c5f4a; margin: 0; }
.card-title { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.table-wrap { overflow-x: auto; }
@media (max-width: 980px) {
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
