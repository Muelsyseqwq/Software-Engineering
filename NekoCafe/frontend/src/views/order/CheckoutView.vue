<template>
  <section class="page-card checkout-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">SANDBOX CHECKOUT</p>
        <h1>猫爪订单结算</h1>
        <p>选择门店与菜品后创建订单，再用沙箱支付完成顾客端主流程演示。</p>
      </div>
      <el-select v-model="selectedStoreId" class="store-select" placeholder="选择门店" size="large" @change="loadMenu">
        <el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" />
      </el-select>
    </header>

    <div class="checkout-grid">
      <section v-loading="loading" class="section-card menu-picker">
        <div class="section-heading">
          <div>
            <p class="eyebrow">STEP 01</p>
            <h2>选择今日菜品</h2>
          </div>
          <el-tag type="warning" effect="plain">{{ selectedCount }} 件</el-tag>
        </div>

        <el-empty v-if="!loading && categories.length === 0" description="请选择门店后加载菜单" />
        <div v-for="category in categories" :key="category.id" class="category-block">
          <h3>{{ category.name }}</h3>
          <div class="dish-list">
            <article v-for="dish in category.dishes" :key="dish.id" class="dish-row">
              <div class="dish-avatar">{{ dish.name.slice(0, 1) }}</div>
              <div class="dish-copy">
                <strong>{{ dish.name }}</strong>
                <span>{{ dish.description || '猫咖今日小食，适合提前点单。' }}</span>
              </div>
              <div class="dish-price">¥{{ Number(dish.price).toFixed(2) }}</div>
              <el-input-number
                v-model="quantities[dish.id]"
                :min="0"
                :max="dish.stock"
                :disabled="dish.stock <= 0"
                controls-position="right"
                size="small"
              />
            </article>
          </div>
        </div>
      </section>

      <aside class="summary-panel section-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">STEP 02</p>
            <h2>确认订单</h2>
          </div>
          <strong class="total">¥{{ totalAmount.toFixed(2) }}</strong>
        </div>

        <div v-if="selectedItems.length" class="summary-list">
          <div v-for="item in selectedItems" :key="item.dish.id" class="summary-item">
            <span>{{ item.dish.name }} × {{ item.quantity }}</span>
            <strong>¥{{ item.subtotal.toFixed(2) }}</strong>
          </div>
        </div>
        <el-empty v-else description="还没有选择菜品" />

        <el-input v-model="remark" type="textarea" :rows="3" maxlength="120" show-word-limit placeholder="备注：例如少糖、靠窗、和猫咪保持距离等" />

        <div class="neko-action-bar">
          <el-button type="primary" size="large" :loading="creating" :disabled="selectedItems.length === 0" @click="submitOrder">
            创建订单
          </el-button>
          <el-button size="large" :loading="paying" :disabled="!currentOrder || currentOrder.status !== 'CREATED'" @click="payOrder">
            沙箱支付
          </el-button>
        </div>

        <div v-if="currentOrder" class="result-card">
          <span>订单 {{ currentOrder.orderNo }}</span>
          <strong>{{ getOrderStatusText(currentOrder.status) }}</strong>
          <small>{{ currentOrder.storeName }} · ¥{{ Number(currentOrder.totalAmount).toFixed(2) }}</small>
        </div>
        <div v-if="payment" class="result-card paid">
          <span>支付流水 {{ payment.paymentNo }}</span>
          <strong>沙箱支付成功</strong>
          <small>{{ payment.channel }} · {{ payment.status }}</small>
        </div>
      </aside>
    </div>

    <section class="section-card order-history">
      <div class="section-heading">
        <div>
          <p class="eyebrow">MY ORDERS</p>
          <h2>我的最近订单</h2>
        </div>
        <el-button round @click="loadOrders">刷新</el-button>
      </div>
      <el-table :data="orders" empty-text="暂无订单，先创建一笔沙箱订单吧">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="storeName" label="门店" min-width="160" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusTagType(row.status)">{{ getOrderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="菜品" min-width="220">
          <template #default="{ row }">{{ formatOrderItems(row) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStoreMenu, type DishItem, type MenuCategory } from '@/api/menu'
import { fetchStores, type StoreSummary } from '@/api/store'
import { createOrder, fetchMyOrders, type OrderResponse } from '@/api/order'
import { sandboxPay, type PaymentResponse } from '@/api/payment'

const route = useRoute()
const loading = ref(false)
const creating = ref(false)
const paying = ref(false)
const stores = ref<StoreSummary[]>([])
const categories = ref<MenuCategory[]>([])
const selectedStoreId = ref<number>()
const quantities = reactive<Record<number, number>>({})
const remark = ref('')
const currentOrder = ref<OrderResponse>()
const payment = ref<PaymentResponse>()
const orders = ref<OrderResponse[]>([])

const selectedItems = computed(() => categories.value
  .flatMap((category) => category.dishes)
  .map((dish) => ({ dish, quantity: quantities[dish.id] || 0, subtotal: Number(dish.price) * (quantities[dish.id] || 0) }))
  .filter((item) => item.quantity > 0))

const selectedCount = computed(() => selectedItems.value.reduce((sum, item) => sum + item.quantity, 0))
const totalAmount = computed(() => selectedItems.value.reduce((sum, item) => sum + item.subtotal, 0))

async function loadStores() {
  stores.value = await fetchStores()
  const queryStoreId = Number(route.query.storeId)
  selectedStoreId.value = stores.value.some((store) => store.id === queryStoreId) ? queryStoreId : stores.value[0]?.id
}

async function loadMenu() {
  if (!selectedStoreId.value) return
  loading.value = true
  categories.value = []
  Object.keys(quantities).forEach((key) => { delete quantities[Number(key)] })
  try {
    categories.value = await fetchStoreMenu(selectedStoreId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '菜单加载失败')
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (!selectedStoreId.value || selectedItems.value.length === 0) return
  creating.value = true
  payment.value = undefined
  try {
    currentOrder.value = await createOrder({
      storeId: selectedStoreId.value,
      items: selectedItems.value.map((item: { dish: DishItem; quantity: number }) => ({ dishId: item.dish.id, quantity: item.quantity })),
      remark: remark.value,
    })
    ElMessage.success('订单已创建，请继续沙箱支付')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建订单失败')
  } finally {
    creating.value = false
  }
}

async function payOrder() {
  if (!currentOrder.value) return
  paying.value = true
  try {
    payment.value = await sandboxPay(currentOrder.value.id)
    currentOrder.value = { ...currentOrder.value, status: 'PAID' }
    ElMessage.success('沙箱支付成功，订单已完成支付')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '沙箱支付失败')
  } finally {
    paying.value = false
  }
}

function formatOrderItems(order: OrderResponse) {
  return order.items.map((item) => `${item.dishName}×${item.quantity}`).join(' / ')
}

function getOrderStatusText(status: string) {
  const statusMap: Record<string, string> = {
    CREATED: '待支付',
    PAID: '已支付，待制作',
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
    orders.value = await fetchMyOrders()
  } catch {
    orders.value = []
  }
}

onMounted(async () => {
  try {
    await loadStores()
    await loadMenu()
    await loadOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结算页加载失败')
  }
})
</script>

<style scoped>
.checkout-page { display: grid; gap: 22px; }
.store-select { width: 300px; }
.checkout-grid { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 20px; align-items: start; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.section-heading h2 { margin: 0; color: #3b2618; }
.menu-picker { min-height: 420px; }
.category-block { display: grid; gap: 12px; margin-top: 18px; }
.category-block h3 { margin: 0; color: #5d3922; }
.dish-list { display: grid; gap: 12px; }
.dish-row { display: grid; grid-template-columns: 52px minmax(0, 1fr) auto 108px; gap: 12px; align-items: center; border: 1px solid rgba(217, 119, 6, 0.12); border-radius: 20px; padding: 12px; background: rgba(255, 255, 255, 0.58); }
.dish-avatar { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 17px; background: linear-gradient(135deg, #fed7aa, #fff7ed); color: #9a3412; font-weight: 900; font-size: 22px; }
.dish-copy { display: grid; gap: 4px; }
.dish-copy strong { color: #3b2618; }
.dish-copy span { color: #7c5f4a; font-size: 13px; line-height: 1.5; }
.dish-price { color: #d97706; font-size: 18px; font-weight: 900; }
.summary-panel { position: sticky; top: 18px; display: grid; gap: 16px; }
.total { color: #d97706; font-size: 28px; }
.summary-list { display: grid; gap: 10px; }
.summary-item { display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px dashed #f0cfaa; padding-bottom: 8px; color: #5d3922; }
.result-card { display: grid; gap: 4px; border-radius: 18px; padding: 14px; background: rgba(217, 119, 6, 0.09); color: #5d3922; }
.result-card strong { color: #d97706; font-size: 18px; }
.result-card small { color: #7c5f4a; }
.result-card.paid { background: rgba(111, 148, 93, 0.12); }
.result-card.paid strong { color: #537245; }
.order-history { display: grid; gap: 12px; }
@media (max-width: 980px) {
  .checkout-grid { grid-template-columns: 1fr; }
  .summary-panel { position: static; }
}
@media (max-width: 680px) {
  .store-select { width: 100%; }
  .dish-row { grid-template-columns: 44px 1fr; }
  .dish-price { grid-column: 2; }
}
</style>
