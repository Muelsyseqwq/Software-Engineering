<template>
  <section class="cat-page">
    <div class="cat-hero">
      <div class="hero-copy">
        <p class="eyebrow">CAT_CARETAKER 猫咪管家</p>
        <h1>猫咪档案管理</h1>
        <p>像整理一本猫咖手账一样，记录每只猫的体重、疫苗、互动偏好和照片。</p>
      </div>
      <div class="hero-actions">
        <el-button class="soft-button" @click="loadCats">刷新档案</el-button>
        <el-button type="primary" class="hero-primary" @click="openCreateDialog">新增猫咪</el-button>
      </div>
    </div>

    <div class="insight-grid">
      <article class="insight-card">
        <span class="insight-icon">🐾</span>
        <div>
          <strong>{{ cats.length }}</strong>
          <p>档案总数</p>
        </div>
      </article>
      <article class="insight-card">
        <span class="insight-icon">🩺</span>
        <div>
          <strong>{{ healthyCount }}</strong>
          <p>健康猫咪</p>
        </div>
      </article>
      <article class="insight-card">
        <span class="insight-icon">☕</span>
        <div>
          <strong>{{ activeCount }}</strong>
          <p>可互动档案</p>
        </div>
      </article>
    </div>

    <div class="table-shell">
      <el-table
        v-loading="loading"
        :data="cats"
        empty-text="暂无猫咪档案，点击新增猫咪创建第一条记录"
        class="cat-table"
      >
        <el-table-column label="猫咪" min-width="210">
          <template #default="{ row }">
            <div class="cat-cell">
              <el-avatar :size="52" :src="row.photoUrl || undefined">{{ row.name?.slice(0, 1) || '猫' }}</el-avatar>
              <div>
                <strong>{{ row.name }}</strong>
                <p>{{ row.breed || '品种待填写' }} · {{ genderLabel(row.gender) }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="基础信息" min-width="150">
          <template #default="{ row }">
            <div class="mini-stack">
              <span>{{ row.age ?? '待填写' }} 岁</span>
              <span>{{ weightLabel(row.weight) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="personality" label="性格标签" min-width="170">
          <template #default="{ row }">
            <el-tag effect="plain" class="personality-tag">{{ row.personality || '待填写' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="healthStatus" label="健康状态" width="150">
          <template #default="{ row }">
            <el-select
              :model-value="normalizeHealthStatus(row.healthStatus)"
              size="small"
              class="status-select"
              @change="(value: string) => handleHealthChange(row, value)"
            >
              <el-option v-for="item in healthStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="档案状态" width="120">
          <template #default="{ row }">
            <el-tag round :type="statusTagType(normalizeCatStatus(row.status))">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="openDetailDialog(row)">查看</el-button>
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" :type="normalizeCatStatus(row.status) === 'DISABLED' ? 'success' : 'warning'" @click="handleToggleStatus(row)">
                {{ normalizeCatStatus(row.status) === 'DISABLED' ? '启用' : '停用' }}
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑猫咪档案' : '新增猫咪档案'" width="820px" class="cat-dialog">
      <el-form :model="form" label-position="top">
        <div class="photo-editor">
          <div class="photo-frame">
            <el-avatar :size="112" :src="form.photoUrl || undefined">{{ form.name?.slice(0, 1) || '猫' }}</el-avatar>
          </div>
          <div class="photo-editor__content">
            <div>
              <h3>档案照片</h3>
              <p>本地上传后会生成可访问地址，保存档案时写入数据库。</p>
            </div>
            <div class="upload-row">
              <el-upload
                accept="image/png,image/jpeg,image/webp,image/gif"
                :show-file-list="false"
                :http-request="handlePhotoUpload"
                :before-upload="beforePhotoUpload"
              >
                <el-button type="primary" plain :loading="uploading">上传本地照片</el-button>
              </el-upload>
              <span class="form-tip">jpg / png / webp / gif，≤ 5MB</span>
            </div>
            <el-input v-model="form.photoUrl" placeholder="也可以填写网络图片地址或保留上传后的地址" />
          </div>
        </div>

        <div class="form-section-title">基础档案</div>
        <div class="form-grid">
          <el-form-item label="名字" required><el-input v-model="form.name" placeholder="例如：拿铁" /></el-form-item>
          <el-form-item label="品种"><el-input v-model="form.breed" placeholder="例如：英短" /></el-form-item>
          <el-form-item label="年龄"><el-input-number v-model="form.age" :min="0" :max="40" controls-position="right" /></el-form-item>
          <el-form-item label="体重（kg）">
            <el-input-number v-model="form.weight" :min="0.2" :max="20" :precision="2" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.gender" placeholder="请选择性别" clearable>
              <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="性格"><el-input v-model="form.personality" placeholder="亲人 / 安静 / 活泼" /></el-form-item>
          <el-form-item label="健康状态">
            <el-select v-model="form.healthStatus" placeholder="请选择健康状态">
              <el-option v-for="item in healthStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="档案状态">
            <el-tag :type="statusTagType(statusForHealth(form.healthStatus))">
              {{ statusLabel(statusForHealth(form.healthStatus)) }}
            </el-tag>
            <span class="form-tip status-tip">健康状态为“健康”时自动可互动，其他状态自动休息中。</span>
          </el-form-item>
        </div>

        <div class="form-section-title">护理与互动</div>
        <el-form-item label="疫苗信息">
          <el-input v-model="form.vaccinium" type="textarea" rows="2" placeholder="例如：猫三联已接种，狂犬疫苗 2026-03 到期" />
        </el-form-item>
        <el-form-item label="互动记录 / 互动偏好">
          <el-input v-model="form.interact" type="textarea" rows="2" placeholder="例如：喜欢逗猫棒，不喜欢被抱太久" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="记录护理注意事项、特殊饮食、适合互动场景等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存档案</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="猫咪档案详情" width="920px" class="cat-dialog" @closed="handleDetailClosed">
      <div v-if="currentCat" class="cat-detail">
        <div class="detail-cover">
          <el-avatar :size="112" :src="currentCat.photoUrl || undefined">{{ currentCat.name?.slice(0, 1) || '猫' }}</el-avatar>
          <div>
            <p class="eyebrow">NEKO PROFILE</p>
            <h2>{{ currentCat.name }}</h2>
            <p>{{ currentCat.breed || '品种待填写' }} · {{ genderLabel(currentCat.gender) }}</p>
          </div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="年龄">{{ currentCat.age ?? '待填写' }}</el-descriptions-item>
          <el-descriptions-item label="体重">{{ weightLabel(currentCat.weight) }}</el-descriptions-item>
          <el-descriptions-item label="性格">{{ currentCat.personality || '待填写' }}</el-descriptions-item>
          <el-descriptions-item label="疫苗信息">{{ currentCat.vaccinium || '待填写' }}</el-descriptions-item>
          <el-descriptions-item label="互动记录">{{ currentCat.interact || '待填写' }}</el-descriptions-item>
          <el-descriptions-item label="健康状态">
            <el-tag :type="healthTagType(currentCat.healthStatus)">{{ healthStatusLabel(currentCat.healthStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="档案状态">
            <el-tag :type="statusTagType(currentCat.status)">{{ statusLabel(currentCat.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentCat.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>

        <div class="health-panel">
          <div class="health-panel__header">
            <div>
              <h3>健康趋势</h3>
              <p>按记录日期展示体重变化，并沉淀疫苗与互动记录。</p>
            </div>
            <el-button type="primary" size="small" @click="openRecordDialog">新增健康记录</el-button>
          </div>

          <div v-loading="recordLoading" class="chart-card">
            <div v-if="weightTrend.length" ref="weightChartRef" class="weight-chart" />
            <el-empty v-else description="暂无体重趋势记录" />
          </div>

          <el-timeline v-if="healthRecords.length" class="health-timeline">
            <el-timeline-item v-for="record in healthRecords" :key="record.id" :timestamp="formatRecordDate(record.recordDate)" placement="top">
              <div class="record-card">
                <p v-if="record.weight"><strong>体重：</strong>{{ weightLabel(record.weight) }}</p>
                <p v-if="record.vaccinium"><strong>疫苗：</strong>{{ record.vaccinium }}</p>
                <p v-if="record.interact"><strong>互动：</strong>{{ record.interact }}</p>
                <p v-if="record.note"><strong>备注：</strong>{{ record.note }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无健康记录" />
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="recordDialogVisible" title="新增健康记录" width="560px" class="cat-dialog">
      <el-form :model="recordForm" label-position="top">
        <el-form-item label="记录日期">
          <el-date-picker
            v-model="recordForm.recordDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择记录日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="体重（kg）">
          <el-input-number v-model="recordForm.weight" :min="0.2" :max="20" :precision="2" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="疫苗信息">
          <el-input v-model="recordForm.vaccinium" type="textarea" rows="2" placeholder="例如：猫三联加强针" />
        </el-form-item>
        <el-form-item label="互动记录">
          <el-input v-model="recordForm.interact" type="textarea" rows="2" placeholder="例如：喜欢逗猫棒，精神状态良好" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="recordForm.note" type="textarea" rows="2" placeholder="例如：食欲正常，建议持续观察" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recordSubmitting" @click="handleRecordSubmit">保存记录</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import * as echarts from 'echarts'
import {
  createCat,
  createCatHealthRecord,
  deleteCat,
  fetchCat,
  fetchCatHealthRecords,
  fetchCatWeightTrend,
  fetchCats,
  updateCat,
  updateCatHealthStatus,
  updateCatStatus,
  uploadCatPhoto,
  type CatHealthRecord,
  type CatHealthStatus,
  type CatProfile,
  type CatStatus,
  type CatWeightTrendPoint,
} from '@/api/cat'

const healthStatusOptions: Array<{ label: string; value: CatHealthStatus }> = [
  { label: '健康', value: '健康' },
  { label: '观察中', value: '观察中' },
  { label: '治疗中', value: '治疗中' },
  { label: '恢复中', value: '恢复中' },
]

const statusOptions: Array<{ label: string; value: CatStatus }> = [
  { label: '可互动', value: 'AVAILABLE' },
  { label: '休息中', value: 'RESTING' },
  { label: '停用', value: 'DISABLED' },
]

const genderOptions = [
  { label: '公', value: 'MALE' },
  { label: '母', value: 'FEMALE' },
  { label: '未知', value: 'UNKNOWN' },
]

const cats = ref<CatProfile[]>([])
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<number>()
const currentCat = ref<CatProfile>()
const healthRecords = ref<CatHealthRecord[]>([])
const weightTrend = ref<CatWeightTrendPoint[]>([])
const recordLoading = ref(false)
const recordDialogVisible = ref(false)
const recordSubmitting = ref(false)
const weightChartRef = ref<HTMLDivElement | null>(null)
let weightChart: echarts.ECharts | null = null
const form = reactive<CatProfile>(createEmptyForm())
const recordForm = reactive<CatHealthRecord>(createEmptyRecordForm())

const healthyCount = computed(() => cats.value.filter((cat) => normalizeHealthStatus(cat.healthStatus) === '健康').length)
const activeCount = computed(() => cats.value.filter((cat) => normalizeCatStatus(cat.status) === 'AVAILABLE').length)

function createEmptyForm(): CatProfile {
  return {
    name: '',
    breed: '',
    age: undefined,
    weight: undefined,
    gender: 'UNKNOWN',
    personality: '',
    interact: '',
    healthStatus: '健康',
    vaccinium: '',
    photoUrl: '',
    description: '',
    status: 'AVAILABLE',
  }
}

function createEmptyRecordForm(): CatHealthRecord {
  return {
    recordDate: todayText(),
    weight: undefined,
    vaccinium: '',
    interact: '',
    note: '',
  }
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, createEmptyForm())
}

async function loadCats() {
  loading.value = true
  try {
    cats.value = await fetchCats()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '猫咪档案接口待接入')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: CatProfile) {
  editingId.value = row.id
  Object.assign(form, createEmptyForm(), row, {
    healthStatus: normalizeHealthStatus(row.healthStatus),
    status: normalizeCatStatus(row.status),
  })
  dialogVisible.value = true
}

async function openDetailDialog(row: CatProfile) {
  if (!row.id) return
  try {
    const detail = await fetchCat(row.id)
    currentCat.value = normalizeCatProfile(detail)
    detailVisible.value = true
    await nextTick()
    await loadHealthPanel(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '读取猫咪详情失败')
  }
}

async function loadHealthPanel(catId: number) {
  recordLoading.value = true
  try {
    const [records, trend] = await Promise.all([
      fetchCatHealthRecords(catId),
      fetchCatWeightTrend(catId),
    ])
    healthRecords.value = records
    weightTrend.value = trend
    await nextTick()
    renderWeightChart()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '读取健康记录失败')
  } finally {
    recordLoading.value = false
  }
}

function openRecordDialog() {
  Object.assign(recordForm, createEmptyRecordForm(), {
    weight: currentCat.value?.weight,
  })
  recordDialogVisible.value = true
}

async function handleRecordSubmit() {
  if (!currentCat.value?.id) return
  const hasContent =
    recordForm.weight !== undefined && recordForm.weight !== null
    || Boolean(recordForm.vaccinium?.trim())
    || Boolean(recordForm.interact?.trim())
    || Boolean(recordForm.note?.trim())
  if (!hasContent) {
    ElMessage.warning('请至少填写体重、疫苗、互动记录或备注中的一项')
    return
  }
  if (!recordForm.recordDate) {
    ElMessage.warning('请选择记录日期')
    return
  }
  if (recordForm.weight !== undefined && recordForm.weight !== null && (recordForm.weight < 0.2 || recordForm.weight > 20)) {
    ElMessage.warning('体重需要在 0.20kg 到 20.00kg 之间')
    return
  }

  recordSubmitting.value = true
  try {
    const catId = currentCat.value.id
    await createCatHealthRecord(catId, recordForm)
    ElMessage.success('健康记录已保存')
    recordDialogVisible.value = false
    const detail = await fetchCat(catId)
    currentCat.value = normalizeCatProfile(detail)
    await Promise.all([loadHealthPanel(catId), loadCats()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存健康记录失败')
  } finally {
    recordSubmitting.value = false
  }
}

function beforePhotoUpload(file: File) {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('仅支持 jpg、png、webp、gif 格式的图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function handlePhotoUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const result = await uploadCatPhoto(options.file)
    form.photoUrl = result.photoUrl
    ElMessage.success('照片上传成功')
    options.onSuccess?.(result)
  } catch (error) {
    const message = error instanceof Error ? error.message : '照片上传失败'
    ElMessage.error(message)
    options.onError?.(new Error(message) as Parameters<NonNullable<typeof options.onError>>[0])
  } finally {
    uploading.value = false
  }
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写猫咪名字')
    return
  }
  if (form.weight !== undefined && form.weight !== null && (form.weight < 0.2 || form.weight > 20)) {
    ElMessage.warning('体重需要在 0.20kg 到 20.00kg 之间')
    return
  }
  submitting.value = true
  try {
    const payload: CatProfile = {
      ...form,
      healthStatus: normalizeHealthStatus(form.healthStatus),
      status: statusForHealth(form.healthStatus),
    }
    if (editingId.value) await updateCat(editingId.value, payload)
    else await createCat(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadCats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存接口待接入')
  } finally {
    submitting.value = false
  }
}

async function handleHealthChange(row: CatProfile, value: string) {
  if (!row.id || normalizeHealthStatus(row.healthStatus) === value) return
  try {
    await updateCatHealthStatus(row.id, value)
    ElMessage.success('健康状态已更新')
    await loadCats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '健康状态更新失败')
    await loadCats()
  }
}

async function handleToggleStatus(row: CatProfile) {
  if (!row.id) return
  const isDisabled = normalizeCatStatus(row.status) === 'DISABLED'
  const nextStatus = isDisabled ? statusForHealth(row.healthStatus) : 'DISABLED'
  const actionText = isDisabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}“${row.name}”的猫咪档案吗？`, `${actionText}档案`, { type: 'warning' })
    await updateCatStatus(row.id, nextStatus)
    ElMessage.success(`${actionText}成功`)
    await loadCats()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : `${actionText}失败`)
  }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('删除后该猫咪档案将不再显示，确认继续吗？', '删除猫咪档案', { type: 'warning' })
    await deleteCat(id)
    ElMessage.success('删除成功')
    await loadCats()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除接口待接入')
  }
}

function renderWeightChart() {
  if (!weightTrend.value.length) {
    disposeWeightChart()
    return
  }
  if (!weightChartRef.value) return
  disposeWeightChart()
  weightChart = echarts.init(weightChartRef.value)
  weightChart.setOption({
    grid: { left: 48, right: 22, top: 28, bottom: 34 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const list = Array.isArray(params) ? params : [params]
        const point = list[0] as { axisValue?: string; value?: number | string }
        return `${point.axisValue || ''}<br/>体重：${Number(point.value || 0).toFixed(2)} kg`
      },
    },
    xAxis: {
      type: 'category',
      data: weightTrend.value.map((item) => item.label),
      axisLine: { lineStyle: { color: '#e7c99b' } },
      axisLabel: { color: '#8c7057' },
    },
    yAxis: {
      type: 'value',
      name: 'kg',
      min: (value: { min: number }) => Math.max(0, Math.floor((value.min - 0.5) * 10) / 10),
      axisLabel: { color: '#8c7057' },
      splitLine: { lineStyle: { color: 'rgba(154, 90, 44, 0.12)' } },
    },
    series: [{
      name: '体重',
      type: 'line',
      smooth: true,
      data: weightTrend.value.map((item) => Number(item.value)),
      areaStyle: { color: 'rgba(227, 163, 58, 0.18)' },
      lineStyle: { color: '#d97706', width: 3 },
      itemStyle: { color: '#9a5a2c' },
      symbolSize: 8,
    }],
  })
}

function resizeWeightChart() {
  weightChart?.resize()
}

function disposeWeightChart() {
  weightChart?.dispose()
  weightChart = null
}

function handleDetailClosed() {
  disposeWeightChart()
  healthRecords.value = []
  weightTrend.value = []
}

function normalizeCatProfile(cat: CatProfile): CatProfile {
  return {
    ...cat,
    healthStatus: normalizeHealthStatus(cat.healthStatus),
    status: normalizeCatStatus(cat.status),
  }
}

function normalizeHealthStatus(value?: string): CatHealthStatus {
  if (value === 'OBSERVING') return '观察中'
  if (value === 'TREATMENT') return '治疗中'
  if (value === 'RECOVERING') return '恢复中'
  if (value === '健康' || value === '观察中' || value === '治疗中' || value === '恢复中') return value
  return '健康'
}

function normalizeCatStatus(value?: string): CatStatus {
  if (value === 'ACTIVE') return 'AVAILABLE'
  if (value === 'INACTIVE' || value === 'OFFLINE' || value === '休息中' || value === '不可互动') return 'RESTING'
  if (value === '停用' || value === '已停用') return 'DISABLED'
  if (value === 'RESTING' || value === 'DISABLED') return value
  return 'AVAILABLE'
}

function statusForHealth(value?: string): CatStatus {
  return normalizeHealthStatus(value) === '健康' ? 'AVAILABLE' : 'RESTING'
}

function healthStatusLabel(value?: string) {
  return healthStatusOptions.find((item) => item.value === normalizeHealthStatus(value))?.label || '待填写'
}

function statusLabel(value?: string) {
  return statusOptions.find((item) => item.value === normalizeCatStatus(value))?.label || '待填写'
}

function genderLabel(value?: string) {
  return genderOptions.find((item) => item.value === value)?.label || '待填写'
}

function weightLabel(value?: number) {
  return value === undefined || value === null ? '待填写' : `${Number(value).toFixed(2)} kg`
}

function formatRecordDate(value?: string) {
  return value || '未填写日期'
}

function todayText() {
  return new Date().toISOString().slice(0, 10)
}

function healthTagType(value?: string) {
  const normalized = normalizeHealthStatus(value)
  if (normalized === '健康') return 'success'
  if (normalized === '观察中') return 'warning'
  if (normalized === '治疗中') return 'danger'
  if (normalized === '恢复中') return 'info'
  return 'info'
}

function statusTagType(value?: string) {
  const normalized = normalizeCatStatus(value)
  if (normalized === 'AVAILABLE') return 'success'
  if (normalized === 'RESTING') return 'warning'
  if (normalized === 'DISABLED') return 'info'
  return 'info'
}

onMounted(() => {
  loadCats()
  window.addEventListener('resize', resizeWeightChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeWeightChart)
  disposeWeightChart()
})
</script>

<style scoped>
.cat-page {
  --cream: #fff7e8;
  --paper: rgba(255, 252, 244, 0.92);
  --ink: #3a2719;
  --muted: #8c7057;
  --coffee: #9a5a2c;
  --caramel: #e3a33a;
  --peach: #ffdfb9;
  --line: rgba(154, 90, 44, 0.18);
  position: relative;
  display: grid;
  gap: 18px;
  padding: 24px;
  border-radius: 28px;
  overflow: hidden;
  color: var(--ink);
  background:
    radial-gradient(circle at 10% 0%, rgba(255, 204, 128, 0.36), transparent 34%),
    radial-gradient(circle at 92% 12%, rgba(255, 239, 196, 0.72), transparent 32%),
    linear-gradient(135deg, #fffaf0 0%, #fff2d8 48%, #f8dfb4 100%);
  box-shadow: 0 24px 70px rgba(95, 55, 23, 0.14);
}

.cat-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(rgba(112, 74, 35, 0.08) 1px, transparent 1px),
    linear-gradient(120deg, transparent 0 62%, rgba(255, 255, 255, 0.32) 62% 64%, transparent 64%);
  background-size: 18px 18px, 100% 100%;
  mix-blend-mode: multiply;
}

.cat-hero,
.insight-grid,
.table-shell { position: relative; z-index: 1; }

.cat-hero {
  display: flex;
  justify-content: space-between;
  gap: 22px;
  align-items: center;
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 244, 222, 0.72));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 18px 42px rgba(132, 82, 31, 0.12);
}

.hero-copy h1 {
  margin: 0;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(32px, 4vw, 52px);
  letter-spacing: -0.05em;
}

.hero-copy p:last-child { max-width: 620px; margin: 10px 0 0; color: var(--muted); font-size: 15px; }
.eyebrow { margin: 0 0 8px; color: var(--coffee); font-weight: 900; letter-spacing: 0.12em; font-size: 12px; }
.hero-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.hero-primary { box-shadow: 0 12px 24px rgba(227, 132, 39, 0.28); }
.soft-button { border-color: var(--line); color: var(--coffee); background: rgba(255, 255, 255, 0.62); }

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.insight-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(12px);
}

.insight-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: #ffe5b9;
  font-size: 22px;
}

.insight-card strong { display: block; font-size: 28px; line-height: 1; }
.insight-card p { margin: 5px 0 0; color: var(--muted); font-size: 13px; }

.table-shell {
  padding: 10px;
  border-radius: 24px;
  background: var(--paper);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 18px 40px rgba(111, 73, 32, 0.1);
}

.cat-table :deep(.el-table__inner-wrapper::before) { display: none; }
.cat-table :deep(.el-table__header th) { background: #fff4dd; color: var(--coffee); font-weight: 800; }
.cat-table :deep(.el-table__row) { transition: transform 0.18s ease, background 0.18s ease; }
.cat-table :deep(.el-table__row:hover) { background: #fff9ed; }

.cat-cell { display: flex; gap: 12px; align-items: center; }
.cat-cell strong { display: block; font-size: 16px; }
.cat-cell p { margin: 4px 0 0; color: var(--muted); font-size: 12px; }
.mini-stack { display: grid; gap: 4px; color: var(--muted); }
.personality-tag { max-width: 140px; overflow: hidden; text-overflow: ellipsis; }
.status-select { width: 116px; }
.row-actions { display: flex; gap: 6px; flex-wrap: wrap; }

.photo-editor {
  display: grid;
  grid-template-columns: 136px 1fr;
  gap: 20px;
  align-items: center;
  padding: 18px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: linear-gradient(135deg, #fff8ec, #fff1d9);
}

.photo-frame {
  display: grid;
  place-items: center;
  width: 136px;
  height: 136px;
  border-radius: 28px;
  background: repeating-linear-gradient(135deg, #ffe9bf 0 10px, #fff5df 10px 20px);
  box-shadow: inset 0 0 0 1px rgba(154, 90, 44, 0.14);
}

.photo-editor__content { display: grid; gap: 10px; }
.photo-editor__content h3 { margin: 0; font-size: 18px; }
.photo-editor__content p { margin: 4px 0 0; color: var(--muted); }
.upload-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.form-tip { color: var(--muted); font-size: 13px; }
.form-section-title { margin: 18px 0 12px; color: var(--coffee); font-weight: 900; letter-spacing: 0.08em; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 18px; }
.form-grid :deep(.el-select), .form-grid :deep(.el-input-number) { width: 100%; }

.cat-detail { display: grid; gap: 18px; }
.detail-cover {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, #fff8ec, #ffe7bd);
}
.detail-cover h2 { margin: 0; font-family: Georgia, 'Times New Roman', serif; font-size: 30px; }
.detail-cover p:last-child { margin: 6px 0 0; color: var(--muted); }
.cat-detail :deep(.el-descriptions) { width: 100%; }

.health-panel {
  display: grid;
  gap: 14px;
  margin-top: 4px;
}

.health-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.health-panel__header h3 {
  margin: 0;
  color: var(--coffee);
  font-size: 20px;
}

.health-panel__header p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.chart-card {
  min-height: 300px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fff8ec;
}

.weight-chart {
  width: 100%;
  height: 280px;
}

.health-timeline {
  padding: 8px 4px 0;
}

.record-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.74);
}

.record-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.record-card strong {
  color: var(--coffee);
}

.cat-dialog :deep(.el-dialog) { border-radius: 24px; overflow: hidden; }
.cat-dialog :deep(.el-dialog__header) { padding: 22px 24px 10px; }
.cat-dialog :deep(.el-dialog__body) { padding: 14px 24px 4px; }
.cat-dialog :deep(.el-dialog__footer) { padding: 14px 24px 24px; }

@media (max-width: 760px) {
  .cat-page { padding: 16px; }
  .cat-hero { display: grid; }
  .insight-grid { grid-template-columns: 1fr; }
  .photo-editor, .form-grid { grid-template-columns: 1fr; }
  .photo-frame { width: 100%; }
  .health-panel__header { display: grid; }
}
</style>
