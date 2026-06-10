<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 店长管理</p>
        <h1>店长管理</h1>
        <p>管理各门店的店长身份，分配或移除店长。</p>
      </div>
      <el-button type="primary" @click="openAssign">分配店长</el-button>
    </header>

    <el-table :data="managers" border v-loading="loading" empty-text="暂无店长数据">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="storeName" label="所属门店" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '在职' : row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleRemove(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Assign dialog -->
    <el-dialog v-model="dialogVisible" title="分配店长" width="480px">
      <el-form label-position="top">
        <el-form-item label="选择用户">
          <el-select v-model="assignForm.userId" placeholder="请选择用户" filterable style="width:100%">
            <el-option v-for="u in users" :key="u.id" :label="`${u.nickname}（${u.username}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分配门店">
          <el-select v-model="assignForm.storeId" placeholder="请选择门店" style="width:100%">
            <el-option v-for="s in stores" :key="s.id" :label="`${s.name}（${s.city}）`" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchStoreManagers, assignStoreManager, removeStoreManager, fetchAdminUsers, fetchAdminStores, type StoreManagerRow, type AdminUserRow, type AdminStoreRow } from '@/api/admin'

const managers = ref<StoreManagerRow[]>([])
const users = ref<AdminUserRow[]>([])
const stores = ref<AdminStoreRow[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const assignForm = reactive({ userId: 0, storeId: 0 })

onMounted(() => { loadData(); loadUsersAndStores() })

async function loadData() {
  loading.value = true
  try {
    managers.value = await fetchStoreManagers()
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载店长数据失败')
  } finally { loading.value = false }
}

async function loadUsersAndStores() {
  try {
    users.value = await fetchAdminUsers()
    stores.value = await fetchAdminStores()
  } catch { /* silent */ }
}

function openAssign() {
  assignForm.userId = 0
  assignForm.storeId = 0
  dialogVisible.value = true
}

async function handleAssign() {
  if (!assignForm.userId || !assignForm.storeId) {
    ElMessage.warning('请选择用户和门店')
    return
  }
  try {
    await assignStoreManager(assignForm.userId, assignForm.storeId)
    ElMessage.success('店长分配成功')
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '分配店长失败')
  }
}

async function handleRemove(row: StoreManagerRow) {
  try {
    await ElMessageBox.confirm(
      `确认移除 ${row.nickname}（${row.username}）在「${row.storeName}」的店长身份？`,
      '移除确认',
      { type: 'warning' },
    )
    await removeStoreManager(row.userId, row.storeId)
    ElMessage.success('店长已移除')
    await loadData()
  } catch { /* cancelled */ }
}

function fmt(v?: string) { return v ? v.slice(0, 16) : '-' }
</script>

<style scoped>
.role-page { display: grid; gap: 16px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
</style>
