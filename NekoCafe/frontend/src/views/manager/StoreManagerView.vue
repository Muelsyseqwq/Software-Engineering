<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STORE_MANAGER 店长</p>
        <h1>门店管理</h1>
        <p>查看门店信息、维护桌位，并查看本店预约。组员可基于此页面继续完善。</p>
      </div>
      <el-button type="primary" @click="loadData">刷新</el-button>
    </header>

    <el-card class="section-card" header="门店信息">
      <div v-if="store" class="store-grid">
        <span>门店：{{ store.name }}</span>
        <span>城市：{{ store.city }}</span>
        <span>地址：{{ store.address }}</span>
        <span>状态：<el-tag>{{ store.status }}</el-tag></span>
      </div>
      <el-empty v-else description="门店信息接口待接入" />
    </el-card>

    <el-card class="section-card" header="桌位管理">
      <template #header>
        <div class="card-header"><span>桌位管理</span><el-button size="small" @click="openCreateDialog">新增桌位</el-button></div>
      </template>
      <el-table :data="tables" border empty-text="暂无桌位数据">
        <el-table-column prop="tableNo" label="桌号" />
        <el-table-column prop="capacity" label="容量" />
        <el-table-column prop="area" label="区域" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作"><template #default="{ row }"><el-button size="small" @click="openEditDialog(row)">编辑</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card" header="本店预约">
      <el-table :data="reservations" border empty-text="暂无预约数据">
        <el-table-column prop="reservationNo" label="预约号" />
        <el-table-column prop="customerName" label="顾客" />
        <el-table-column prop="partySize" label="人数" />
        <el-table-column prop="slotTime" label="时段" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑桌位' : '新增桌位'" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="桌号"><el-input v-model="form.tableNo" placeholder="A01" /></el-form-item>
        <el-form-item label="容量"><el-input v-model.number="form.capacity" placeholder="4" /></el-form-item>
        <el-form-item label="区域"><el-input v-model="form.area" placeholder="猫咪互动区" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="form.status" placeholder="AVAILABLE" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createManagerTable, fetchManagerReservations, fetchManagerStore, fetchManagerTables, updateManagerTable, type ManagerReservationRow, type ManagerStoreInfo, type ManagerTableRow } from '@/api/manager'

const store = ref<ManagerStoreInfo>()
const tables = ref<ManagerTableRow[]>([])
const reservations = ref<ManagerReservationRow[]>([])
const dialogVisible = ref(false)
const editingId = ref<number>()
const form = reactive<ManagerTableRow>({ tableNo: '', capacity: 2, area: '', status: 'AVAILABLE' })

function resetForm() {
  editingId.value = undefined
  Object.assign(form, { tableNo: '', capacity: 2, area: '', status: 'AVAILABLE' })
}

async function loadData() {
  try {
    store.value = await fetchManagerStore()
    tables.value = await fetchManagerTables()
    reservations.value = await fetchManagerReservations()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '店长接口待接入')
  }
}

function openCreateDialog() { resetForm(); dialogVisible.value = true }
function openEditDialog(row: ManagerTableRow) { editingId.value = row.id; Object.assign(form, row); dialogVisible.value = true }

async function handleSubmit() {
  try {
    if (editingId.value) await updateManagerTable(editingId.value, form)
    else await createManagerTable(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '桌位保存接口待接入')
  }
}

onMounted(loadData)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header, .card-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.store-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
</style>
