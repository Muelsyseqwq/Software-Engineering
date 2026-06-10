<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">总部运营 / 总控后台</p>
        <h1>总控后台</h1>
        <p>管理用户、角色、门店和系统配置，作为总部运营统一维护平台基础数据的入口。</p>
      </div>
      <el-button type="primary" @click="loadData">刷新</el-button>
    </header>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card>
          <strong>{{ stats.userCount }}</strong>
          <span>用户数量</span>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card>
          <strong>{{ stats.roleCount }}</strong>
          <span>角色数量</span>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card>
          <strong>{{ stats.storeCount }}</strong>
          <span>门店数量</span>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs>
      <el-tab-pane label="用户列表">
        <el-table :data="users" border empty-text="暂无用户数据，接口待接入">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="phone" label="手机号" />
          <el-table-column label="状态">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="角色">
            <template #default="{ row }">
              <el-tag v-for="role in row.roles" :key="role" style="margin-right: 6px">{{ role }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="角色列表">
        <el-table :data="roles" border empty-text="暂无角色数据，接口待接入">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="code" label="角色编码" />
          <el-table-column prop="name" label="角色名称" />
          <el-table-column prop="description" label="说明" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="门店列表">
        <el-table :data="stores" border empty-text="暂无门店数据，接口待接入">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="门店名称" />
          <el-table-column prop="city" label="城市" />
          <el-table-column prop="address" label="地址" />
          <el-table-column label="状态">
            <template #default="{ row }">
              <el-tag :type="storeStatusType(row.status)">{{ storeStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminRoles, fetchAdminStores, fetchAdminUsers, type AdminRoleRow, type AdminStoreRow, type AdminUserRow } from '@/api/admin'

const users = ref<AdminUserRow[]>([])
const roles = ref<AdminRoleRow[]>([])
const stores = ref<AdminStoreRow[]>([])

const stats = computed(() => ({
  userCount: users.value.length,
  roleCount: roles.value.length,
  storeCount: stores.value.length,
}))

async function loadData() {
  try {
    users.value = await fetchAdminUsers()
    roles.value = await fetchAdminRoles()
    stores.value = await fetchAdminStores()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '管理员接口待接入')
  }
}

onMounted(loadData)

function statusLabel(status?: string) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'DISABLED') return '停用'
  return status || '未知'
}

function statusTagType(status?: string) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED') return 'info'
  return 'warning'
}

function storeStatusLabel(status?: string) {
  if (status === 'OPEN') return '营业中'
  if (status === 'CLOSED') return '已打烊'
  if (status === 'PREPARING') return '筹备中'
  return status || '未知'
}

function storeStatusType(status?: string) {
  if (status === 'OPEN') return 'success'
  if (status === 'PREPARING') return 'warning'
  if (status === 'CLOSED') return 'info'
  return 'info'
}
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.stat-row strong { font-size: 26px; color: #3b2618; }
.stat-row span { color: #7c6554; }
</style>
