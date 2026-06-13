<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 活动管理</p>
        <h1>活动管理</h1>
        <p>创建营销活动并发布给各门店店长，查看门店接受状态。</p>
      </div>
    </header>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-select v-model="filterType" placeholder="活动类型" clearable style="width:140px" @change="loadData">
        <el-option label="促销活动" value="PROMOTION" />
        <el-option label="娱乐活动" value="ENTERTAINMENT" />
        <el-option label="公告通知" value="NOTICE" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="活动状态" clearable style="width:140px; margin-left:8px" @change="loadData">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已结束" value="ENDED" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
      <el-button type="primary" style="margin-left:auto" @click="openCreate">创建活动</el-button>
    </div>

    <!-- Activity table -->
    <el-table :data="activities" border v-loading="loading" empty-text="暂无活动数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="活动标题" min-width="160" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="rewardName" label="优惠券" min-width="120">
        <template #default="{ row }">{{ row.rewardName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="160">
        <template #default="{ row }">{{ fmt(row.startAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="160">
        <template #default="{ row }">{{ fmt(row.endAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :disabled="row.status === 'DELETED'" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 'PUBLISHED'" size="small" type="success" :disabled="row.status === 'DELETED'" @click="openPublish(row)">发布</el-button>
          <el-button size="small" type="warning" :disabled="row.status === 'DELETED'" @click="openAcceptance(row)">门店接受</el-button>
          <el-button size="small" type="danger" :disabled="row.status === 'DELETED'" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑活动' : '创建活动'" width="540px">
      <el-form :model="form" label-position="top">
        <el-form-item label="活动标题" required>
          <el-input v-model="form.title" placeholder="如：周末猫爪甜品节" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="促销活动" value="PROMOTION" />
            <el-option label="娱乐活动" value="ENTERTAINMENT" />
            <el-option label="公告通知" value="NOTICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="活动详细描述" />
        </el-form-item>
        <el-form-item label="发放优惠券">
          <el-select v-model="form.rewardId" placeholder="选择要发放的优惠券（可选）" clearable style="width:100%">
            <el-option v-for="r in rewards" :key="r.id" :label="`${r.name}（${r.rewardType === 'COUPON' ? '优惠券' : r.rewardType}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker v-model="form.startAt" type="datetime" placeholder="选择时间" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker v-model="form.endAt" type="datetime" placeholder="选择时间" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Publish dialog -->
    <el-dialog v-model="publishVisible" title="发布活动到门店" width="480px">
      <p style="margin-bottom:12px">将 <strong>{{ publishTarget?.title }}</strong> 发布到以下门店：</p>
      <el-checkbox-group v-model="publishStoreIds">
        <el-checkbox v-for="s in stores" :key="s.id" :value="s.id" :label="s.id" style="display:block;margin-bottom:6px">
          {{ s.name }}（{{ s.city }} · {{ s.status === 'OPEN' ? '营业中' : '筹备中' }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublish">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- Acceptance dialog -->
    <el-dialog v-model="acceptanceVisible" title="门店接受状态" width="600px">
      <el-table :data="acceptanceList" border empty-text="该活动尚未发布到任何门店">
        <el-table-column prop="storeName" label="门店" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="accType(row.acceptStatus)">{{ accLabel(row.acceptStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" width="100" />
        <el-table-column label="处理时间" width="160">
          <template #default="{ row }">{{ fmt(row.handledAt) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="handleRemark" label="备注" min-width="120" />
      </el-table>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchActivities, createActivity, updateActivity, deleteActivity,
  publishActivity, fetchActivityStores, fetchActivityRewards,
  type ActivityRow, type CreateActivityRequest, type StoreAcceptanceRow,
  type RewardOption,
} from '@/api/activity'
import { fetchAdminStores, type AdminStoreRow } from '@/api/admin'

const activities = ref<ActivityRow[]>([])
const stores = ref<AdminStoreRow[]>([])
const rewards = ref<RewardOption[]>([])
const loading = ref(false)
const filterType = ref('')
const filterStatus = ref('')

// Create/Edit
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<CreateActivityRequest>({ title: '', type: 'PROMOTION', status: 'DRAFT', description: '', coverUrl: '', startAt: undefined, endAt: undefined, rewardId: undefined })

// Publish
const publishVisible = ref(false)
const publishTarget = ref<ActivityRow | null>(null)
const publishStoreIds = ref<number[]>([])

// Acceptance
const acceptanceVisible = ref(false)
const acceptanceList = ref<StoreAcceptanceRow[]>([])

onMounted(() => { loadData(); loadStores(); loadRewards() })

async function loadData() {
  loading.value = true
  try {
    activities.value = await fetchActivities(filterType.value || undefined, filterStatus.value || undefined)
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载活动失败')
  } finally { loading.value = false }
}

async function loadStores() {
  try { stores.value = await fetchAdminStores() } catch { /* silent */ }
}

async function loadRewards() {
  try { rewards.value = await fetchActivityRewards() } catch { /* silent */ }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { title: '', type: 'PROMOTION', status: 'DRAFT', description: '', coverUrl: '', startAt: undefined, endAt: undefined, rewardId: undefined })
}

function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row: ActivityRow) {
  editingId.value = row.id
  Object.assign(form, { title: row.title, type: row.type, description: row.description || '', startAt: row.startAt, endAt: row.endAt, rewardId: row.rewardId })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    const payload: CreateActivityRequest = {
      title: form.title,
      type: form.type,
      description: form.description,
      rewardId: form.rewardId,
    }
    if (form.startAt) payload.startAt = typeof form.startAt === 'string' ? form.startAt : (form.startAt as Date).toISOString()
    if (form.endAt) payload.endAt = typeof form.endAt === 'string' ? form.endAt : (form.endAt as Date).toISOString()

    if (editingId.value) {
      await updateActivity(editingId.value, payload)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(payload)
      ElMessage.success('活动已创建')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该活动？', '删除确认', { type: 'warning' })
    await deleteActivity(id)
    ElMessage.success('已删除')
    await loadData()
  } catch { /* cancelled */ }
}

function openPublish(row: ActivityRow) {
  publishTarget.value = row
  publishStoreIds.value = []
  publishVisible.value = true
}

async function handlePublish() {
  if (!publishTarget.value || publishStoreIds.value.length === 0) {
    ElMessage.warning('请选择至少一个门店')
    return
  }
  try {
    await publishActivity(publishTarget.value.id, publishStoreIds.value)
    ElMessage.success(`已发布到 ${publishStoreIds.value.length} 个门店`)
    publishVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发布失败')
  }
}

async function openAcceptance(row: ActivityRow) {
  publishTarget.value = row
  try {
    acceptanceList.value = await fetchActivityStores(row.id)
    acceptanceVisible.value = true
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载接受状态失败')
  }
}

// --- helpers ---
function fmt(v?: string) { return v ? v.slice(0, 16) : '-' }
function typeLabel(t?: string) { const m: Record<string, string> = { PROMOTION: '促销', ENTERTAINMENT: '娱乐', NOTICE: '公告' }; return m[t || ''] || t || '-' }
function statusLabel(s?: string) { const m: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', ENDED: '已结束', DELETED: '已删除' }; return m[s || ''] || s || '-' }
function statusTagType(s?: string) { if (s === 'PUBLISHED') return 'success'; if (s === 'DRAFT') return 'info'; if (s === 'DELETED') return 'danger'; return 'warning' }
function accLabel(s?: string) { const m: Record<string, string> = { PENDING: '待接受', ACCEPTED: '已接受', REJECTED: '已拒绝' }; return m[s || ''] || s || '-' }
function accType(s?: string) { if (s === 'ACCEPTED') return 'success'; if (s === 'REJECTED') return 'danger'; return 'warning' }
</script>

<style scoped>
.role-page { display: grid; gap: 16px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.toolbar { display: flex; align-items: center; gap: 8px; }
</style>
