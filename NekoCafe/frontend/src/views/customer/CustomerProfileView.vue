<template>
  <section class="page-card profile-page">
    <header class="page-header">
      <div><p class="eyebrow">PAW MEMBER</p><h1>会员与偏好</h1><p>在猫爪兑换柜台查看积分、兑换奖励，也可以继续维护个性化偏好。</p></div>
      <el-button type="primary" round :loading="saving" @click="savePreferences">保存偏好</el-button>
    </header>

    <div class="profile-grid">
      <section v-loading="loading" class="section-card member-card">
        <p class="eyebrow">MEMBER POINTS</p>
        <strong>{{ points?.points ?? 0 }}</strong>
        <span>{{ points?.levelCode || 'NORMAL' }} · 累计消费 ¥{{ Number(points?.totalSpent || 0).toFixed(2) }}</span>
      </section>

      <section class="section-card preference-card">
        <div class="section-heading"><div><p class="eyebrow">PREFERENCES</p><h2>注册偏好</h2></div></div>
        <div v-for="group in preferenceGroups" :key="group.type" class="preference-group">
          <h3>{{ group.label }}</h3>
          <el-checkbox-group v-model="selected[group.type]">
            <el-checkbox-button v-for="item in group.items" :key="item" :label="item">{{ item }}</el-checkbox-button>
          </el-checkbox-group>
        </div>
      </section>
    </div>

    <section class="section-card rewards-card" v-loading="loading">
      <div class="section-heading">
        <div><p class="eyebrow">PAW EXCHANGE</p><h2>猫爪兑换柜台</h2><span>挑选一张猫咖奖励券，用积分兑换到店小惊喜。</span></div>
        <el-button round @click="loadData">刷新</el-button>
      </div>
      <el-empty v-if="!rewards.length" description="暂无可兑换奖励" />
      <div v-else class="reward-grid">
        <article v-for="reward in rewards" :key="reward.id" class="reward-card">
          <div class="ticket-stub">{{ rewardIcon(reward.rewardType) }}</div>
          <div class="reward-body">
            <div class="reward-title"><h3>{{ reward.name }}</h3><el-tag effect="plain">{{ rewardTypeText(reward.rewardType) }}</el-tag></div>
            <p>{{ reward.description || '兑换后到店出示记录即可使用。' }}</p>
            <el-tag size="small" :type="canRedeemByLevel(reward.requiredLevel) ? 'success' : 'warning'" class="level-tag">
              {{ levelLabel(reward.requiredLevel) }}可兑
            </el-tag>
            <div class="reward-meta"><strong>{{ reward.pointsCost }} 积分</strong><span>{{ stockText(reward.stock) }}</span></div>
            <el-button type="primary" round :disabled="!canRedeem(reward)" :loading="redeemingId === reward.id" @click="confirmRedeem(reward)">
              {{ redeemButtonText(reward) }}
            </el-button>
            <span v-if="!canRedeemByLevel(reward.requiredLevel)" class="level-tip">需要{{ levelLabel(reward.requiredLevel) }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="section-card coupons-card" v-loading="loading">
      <div class="section-heading">
        <div><p class="eyebrow">MY COUPONS</p><h2>我的优惠券</h2><span>这里展示当前账号已领取、可在结算时使用的优惠券。</span></div>
      </div>
      <el-empty v-if="!usableCoupons.length" description="暂无可用优惠券，去兑换柜台或活动页领取吧" />
      <div v-else class="coupon-grid">
        <article v-for="coupon in usableCoupons" :key="coupon.id" class="coupon-card">
          <div class="coupon-icon">🎫</div>
          <div>
            <div class="coupon-title"><h3>{{ coupon.rewardName }}</h3><el-tag type="success">可使用</el-tag></div>
            <p>兑换号：{{ coupon.redemptionNo }}</p>
            <small>领取时间：{{ formatTime(coupon.redeemedAt) }} · 结算时可下拉选择使用</small>
          </div>
        </article>
      </div>
    </section>

    <section class="section-card transactions-card">
      <div class="section-heading"><div><p class="eyebrow">REDEMPTIONS</p><h2>最近兑换记录</h2></div></div>
      <el-table :data="redemptions" empty-text="暂无兑换记录">
        <el-table-column prop="redeemedAt" label="时间" min-width="160"><template #default="{ row }">{{ formatTime(row.redeemedAt) }}</template></el-table-column>
        <el-table-column prop="redemptionNo" label="兑换号" min-width="180" />
        <el-table-column prop="rewardName" label="奖励" min-width="180" />
        <el-table-column prop="pointsCost" label="消耗积分" width="120" />
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag type="success">{{ redemptionStatusText(row.status) }}</el-tag></template></el-table-column>
      </el-table>
    </section>

    <section class="section-card transactions-card">
      <div class="section-heading"><div><p class="eyebrow">POINT HISTORY</p><h2>积分流水</h2></div></div>
      <el-table :data="points?.transactions || []" empty-text="暂无积分流水">
        <el-table-column prop="createdAt" label="时间" min-width="160"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column prop="points" label="积分" width="100"><template #default="{ row }"><strong :class="row.points >= 0 ? 'points-plus' : 'points-minus'">{{ formatPoints(row.points) }}</strong></template></el-table-column>
        <el-table-column prop="balanceAfter" label="余额" width="100" />
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchCustomerPoints,
  fetchCustomerPreferences,
  fetchMyRedemptions,
  fetchRewardCatalog,
  redeemReward,
  saveCustomerPreferences,
  type PointsSummaryResponse,
  type RewardCatalogResponse,
  type RewardRedemptionResponse,
} from '@/api/customer'

const preferenceGroups = [
  { type: 'TASTE', label: '口味偏好', items: ['少糖', '少冰', '咖啡', '甜品', '茶饮'] },
  { type: 'CAT', label: '猫咪互动', items: ['安静猫咪', '活泼猫咪', '短毛猫', '长毛猫'] },
  { type: 'SEAT', label: '座位偏好', items: ['靠窗', '安静角落', '适合拍照'] },
  { type: 'ALLERGY', label: '注意事项', items: ['猫毛敏感', '乳制品', '坚果'] },
]
const loading = ref(false)
const saving = ref(false)
const redeemingId = ref<number>()
const points = ref<PointsSummaryResponse>()
const rewards = ref<RewardCatalogResponse[]>([])
const redemptions = ref<RewardRedemptionResponse[]>([])
const selected = reactive<Record<string, string[]>>({ TASTE: [], CAT: [], SEAT: [], ALLERGY: [] })
function formatTime(value: string) { return value ? value.replace('T', ' ').slice(0, 16) : '-' }
function formatPoints(value: number) { return value >= 0 ? `+${value}` : String(value) }
function rewardIcon(type: string) { return ({ COUPON: '🎫', SERVICE: '🐾', ITEM: '☕' } as Record<string, string>)[type] || '🎁' }
function rewardTypeText(type: string) { return ({ COUPON: '券包', SERVICE: '服务', ITEM: '实物' } as Record<string, string>)[type] || type }
function stockText(stock?: number | null) { return stock == null ? '不限库存' : stock > 0 ? `剩余 ${stock} 份` : '已兑完' }
function canRedeem(reward: RewardCatalogResponse) { return canRedeemByLevel(reward.requiredLevel) && (points.value?.points ?? 0) >= reward.pointsCost && (reward.stock == null || reward.stock > 0) }
function normalizeLevel(level?: string | null) { return (level || 'NORMAL').toUpperCase() }
function levelRank(level?: string | null) {
  const normalized = normalizeLevel(level)
  if (normalized === 'SVIP') return 3
  if (normalized === 'VIP') return 2
  return 1
}
const usableCoupons = computed(() => redemptions.value.filter((coupon) => coupon.status === 'REDEEMED' && !coupon.usedAt && !coupon.orderId))
function canRedeemByLevel(requiredLevel?: string | null) { return levelRank(points.value?.levelCode) >= levelRank(requiredLevel) }
function levelLabel(level?: string | null) {
  const normalized = normalizeLevel(level)
  if (normalized === 'SVIP') return 'SVIP 会员'
  if (normalized === 'VIP') return 'VIP 会员'
  return '普通会员'
}
function redeemButtonText(reward: RewardCatalogResponse) {
  if (!canRedeemByLevel(reward.requiredLevel)) return `需要 ${levelLabel(reward.requiredLevel)}`
  if (reward.stock != null && reward.stock <= 0) return '已兑完'
  if ((points.value?.points ?? 0) < reward.pointsCost) return '积分不足'
  return '立即兑换'
}
function redemptionStatusText(status: string) {
  const map: Record<string, string> = { REDEEMED: '已兑换', LOCKED: '已绑定订单', USED: '已使用', EXPIRED: '已过期' }
  return map[status] || status
}
async function loadData() {
  loading.value = true
  try {
    const [pointsData, preferences, rewardData, redemptionData] = await Promise.all([
      fetchCustomerPoints(),
      fetchCustomerPreferences(),
      fetchRewardCatalog(),
      fetchMyRedemptions(),
    ])
    points.value = pointsData
    rewards.value = rewardData
    redemptions.value = redemptionData
    Object.keys(selected).forEach((key) => { selected[key] = [] })
    preferences.forEach((item) => {
      if (!selected[item.preferenceType]) selected[item.preferenceType] = []
      selected[item.preferenceType].push(item.preferenceValue)
    })
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '会员信息加载失败') } finally { loading.value = false }
}
async function confirmRedeem(reward: RewardCatalogResponse) {
  await ElMessageBox.confirm(`确认使用 ${reward.pointsCost} 积分兑换「${reward.name}」吗？`, '兑换奖励', { type: 'warning' })
  redeemingId.value = reward.id
  try {
    const result = await redeemReward(reward.id)
    ElMessage.success(`兑换成功，剩余 ${result.balanceAfter} 积分`)
    await loadData()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '兑换失败') } finally { redeemingId.value = undefined }
}
async function savePreferences() {
  saving.value = true
  try {
    await saveCustomerPreferences(Object.entries(selected).flatMap(([preferenceType, values]) => values.map((preferenceValue) => ({ preferenceType, preferenceValue }))))
    ElMessage.success('偏好已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '偏好保存失败') } finally { saving.value = false }
}
onMounted(loadData)
</script>

<style scoped>
.profile-grid { display: grid; grid-template-columns: 280px 1fr; gap: 22px; }
.member-card { display: flex; flex-direction: column; justify-content: center; min-height: 220px; background: linear-gradient(135deg, #3b2618, #92400e); color: #fff8f0; }
.member-card .eyebrow { color: #fed7aa; }
.member-card strong { font-size: 64px; line-height: 1; }
.member-card span { margin-top: 12px; opacity: 0.78; }
.preference-group { margin-top: 18px; }
.preference-group h3 { margin: 0 0 12px; color: #3b2618; }
.rewards-card, .transactions-card, .coupons-card { margin-top: 22px; }
.reward-grid, .coupon-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
.reward-card, .coupon-card { display: grid; grid-template-columns: 72px 1fr; overflow: hidden; border: 1px solid #f5dfc5; border-radius: 22px; background: linear-gradient(135deg, #fffaf4, #fff); box-shadow: 0 16px 34px rgba(146, 64, 14, 0.08); }
.coupon-card { padding: 16px; align-items: center; border-color: #bbf7d0; background: linear-gradient(135deg, #f0fdf4, #fff); }
.ticket-stub, .coupon-icon { display: grid; place-items: center; background: repeating-linear-gradient(180deg, #fde8c8, #fde8c8 12px, #fff2dc 12px, #fff2dc 24px); font-size: 34px; }
.coupon-icon { width: 56px; height: 56px; border-radius: 16px; background: #dcfce7; }
.reward-body { padding: 18px; display: grid; gap: 12px; }
.reward-title, .coupon-title { display: flex; justify-content: space-between; gap: 8px; align-items: flex-start; }
.reward-title h3, .coupon-title h3 { margin: 0; color: #3b2618; }
.coupon-card p, .coupon-card small { margin: 4px 0 0; color: rgba(59,38,24,0.62); }
.reward-body p { margin: 0; color: rgba(59,38,24,0.62); line-height: 1.7; }
.reward-meta { display: flex; justify-content: space-between; gap: 10px; color: #8a6a52; font-weight: 800; }
.reward-meta strong { color: #d97706; }
.points-plus { color: #16a34a; }
.points-minus { color: #dc2626; }
.level-tag { margin-top: 6px; }
.level-tip { color: #d46b08; font-size: 13px; display: block; margin-top: 8px; }
@media (max-width: 860px) { .profile-grid { grid-template-columns: 1fr; } }
</style>
