<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">HQ_OPERATOR / 用户与角色</p>
        <h1>用户与角色</h1>
        <p>管理平台用户、查看角色与门店信息。</p>
      </div>
      <el-button type="primary" @click="loadAll">刷新</el-button>
    </header>

    <!-- Stat cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card><strong>{{ users.length }}</strong><span>用户数量</span></el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card><strong>{{ roles.length }}</strong><span>角色数量</span></el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card><strong>{{ stores.length }}</strong><span>门店数量</span></el-card>
      </el-col>
    </el-row>

    <el-tabs type="card">
      <!-- Users -->
      <el-tab-pane label="用户列表">
        <el-table :data="users" border v-loading="loading" empty-text="暂无用户数据">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="phone" label="手机号" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="160">
            <template #default="{ row }">
              <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right:4px">{{ roleLabel(r) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.status === 'ACTIVE'" size="small" type="warning" @click="toggleStatus(row.id, 'DISABLED')">停用</el-button>
              <el-button v-else size="small" type="success" @click="toggleStatus(row.id, 'ACTIVE')">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Roles -->
      <el-tab-pane label="角色列表">
        <el-table :data="roles" border empty-text="暂无角色数据">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="code" label="角色编码" />
          <el-table-column prop="name" label="角色名称" />
          <el-table-column prop="description" label="说明" />
        </el-table>
      </el-tab-pane>

      <!-- Stores -->
      <el-tab-pane label="门店列表">
        <el-table :data="stores" border empty-text="暂无门店数据">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="门店名称" />
          <el-table-column prop="city" label="城市" />
          <el-table-column prop="address" label="地址" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'OPEN' ? 'success' : row.status === 'PREPARING' ? 'warning' : 'info'">
                {{ row.status === 'OPEN' ? '营业中' : row.status === 'PREPARING' ? '筹备中' : row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminRoles, fetchAdminStores, fetchAdminUsers, updateUserStatus, type AdminRoleRow, type AdminStoreRow, type AdminUserRow } from '@/api/admin'

const users = ref<AdminUserRow[]>([])
const roles = ref<AdminRoleRow[]>([])
const stores = ref<AdminStoreRow[]>([])
const loading = ref(false)

async function loadAll() {
  loading.value = true
  try {
    users.value = await fetchAdminUsers()
    roles.value = await fetchAdminRoles()
    stores.value = await fetchAdminStores()
  } catch (e) {
    ElMessage.warning(e instanceof Error ? e.message : '加载数据失败')
  } finally { loading.value = false }
}

async function toggleStatus(userId: number, newStatus: string) {
  try {
    await updateUserStatus(userId, newStatus)
    ElMessage.success('用户状态已更新')
    await loadAll()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败')
  }
}

function roleLabel(code: string) {
  const m: Record<string, string> = {
    CUSTOMER: '顾客', STAFF: '店员', STORE_MANAGER: '店长',
    HQ_OPERATOR: '总部运营', CAT_CARETAKER: '猫咪管家', ADMIN: '管理员',
  }
  return m[code] || code
}

onMounted(loadAll)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.stat-row strong { font-size: 26px; color: #3b2618; }
.stat-row span { color: #7c6554; }
</style>
