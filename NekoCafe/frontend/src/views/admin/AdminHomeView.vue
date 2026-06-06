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

    <el-tabs>
      <el-tab-pane label="用户列表">
        <el-table :data="users" border empty-text="暂无用户数据，接口待接入">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="phone" label="手机号" />
          <el-table-column prop="status" label="状态" />
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
          <el-table-column prop="status" label="状态" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminRoles, fetchAdminStores, fetchAdminUsers, type AdminRoleRow, type AdminStoreRow, type AdminUserRow } from '@/api/admin'

const users = ref<AdminUserRow[]>([])
const roles = ref<AdminRoleRow[]>([])
const stores = ref<AdminStoreRow[]>([])

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
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
</style>
