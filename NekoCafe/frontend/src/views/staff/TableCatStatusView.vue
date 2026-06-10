<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 状态</p>
        <h1>桌位与猫咪状态</h1>
        <p>查看门店桌位和猫咪的当前状态，支持按状态筛选。店员可修改桌位状态。</p>
      </div>
    </header>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <el-tab-pane label="桌位状态" name="tables">
        <div class="filter-bar">
          <el-select v-model="tableStatusFilter" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="空闲" value="AVAILABLE" />
            <el-option label="使用中" value="OCCUPIED" />
            <el-option label="已预约" value="RESERVED" />
            <el-option label="清洁中" value="CLEANING" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
          <el-button type="primary" @click="loadTables">查询</el-button>
        </div>

        <div class="table-wrap">
          <el-table :data="tables" border empty-text="暂无桌位数据">
            <el-table-column prop="tableNo" label="桌号" width="100" />
            <el-table-column prop="area" label="区域" width="130" />
            <el-table-column prop="capacity" label="容纳人数" width="100" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)">{{ translateTableStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280">
              <template #default="{ row }">
                <el-button size="small" @click="openStatusDialog(row)">修改状态</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="猫咪状态" name="cats">
        <div class="filter-bar">
          <el-select v-model="catStatusFilter" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="可互动" value="AVAILABLE" />
            <el-option label="休息中" value="RESTING" />
            <el-option label="生病/不可互动" value="SICK" />
            <el-option label="下架" value="OFFLINE" />
          </el-select>
          <el-button type="primary" @click="loadCats">查询</el-button>
        </div>

        <div class="table-wrap">
          <el-table :data="cats" border empty-text="暂无猫咪数据">
            <el-table-column prop="name" label="名字" width="100" />
            <el-table-column prop="breed" label="品种" width="120" />
            <el-table-column prop="age" label="年龄" width="80" />
            <el-table-column prop="gender" label="性别" width="80" />
            <el-table-column prop="personality" label="性格" min-width="140" show-overflow-tooltip />
            <el-table-column prop="healthStatus" label="健康状态" width="120" />
            <el-table-column prop="status" label="互动状态" width="120">
              <template #default="{ row }">
                <el-tag :type="catStatusTagType(row.status)">{{ translateCatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 修改桌位状态弹窗 -->
    <el-dialog v-model="statusDialogVisible" title="修改桌位状态" width="400px">
      <el-form :model="statusForm" label-width="80px">
        <el-form-item label="桌号">
          <span>{{ statusForm.tableNo }}</span>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.newStatus" placeholder="请选择状态">
            <el-option label="空闲" value="AVAILABLE" />
            <el-option label="使用中" value="OCCUPIED" />
            <el-option label="已预约" value="RESERVED" />
            <el-option label="清洁中" value="CLEANING" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="修改原因">
          <el-input v-model="statusForm.reason" type="textarea" rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatusChange">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCats, fetchTables, updateTableStatus, type Cat, type DiningTable } from '@/api/staff'

const activeTab = ref('tables')
const tables = ref<DiningTable[]>([])
const cats = ref<Cat[]>([])
const tableStatusFilter = ref('')
const catStatusFilter = ref('')

const statusDialogVisible = ref(false)
const statusForm = ref({
  tableId: 0,
  tableNo: '',
  newStatus: '',
  reason: ''
})

async function loadTables() {
  try {
    tables.value = await fetchTables(tableStatusFilter.value || undefined)
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '加载桌位数据失败')
  }
}

async function loadCats() {
  try {
    cats.value = await fetchCats(catStatusFilter.value || undefined)
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '加载猫咪数据失败')
  }
}

function onTabChange() {
  if (activeTab.value === 'tables') {
    loadTables()
  } else {
    loadCats()
  }
}

function translateTableStatus(status: string) {
  const map: Record<string, string> = {
    AVAILABLE: '空闲',
    OCCUPIED: '使用中',
    RESERVED: '已预约',
    CLEANING: '清洁中',
    DISABLED: '停用'
  }
  return map[status] || status
}

function statusTagType(status: string) {
  switch (status) {
    case 'AVAILABLE': return 'success'
    case 'OCCUPIED': return 'danger'
    case 'RESERVED': return 'warning'
    case 'CLEANING': return 'info'
    case 'DISABLED': return ''
    default: return ''
  }
}

function translateCatStatus(status: string) {
  const map: Record<string, string> = {
    AVAILABLE: '可互动',
    RESTING: '休息中',
    SICK: '生病/不可互动',
    OFFLINE: '下架'
  }
  return map[status] || status
}

function catStatusTagType(status: string) {
  switch (status) {
    case 'AVAILABLE': return 'success'
    case 'RESTING': return 'warning'
    case 'SICK': return 'danger'
    case 'OFFLINE': return 'info'
    default: return ''
  }
}

function openStatusDialog(row: DiningTable) {
  statusForm.value = {
    tableId: row.id,
    tableNo: row.tableNo,
    newStatus: row.status,
    reason: ''
  }
  statusDialogVisible.value = true
}

async function submitStatusChange() {
  try {
    await updateTableStatus(statusForm.value.tableId, statusForm.value.newStatus, statusForm.value.reason)
    ElMessage.success('桌位状态已更新')
    statusDialogVisible.value = false
    await loadTables()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

onMounted(() => {
  loadTables()
})
</script>

<style scoped>
.role-page { display: grid; gap: 18px; min-width: 0; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.filter-bar { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
.table-wrap { overflow-x: auto; }
</style>
