<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 店长管理</p>
        <h1>店长管理</h1>
        <p>管理各门店的店长身份，可从已有用户中选择或新建用户直接任命为店长。</p>
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
    <el-dialog v-model="dialogVisible" title="分配店长" width="520px">
      <el-tabs v-model="assignMode" class="assign-tabs">
        <!-- Tab 1: select existing user -->
        <el-tab-pane label="从已有用户中选择" name="existing">
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
        </el-tab-pane>

        <!-- Tab 2: create new user -->
        <el-tab-pane label="新建用户并任命" name="new">
          <el-form label-position="top" :model="newUserForm" :rules="newUserRules" ref="newUserFormRef">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="newUserForm.username" placeholder="登录用户名" maxlength="64" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="newUserForm.password" type="password" placeholder="设置登录密码" show-password maxlength="64" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="newUserForm.nickname" placeholder="店长显示昵称" maxlength="64" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="newUserForm.phone" placeholder="选填" maxlength="20" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="newUserForm.email" placeholder="选填" maxlength="128" />
            </el-form-item>
            <el-form-item label="分配门店" prop="storeId">
              <el-select v-model="newUserForm.storeId" placeholder="请选择门店" style="width:100%">
                <el-option v-for="s in stores" :key="s.id" :label="`${s.name}（${s.city}）`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign" :loading="assigning">
          {{ assignMode === 'existing' ? '确认分配' : '创建并任命' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchStoreManagers,
  assignStoreManager,
  removeStoreManager,
  fetchAdminUsers,
  fetchAdminStores,
  createStoreManagerWithUser,
  type StoreManagerRow,
  type AdminUserRow,
  type AdminStoreRow,
  type CreateStoreManagerWithUserRequest,
} from '@/api/admin'

const managers = ref<StoreManagerRow[]>([])
const users = ref<AdminUserRow[]>([])
const stores = ref<AdminStoreRow[]>([])
const loading = ref(false)
const assigning = ref(false)

const dialogVisible = ref(false)
const assignMode = ref('existing')
const assignForm = reactive({ userId: 0, storeId: 0 })

const newUserFormRef = ref()
const newUserForm = reactive<CreateStoreManagerWithUserRequest>({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  storeId: 0,
})

const newUserRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
}

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
  newUserForm.username = ''
  newUserForm.password = ''
  newUserForm.nickname = ''
  newUserForm.phone = ''
  newUserForm.email = ''
  newUserForm.storeId = 0
  assignMode.value = 'existing'
  dialogVisible.value = true
}

async function handleAssign() {
  if (assignMode.value === 'existing') {
    if (!assignForm.userId || !assignForm.storeId) {
      ElMessage.warning('请选择用户和门店')
      return
    }
    assigning.value = true
    try {
      await assignStoreManager(assignForm.userId, assignForm.storeId)
      ElMessage.success('店长分配成功')
      dialogVisible.value = false
      await loadData()
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '分配店长失败')
    } finally { assigning.value = false }
  } else {
    // validate form
    try {
      await newUserFormRef.value?.validate()
    } catch { return }

    if (!newUserForm.storeId) {
      ElMessage.warning('请选择门店')
      return
    }

    assigning.value = true
    try {
      const req: CreateStoreManagerWithUserRequest = {
        username: newUserForm.username,
        password: newUserForm.password,
        nickname: newUserForm.nickname,
        storeId: newUserForm.storeId,
      }
      if (newUserForm.phone) req.phone = newUserForm.phone
      if (newUserForm.email) req.email = newUserForm.email
      await createStoreManagerWithUser(req)
      ElMessage.success('用户创建成功并已任命为店长')
      dialogVisible.value = false
      await loadData()
      await loadUsersAndStores()
    } catch (e) {
      ElMessage.error(e instanceof Error ? e.message : '创建失败')
    } finally { assigning.value = false }
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
.assign-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>
