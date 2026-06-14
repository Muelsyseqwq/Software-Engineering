<template>
  <section class="page-card my-orders-page">
    <header class="page-header">
      <div><p class="eyebrow">ORDER LIFECYCLE</p><h1>我的订单</h1><p>跟踪待支付、制作中、已完成订单，并处理评价与退款申请。</p></div>
      <el-button round :loading="loading" @click="loadOrders">刷新订单</el-button>
    </header>

    <section v-loading="loading" class="section-card">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单，先去结算页创建一笔订单吧" />
      <div v-else class="orders-stack">
        <article v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-top">
            <div><p class="eyebrow">{{ order.orderNo }}</p><h2>{{ order.storeName }}</h2><span>{{ formatTime(order.createdAt) }}{{ order.tableNo ? ` · 桌号 ${order.tableNo}` : '' }} · ¥{{ Number(order.totalAmount).toFixed(2) }}</span></div>
            <div class="status-tags"><el-tag :type="statusTag(order.status)">{{ statusText(order.status) }}</el-tag><el-tag v-if="order.refundStatus && order.refundStatus !== 'NONE'" type="danger" effect="plain">{{ refundText(order.refundStatus) }}</el-tag></div>
          </div>
          <div class="items-line">{{ order.items.map((item) => `${item.dishName}×${item.quantity}`).join(' / ') }}</div>
          <div class="timeline">
            <span>创建：{{ formatTime(order.createdAt) }}</span>
            <span v-if="order.paidAt">支付：{{ formatTime(order.paidAt) }}</span>
            <span v-if="order.completedAt">完成：{{ formatTime(order.completedAt) }}</span>
          </div>
          <div v-if="refundByOrderId.get(order.id)" class="refund-detail">
            <strong>{{ refundText(refundByOrderId.get(order.id)?.status || order.refundStatus || '') }}</strong>
            <p>申请原因：{{ refundByOrderId.get(order.id)?.reason || '未填写' }}</p>
            <p v-if="refundByOrderId.get(order.id)?.status === 'APPROVED'">同意理由：{{ refundByOrderId.get(order.id)?.reviewRemark || '审核人未填写说明' }}</p>
            <p v-else-if="refundByOrderId.get(order.id)?.status === 'REJECTED'">驳回理由：{{ refundByOrderId.get(order.id)?.reviewRemark || '审核人未填写说明' }}</p>
            <p v-else>审核说明：{{ refundByOrderId.get(order.id)?.reviewRemark || '审核中，请等待门店处理' }}</p>
            <small v-if="refundByOrderId.get(order.id)?.reviewedAt">审核时间：{{ formatTime(refundByOrderId.get(order.id)?.reviewedAt) }}</small>
          </div>
          <div v-if="order.reviewed" class="review-detail">
            <div class="review-detail-header">
              <strong>我的评价</strong>
              <el-rate v-if="order.reviewRating" :model-value="order.reviewRating" disabled />
            </div>
            <p>{{ order.reviewContent || '未填写文字评价' }}</p>
            <small v-if="order.reviewCreatedAt">评价时间：{{ formatTime(order.reviewCreatedAt) }}</small>
          </div>
          <div class="neko-action-bar">
            <el-button v-if="order.canPay" type="primary" :loading="payingId === order.id" @click="pay(order)">继续支付</el-button>
            <el-button v-if="order.canCancel" type="info" plain :loading="cancellingId === order.id" @click="cancel(order)">取消订单</el-button>
            <el-button v-if="order.canRefund" type="warning" plain @click="openRefund(order)">申请退款</el-button>
            <el-button v-if="order.canReview" type="success" plain @click="openReview(order)">去评价</el-button>
            <el-tag v-if="order.reviewed" type="success" effect="plain">已评价</el-tag>
          </div>
        </article>
      </div>
    </section>

    <el-dialog v-model="reviewDialog" title="评价订单" width="420px" append-to-body :lock-scroll="false" align-center>
      <el-rate v-model="reviewForm.rating" size="large" />
      <el-input v-model="reviewForm.content" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="分享这次猫咖体验" class="dialog-input" />
      <template #footer><el-button @click="reviewDialog = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitReview">提交评价</el-button></template>
    </el-dialog>

    <el-dialog v-model="refundDialog" title="申请退款" width="420px">
      <el-input v-model="refundReason" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="请填写退款原因" />
      <template #footer><el-button @click="refundDialog = false">取消</el-button><el-button type="warning" :loading="submitting" @click="submitRefund">提交申请</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelOrder, fetchMyOrders, type OrderResponse } from '@/api/order'
import { sandboxPay } from '@/api/payment'
import { applyRefund, createReview, fetchMyRefunds, type RefundResponse } from '@/api/customer'

const loading = ref(false)
const submitting = ref(false)
const payingId = ref<number>()
const cancellingId = ref<number>()
const orders = ref<OrderResponse[]>([])
const refunds = ref<RefundResponse[]>([])
const selectedOrder = ref<OrderResponse>()
const reviewDialog = ref(false)
const refundDialog = ref(false)
const refundReason = ref('')
const reviewForm = reactive({ rating: 5, content: '' })
const refundByOrderId = computed(() => new Map(refunds.value.map((refund) => [refund.orderId, refund])))

function statusText(status: string) { return ({ CREATED: '待支付', PAID: '已支付', PREPARING: '制作中', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款' } as Record<string, string>)[status] || status }
function refundText(status: string) { return ({ APPLIED: '退款申请中', APPROVED: '退款已通过', REJECTED: '退款被驳回', REFUNDED: '已退款' } as Record<string, string>)[status] || status }
function statusTag(status: string) { return ({ CREATED: 'warning', PAID: 'primary', PREPARING: 'warning', COMPLETED: 'success', CANCELLED: 'info' } as Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'>)[status] || 'info' }
function formatTime(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '-' }
async function loadOrders() { loading.value = true; try { const [orderRows, refundRows] = await Promise.all([fetchMyOrders(), fetchMyRefunds()]); orders.value = orderRows; refunds.value = refundRows } catch (error) { ElMessage.error(error instanceof Error ? error.message : '订单加载失败') } finally { loading.value = false } }
async function pay(order: OrderResponse) { payingId.value = order.id; try { await sandboxPay(order.id); ElMessage.success('支付成功，积分已更新'); await loadOrders() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '支付失败') } finally { payingId.value = undefined } }
async function cancel(order: OrderResponse) { cancellingId.value = order.id; try { await cancelOrder(order.id); ElMessage.success('订单已取消'); await loadOrders() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '取消失败') } finally { cancellingId.value = undefined } }
function openReview(order: OrderResponse) { selectedOrder.value = order; reviewForm.rating = 5; reviewForm.content = ''; reviewDialog.value = true }
function openRefund(order: OrderResponse) { selectedOrder.value = order; refundReason.value = ''; refundDialog.value = true }
async function submitReview() { if (!selectedOrder.value) return; submitting.value = true; try { await createReview(selectedOrder.value.id, reviewForm); ElMessage.success('评价已提交'); reviewDialog.value = false; await loadOrders() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '评价失败') } finally { submitting.value = false } }
async function submitRefund() { if (!selectedOrder.value) return; submitting.value = true; try { await applyRefund(selectedOrder.value.id, { reason: refundReason.value }); ElMessage.success('退款申请已提交'); refundDialog.value = false; await loadOrders() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '退款申请失败') } finally { submitting.value = false } }
onMounted(loadOrders)
</script>

<style scoped>
.orders-stack { display: grid; gap: 16px; }
.order-card { border-radius: 24px; padding: 20px; background: linear-gradient(135deg, rgba(255,255,255,0.84), rgba(255,247,237,0.74)); border: 1px solid rgba(217,119,6,0.1); }
.order-top { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.order-top h2 { margin: 8px 0 6px; color: #3b2618; }
.order-top span, .timeline { color: rgba(59,38,24,0.58); }
.status-tags { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.items-line { margin: 16px 0; border-radius: 16px; padding: 12px 14px; background: rgba(255,240,220,0.7); color: #7c3f12; font-weight: 800; }
.timeline { display: flex; gap: 14px; flex-wrap: wrap; font-size: 13px; }
.refund-detail { display: grid; gap: 6px; margin-top: 14px; border-radius: 16px; padding: 12px 14px; background: rgba(248, 113, 113, 0.08); color: #7f1d1d; }
.refund-detail p { margin: 0; color: #8a4b42; line-height: 1.6; }
.refund-detail small { color: #9f665f; }
.review-detail { display: grid; gap: 8px; margin-top: 14px; border-radius: 16px; padding: 12px 14px; background: rgba(34, 197, 94, 0.08); color: #166534; }
.review-detail-header { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.review-detail p { margin: 0; color: #3f6d42; line-height: 1.6; }
.review-detail small { color: #5c8a5f; }
.dialog-input { margin-top: 18px; }
@media (max-width: 700px) { .order-top { flex-direction: column; } .status-tags { justify-content: flex-start; } }
</style>
