<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 履约</p>
        <h1>订单履约</h1>
        <p>处理菜品订单状态：开始制作、完成订单。</p>
      </div>
      <el-button type="primary" @click="loadOrders">刷新订单</el-button>
    </header>

    <el-table :data="orders" border empty-text="暂无待处理订单">
      <el-table-column prop="orderNo" label="订单号" min-width="140" />
      <el-table-column prop="summary" label="菜品摘要" min-width="180" />
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><el-tag :type="getOrderStatusTagType(row.status)">{{ getOrderStatusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <el-button size="small" :disabled="row.status === 'PREPARING'" @click="handleStart(row.id)">开始制作</el-button>
          <el-button type="success" size="small" @click="handleComplete(row.id)">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { completeOrder, fetchPendingOrders, startOrder, type StaffOrderRow } from '@/api/staff'

const orders = ref<StaffOrderRow[]>([])

function getOrderStatusText(status: string) {
  const statusMap: Record<string, string> = {
    CREATED: '待支付',
    PAID: '待制作',
    PREPARING: '制作中',
    COMPLETED: '已完成',
  }
  return statusMap[status] || '状态未知'
}

function getOrderStatusTagType(status: string) {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    CREATED: 'warning',
    PAID: 'primary',
    PREPARING: 'warning',
    COMPLETED: 'success',
  }
  return typeMap[status] || 'info'
}

async function loadOrders() {
  try {
    orders.value = await fetchPendingOrders()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '订单接口待接入')
  }
}

async function handleStart(id: number) {
  try {
    await startOrder(id)
    ElMessage.success('已开始制作')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '开始制作接口待接入')
  }
}

async function handleComplete(id: number) {
  try {
    await completeOrder(id)
    ElMessage.success('订单已完成')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '完成订单接口待接入')
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
</style>
