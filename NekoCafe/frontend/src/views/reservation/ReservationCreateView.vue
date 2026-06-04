<template>
  <section class="page-card reservation-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">RESERVATION</p>
        <h1>创建猫咖预约</h1>
        <p>选择门店、日期与时段，给今天的猫爪相遇留一个座位。</p>
      </div>
      <el-button @click="router.push('/reservations/me')">我的预约</el-button>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="reservation-form">
      <div class="form-grid">
        <el-form-item label="门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="选择门店" size="large" @change="handleQueryChange">
            <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" prop="date">
          <el-date-picker v-model="form.date" value-format="YYYY-MM-DD" type="date" size="large" placeholder="选择日期" :disabled-date="disabledDate" @change="handleQueryChange" />
        </el-form-item>
        <el-form-item label="人数" prop="partySize">
          <el-input-number v-model="form.partySize" :min="1" :max="8" size="large" @change="handleQueryChange" />
        </el-form-item>
      </div>

      <section class="slot-panel" v-loading="slotsLoading">
        <div class="section-title">
          <h2>可用时段</h2>
          <span>按桌位容量自动筛选</span>
        </div>
        <div v-if="slots.length" class="slot-grid">
          <button
            v-for="slot in slots"
            :key="slot.id"
            class="slot-card"
            :class="{ 'is-active': form.slotId === slot.id }"
            type="button"
            @click="selectSlot(slot)"
          >
            <strong>{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</strong>
            <span>{{ slot.area || '猫咖座位区' }} · {{ slot.tableNo }}</span>
            <small>可容纳 {{ slot.capacity }} 人，余 {{ slot.availableCount }} 位</small>
          </button>
        </div>
        <el-empty v-else description="请选择门店、日期和人数，或当前没有可用时段" />
      </section>

      <div class="form-grid">
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model.trim="form.contactName" size="large" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="手机号" prop="contactPhone">
          <el-input v-model.trim="form.contactPhone" size="large" placeholder="请输入手机号" />
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input v-model.trim="form.remark" type="textarea" :rows="3" placeholder="例如：希望靠窗、对猫毛过敏请提前说明" />
      </el-form-item>

      <div class="submit-row">
        <el-button size="large" @click="router.push('/stores')">返回门店</el-button>
        <el-button type="primary" size="large" :loading="submitting" @click="submit">确认预约</el-button>
      </div>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { fetchStores, type StoreSummary } from '@/api/store'
import { createReservation, fetchReservationSlots, type ReservationSlot } from '@/api/reservation'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const stores = ref<StoreSummary[]>([])
const slots = ref<ReservationSlot[]>([])
const slotsLoading = ref(false)
const submitting = ref(false)

const form = reactive({
  storeId: undefined as number | undefined,
  date: tomorrow(),
  partySize: 2,
  slotId: undefined as number | undefined,
  tableId: undefined as number | undefined,
  contactName: auth.user?.nickname || '',
  contactPhone: auth.user?.phone || '',
  remark: '',
})

const rules: FormRules<typeof form> = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  date: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  partySize: [{ required: true, message: '请输入预约人数', trigger: 'change' }],
  slotId: [{ required: true, message: '请选择预约时段', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
}

function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

function disabledDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function formatTime(value: string) {
  return value?.slice(0, 5) || '--:--'
}

function selectSlot(slot: ReservationSlot) {
  form.slotId = slot.id
  form.tableId = slot.tableId
}

async function handleQueryChange() {
  form.slotId = undefined
  form.tableId = undefined
  await loadSlots()
}

async function loadStores() {
  stores.value = await fetchStores()
  const queryStoreId = Number(route.query.storeId)
  form.storeId = stores.value.some((store) => store.id === queryStoreId) ? queryStoreId : stores.value[0]?.id
}

async function loadSlots() {
  if (!form.storeId || !form.date || !form.partySize) return
  slotsLoading.value = true
  try {
    slots.value = await fetchReservationSlots({ storeId: form.storeId, date: form.date, partySize: form.partySize })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约时段加载失败')
  } finally {
    slotsLoading.value = false
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid || !form.storeId || !form.tableId || !form.slotId) return
    submitting.value = true
    try {
      await createReservation({
        storeId: form.storeId,
        tableId: form.tableId,
        slotId: form.slotId,
        partySize: form.partySize,
        contactName: form.contactName,
        contactPhone: form.contactPhone,
        remark: form.remark,
      })
      ElMessage.success('预约成功，猫咪已经帮你占好座位啦')
      await router.push('/reservations/me')
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '预约创建失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  try {
    await loadStores()
    await loadSlots()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约页面初始化失败')
  }
})
</script>

<style scoped>
.reservation-page { display: grid; gap: 22px; }
.page-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 900; letter-spacing: 0.1em; }
.page-header h1 { margin: 0 0 8px; color: #3b2618; }
.page-header p { margin: 0; color: #7c5f4a; }
.reservation-form { display: grid; gap: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.slot-panel { border: 1px solid #f5dfc5; border-radius: 20px; padding: 18px; background: #fffaf4; min-height: 180px; }
.section-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.section-title h2 { margin: 0; color: #3b2618; }
.section-title span { color: #8a6a52; }
.slot-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.slot-card { display: grid; gap: 6px; border: 1px solid #f0cfaa; border-radius: 16px; padding: 14px; background: #fff; color: #4a2d1b; text-align: left; cursor: pointer; transition: 0.18s ease; }
.slot-card:hover, .slot-card.is-active { transform: translateY(-2px); border-color: #d97706; box-shadow: 0 14px 30px rgba(217, 119, 6, 0.14); }
.slot-card strong { color: #3b2618; }
.slot-card span { color: #6b4d37; }
.slot-card small { color: #d97706; font-weight: 800; }
.submit-row { display: flex; justify-content: flex-end; gap: 12px; }
@media (max-width: 860px) {
  .page-header, .section-title { flex-direction: column; align-items: flex-start; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
