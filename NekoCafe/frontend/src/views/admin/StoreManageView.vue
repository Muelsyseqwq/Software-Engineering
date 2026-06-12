<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 门店管理</p>
        <h1>门店管理</h1>
        <p>创建、编辑与管理各城市门店信息。</p>
      </div>
    </header>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-select v-model="filterCity" placeholder="城市筛选" clearable style="width:160px">
        <el-option v-for="c in cityOptions" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:140px; margin-left:8px">
        <el-option label="营业中" value="OPEN" />
        <el-option label="筹备中" value="PREPARING" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" style="margin-left:auto" @click="openCreate">创建门店</el-button>
    </div>

    <!-- Store table -->
    <el-table :data="filteredStores" border v-loading="loading" empty-text="暂无门店数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="address" label="地址" min-width="180" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="营业时间" width="160">
        <template #default="{ row }">
          {{ fmtTime(row.openingTime) }} - {{ fmtTime(row.closingTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑门店' : '创建门店'" width="640px" :close-on-click-modal="false">
      <el-form :model="form" label-position="top">
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="门店名称" required>
              <el-input v-model="form.name" placeholder="如：NekoCafé 朝阳大悦城店" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="营业中" value="OPEN" />
                <el-option label="筹备中" value="PREPARING" />
                <el-option label="已关闭" value="CLOSED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="城市" required>
              <el-input v-model="form.city" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" placeholder="如：010-88886666" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址" required>
          <el-input v-model="form.address" placeholder="详细地址" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始营业时间">
              <el-time-picker v-model="form.openingTime" placeholder="选择时间" format="HH:mm" value-format="HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束营业时间">
              <el-time-picker v-model="form.closingTime" placeholder="选择时间" format="HH:mm" value-format="HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="商圈">
              <el-input v-model="form.businessArea" placeholder="如：核心商圈" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面积（㎡）">
              <el-input-number v-model="form.areaSquareMeter" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="form.latitude" :precision="7" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="form.longitude" :precision="7" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="门店描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="门店介绍" />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex; gap:12px; align-items:flex-start">
            <el-input v-model="form.coverUrl" placeholder="输入封面图 URL 或上传图片" style="flex:1" />
            <el-upload
              :http-request="handleUpload"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp,image/gif"
            >
              <el-button type="primary" :loading="uploading">上传图片</el-button>
            </el-upload>
          </div>
          <img v-if="form.coverUrl" :src="form.coverUrl" style="margin-top:8px; max-width:200px; max-height:120px; border-radius:8px; object-fit:cover" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminStoreList, createStore, updateStore, deleteStore, uploadStorePhoto,
  type AdminStoreDetailRow, type CreateStoreRequest,
} from '@/api/admin'

const stores = ref<AdminStoreDetailRow[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const filterCity = ref('')
const filterStatus = ref('')

// Create/Edit
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<CreateStoreRequest>({
  name: '', city: '', address: '', phone: '',
  openingTime: undefined, closingTime: undefined, status: 'OPEN',
  description: '', businessArea: '', areaSquareMeter: undefined,
  latitude: undefined, longitude: undefined, coverUrl: '',
})

const cityOptions = computed(() => [...new Set(stores.value.map(s => s.city))].sort())

const filteredStores = computed(() => {
  return stores.value.filter(s => {
    if (filterCity.value && s.city !== filterCity.value) return false
    if (filterStatus.value && s.status !== filterStatus.value) return false
    return true
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    stores.value = await fetchAdminStoreList()
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载门店列表失败')
  } finally { loading.value = false }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '', city: '', address: '', phone: '',
    openingTime: undefined, closingTime: undefined, status: 'OPEN',
    description: '', businessArea: '', areaSquareMeter: undefined,
    latitude: undefined, longitude: undefined, coverUrl: '',
  })
}

function openCreate() { resetForm(); dialogVisible.value = true }

function openEdit(row: AdminStoreDetailRow) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, city: row.city, address: row.address,
    phone: row.phone || '', openingTime: row.openingTime,
    closingTime: row.closingTime, status: row.status,
    description: row.description || '', businessArea: row.businessArea || '',
    areaSquareMeter: row.areaSquareMeter, latitude: row.latitude,
    longitude: row.longitude, coverUrl: row.coverUrl || '',
  })
  dialogVisible.value = true
}

async function handleUpload(options: { file: File }) {
  uploading.value = true
  try {
    const result = await uploadStorePhoto(options.file)
    form.coverUrl = result.url
    ElMessage.success('封面图上传成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally { uploading.value = false }
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('请输入门店名称'); return }
  if (!form.city.trim()) { ElMessage.warning('请输入城市'); return }
  if (!form.address.trim()) { ElMessage.warning('请输入地址'); return }

  saving.value = true
  try {
    if (editingId.value) {
      await updateStore(editingId.value, form)
      ElMessage.success('门店已更新')
    } else {
      await createStore(form)
      ElMessage.success('门店已创建')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该门店？删除后门店将不再显示。', '删除确认', { type: 'warning' })
    await deleteStore(id)
    ElMessage.success('已删除')
    await loadData()
  } catch { /* cancelled */ }
}

// --- helpers ---
function fmtTime(v?: string) { return v ? v.slice(0, 5) : '--:--' }
function statusLabel(s: string) { const m: Record<string, string> = { OPEN: '营业中', PREPARING: '筹备中', CLOSED: '已关闭' }; return m[s] || s }
function statusTagType(s: string) { if (s === 'OPEN') return 'success'; if (s === 'PREPARING') return 'warning'; return 'info' }
</script>

<style scoped>
.role-page { display: grid; gap: 16px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.toolbar { display: flex; align-items: center; gap: 8px; }
</style>
