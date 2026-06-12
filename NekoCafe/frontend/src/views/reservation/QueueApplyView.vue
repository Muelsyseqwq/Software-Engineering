<template>
  <section class="page-card queue-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">WAITING QUEUE</p>
        <h1>排队取号</h1>
        <p>桌位满员时领取候位号码，实时查看当前叫号与自己的排队状态。</p>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/reservations/new')">去预约</el-button>
        <el-button type="primary" :loading="loading" @click="loadStatus">刷新状态</el-button>
      </div>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="queue-form">
      <div class="form-grid">
        <el-form-item label="门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="选择门店" size="large" @change="loadStatus">
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排队人数" prop="partySize">
          <el-input-number v-model="form.partySize" :min="1" :max="8" size="large" @change="loadStatus" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model.trim="form.contactName" size="large" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="手机号" prop="contactPhone">
          <el-input v-model.trim="form.contactPhone" size="large" placeholder="请输入手机号" />
        </el-form-item>
      </div>
    </el-form>

    <section class="status-grid" v-loading="loading">
      <article class="number-card current">
        <span>当前叫号</span>
        <strong>{{ status?.currentNumber ? status.currentNumber : '--' }}</strong>
        <small>当前等待 {{ status?.waitingCount ?? 0 }} 组</small>
      </article>
      <article class="number-card next">
        <span>下一流水号</span>
        <strong>{{ status?.nextNumber ?? 1 }}</strong>
        <small>号码按门店每日独立发放</small>
      </article>
      <article class="ticket-card">
        <div class="ticket-title">
          <span>我的号码</span>
          <el-tag v-if="status?.myTicket" :type="ticketTagType(status.myTicket.status)">{{ ticketStatusText(status.myTicket.status) }}</el-tag>
        </div>
        <template v-if="status?.myTicket">
          <strong>{{ status.myTicket.queueNumber }}</strong>
          <p>{{ ticketHint(status.myTicket.status) }}</p>
          <small>{{ status.myTicket.contactName }} · {{ status.myTicket.partySize }} 人 · {{ formatTime(status.myTicket.createdAt) }}</small>
        </template>
        <el-empty v-else description="当前没有排队号码" />
      </article>
    </section>

    <div class="submit-row">
      <el-alert
        v-if="status?.myTicket?.status === 'EXPIRED'"
        title="你已过号，需要重新取号排队。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-button v-if="canCancel" :loading="submitting" @click="cancelTicket">取消排队</el-button>
      <el-button type="primary" size="large" :disabled="!status?.canApply" :loading="submitting" @click="apply">
        {{ status?.canApply ? '申请排队' : '当前不可取号' }}
      </el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { fetchStores, type StoreSummary } from '@/api/store'
import { applyQueue, cancelQueueTicket, fetchQueueStatus, type QueueStatus } from '@/api/reservation'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const stores = ref<StoreSummary[]>([])
const status = ref<QueueStatus>()
const loading = ref(false)
const submitting = ref(false)
let timer: number | undefined

const form = reactive({
  storeId: undefined as number | undefined,
  partySize: 2,
  contactName: auth.user?.nickname || '',
  contactPhone: auth.user?.phone || '',
})

const rules: FormRules<typeof form> = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  partySize: [{ required: true, message: '请输入排队人数', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
}

const canCancel = computed(() => ['WAITING', 'CALLED'].includes(status.value?.myTicket?.status || ''))

async function loadStores() {
  stores.value = await fetchStores()
  const queryStoreId = Number(route.query.storeId)
  form.storeId = stores.value.some((store) => store.id === queryStoreId) ? queryStoreId : stores.value[0]?.id
}

async function loadStatus() {
  if (!form.storeId) return
  loading.value = true
  try {
    status.value = await fetchQueueStatus(form.storeId, form.partySize)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排队状态加载失败')
  } finally {
    loading.value = false
  }
}

async function apply() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid || !form.storeId) return
    submitting.value = true
    try {
      const ticket = await applyQueue({
        storeId: form.storeId,
        partySize: form.partySize,
        contactName: form.contactName,
        contactPhone: form.contactPhone,
      })
      ElMessage.success(`取号成功，你的号码是 ${ticket.queueNumber}`)
      await loadStatus()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '申请排队失败')
    } finally {
      submitting.value = false
    }
  })
}

async function cancelTicket() {
  const ticket = status.value?.myTicket
  if (!ticket) return
  await ElMessageBox.confirm('确定取消当前排队号码吗？取消后需要重新取号。', '取消排队', { type: 'warning' })
  submitting.value = true
  try {
    await cancelQueueTicket(ticket.id)
    ElMessage.success('已取消排队')
    await loadStatus()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '取消排队失败')
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

function ticketHint(value: string) {
  if (value === 'WAITING') return '请留意当前叫号，快到你啦。'
  if (value === 'CALLED') return '已叫到你的号码，请尽快到前台确认。'
  if (value === 'SEATED') return '已确认入座，祝你用餐愉快。'
  if (value === 'EXPIRED') return '该号码已过号，请重新取号排队。'
  if (value === 'CANCELLED') return '该号码已取消，可重新取号。'
  return '请关注现场叫号。'
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
    await loadStores()
    await loadStatus()
    timer = window.setInterval(loadStatus, 15000)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排队页面初始化失败')
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.queue-page { display: grid; gap: 22px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; flex-wrap: wrap; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.1em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.queue-form { display: grid; gap: 18px; }
.form-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.status-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.number-card, .ticket-card { border: 1px solid #f5dfc5; border-radius: 22px; background: #fffaf4; padding: 20px; min-height: 160px; display: grid; gap: 8px; align-content: center; }
.number-card span, .ticket-title span { color: #8a6a52; font-weight: 800; }
.number-card strong, .ticket-card strong { font-size: 56px; color: #d97706; line-height: 1; }
.number-card small, .ticket-card small, .ticket-card p { color: #7c5f4a; margin: 0; }
.ticket-title { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.submit-row { display: flex; justify-content: flex-end; gap: 12px; align-items: center; flex-wrap: wrap; }
.submit-row .el-alert { flex: 1 1 260px; }
@media (max-width: 980px) {
  .form-grid, .status-grid { grid-template-columns: 1fr; }
}
</style>
