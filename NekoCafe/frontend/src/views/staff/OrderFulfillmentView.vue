<template>
  <section class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STAFF 履约</p>
        <h1>订单履约</h1>
        <p>处理菜品订单状态：开始制作、完成订单，查看已处理的历史订单。</p>
      </div>
      <el-button type="primary" @click="loadCurrentTab">刷新订单</el-button>
    </header>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange" class="order-tabs">
      <el-tab-pane label="待处理订单" name="pending">
        <div class="table-wrap">
          <el-table :data="pendingList" border empty-text="暂无待处理订单">
            <el-table-column prop="orderNo" label="订单号" min-width="130" />
            <el-table-column prop="summary" label="菜品摘要" min-width="160" />
            <el-table-column prop="amount" label="金额" width="90" />
            <el-table-column prop="tableNo" label="桌号" width="90" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusTagType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="退款" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.refundStatus === 'APPLIED'" type="danger">退款申请中</el-tag>
                <span v-else class="muted">无</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="150" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <span v-if="row.refundStatus === 'APPLIED'" class="muted">等待店长处理退款</span>
                <template v-else>
                  <el-button v-if="row.status === '待支付' || row.status === '已支付/待制作'" size="small" @click="handleStart(row.id)">开始制作</el-button>
                  <el-button v-if="row.status === '制作中'" type="success" size="small" @click="handleComplete(row.id)">完成</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="已处理订单" name="handled">
        <div class="table-wrap">
          <el-table :data="handledList" border empty-text="暂无已处理订单">
            <el-table-column prop="orderNo" label="订单号" min-width="130" />
            <el-table-column prop="summary" label="菜品摘要" min-width="160" />
            <el-table-column prop="amount" label="金额" width="90" />
            <el-table-column prop="tableNo" label="桌号" width="90" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusTagType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="退款" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.refundStatus === 'APPLIED'" type="danger">退款申请中</el-tag>
                <span v-else class="muted">无</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="150" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === '已完成'" size="small" type="primary" text @click="openReviewDialog(row.id)">查看评价</el-button>
                <span v-else-if="row.status === '制作中'">制作中</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 查看评价弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="订单评价" width="460px" :close-on-click-modal="true">
      <div v-if="currentReview">
        <div style="margin-bottom: 12px;">
          <span style="color: #606266;">顾客：</span>
          <span style="font-weight: 600;">{{ currentReview.customerName }}</span>
        </div>
        <div style="margin-bottom: 12px;">
          <span style="color: #606266;">评分：</span>
          <el-rate :model-value="currentReview.rating" disabled show-score text-color="#ff9900" />
        </div>
        <div style="margin-bottom: 12px;">
          <span style="color: #606266;">评价内容：</span>
          <div style="margin-top: 6px; padding: 10px; background: #f5f7fa; border-radius: 6px; line-height: 1.6;">
            {{ currentReview.content || '暂无文字评价' }}
          </div>
        </div>
        <div style="margin-bottom: 4px;">
          <span style="color: #606266;">评价状态：</span>
          <el-tag :type="getReviewStatusTagType(currentReview.status)">{{ currentReview.status }}</el-tag>
        </div>
        <div style="color: #909399; font-size: 13px; margin-top: 8px;">
          评价时间：{{ currentReview.createdAt }}
        </div>
      </div>
      <div v-else style="text-align: center; color: #909399; padding: 30px 0;">
        该订单暂无评价
      </div>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { completeOrder, fetchHandledOrders, fetchOrderReview, fetchPendingOrders, startOrder, type StaffOrderRow, type StaffReviewRow } from '@/api/staff'

const activeTab = ref('pending')
const pendingList = ref<StaffOrderRow[]>([])
const handledList = ref<StaffOrderRow[]>([])
const reviewDialogVisible = ref(false)
const currentReview = ref<StaffReviewRow | null>(null)

function getOrderStatusTagType(status: string) {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    '待支付': 'warning',
    '已支付/待制作': 'primary',
    '制作中': 'warning',
    '已完成': 'success',
    '已取消': 'danger',
    '退款中': 'danger',
    '已退款': 'info',
  }
  return typeMap[status] || 'info'
}

async function loadPending() {
  try {
    pendingList.value = await fetchPendingOrders()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '加载待处理订单失败')
  }
}

async function loadHandled() {
  try {
    handledList.value = await fetchHandledOrders()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '加载已处理订单失败')
  }
}

function loadCurrentTab() {
  if (activeTab.value === 'pending') {
    loadPending()
  } else {
    loadHandled()
  }
}

function onTabChange() {
  loadCurrentTab()
}

async function handleStart(id: number) {
  try {
    await startOrder(id)
    ElMessage.success('已开始制作')
    await loadPending()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function handleComplete(id: number) {
  try {
    await completeOrder(id)
    ElMessage.success('订单已完成')
    await loadPending()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function openReviewDialog(orderId: number) {
  try {
    const review = await fetchOrderReview(orderId)
    currentReview.value = review || null
    reviewDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载评价失败')
  }
}

function getReviewStatusTagType(status: string) {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    '显示中': 'success',
    '已隐藏': 'info',
    '待审核': 'warning',
  }
  return typeMap[status] || 'info'
}

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.role-page { display: grid; gap: 18px; min-width: 0; }
.page-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.order-tabs { min-width: 0; }
.table-wrap { overflow-x: auto; }
.muted { color: #8a6a52; font-size: 12px; }
</style>
