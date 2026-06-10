<template>
  <section class="page-card profile-page">
    <header class="page-header">
      <div><p class="eyebrow">PAW MEMBER</p><h1>会员与偏好</h1><p>查看猫爪积分、消费累计和个性化偏好，后续推荐会优先参考这些标签。</p></div>
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

    <section class="section-card transactions-card">
      <div class="section-heading"><div><p class="eyebrow">POINT HISTORY</p><h2>积分流水</h2></div><el-button round @click="loadData">刷新</el-button></div>
      <el-table :data="points?.transactions || []" empty-text="暂无积分流水">
        <el-table-column prop="createdAt" label="时间" min-width="160"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column prop="points" label="积分" width="100"><template #default="{ row }"><strong class="points-plus">+{{ row.points }}</strong></template></el-table-column>
        <el-table-column prop="balanceAfter" label="余额" width="100" />
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCustomerPoints, fetchCustomerPreferences, saveCustomerPreferences, type PointsSummaryResponse } from '@/api/customer'

const preferenceGroups = [
  { type: 'TASTE', label: '口味偏好', items: ['少糖', '少冰', '咖啡', '甜品', '茶饮'] },
  { type: 'CAT', label: '猫咪互动', items: ['安静猫咪', '活泼猫咪', '短毛猫', '长毛猫'] },
  { type: 'SEAT', label: '座位偏好', items: ['靠窗', '安静角落', '适合拍照'] },
  { type: 'ALLERGY', label: '注意事项', items: ['猫毛敏感', '乳制品', '坚果'] },
]
const loading = ref(false)
const saving = ref(false)
const points = ref<PointsSummaryResponse>()
const selected = reactive<Record<string, string[]>>({ TASTE: [], CAT: [], SEAT: [], ALLERGY: [] })
function formatTime(value: string) { return value ? value.replace('T', ' ').slice(0, 16) : '-' }
async function loadData() {
  loading.value = true
  try {
    points.value = await fetchCustomerPoints()
    const preferences = await fetchCustomerPreferences()
    Object.keys(selected).forEach((key) => { selected[key] = [] })
    preferences.forEach((item) => {
      if (!selected[item.preferenceType]) selected[item.preferenceType] = []
      selected[item.preferenceType].push(item.preferenceValue)
    })
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '会员信息加载失败') } finally { loading.value = false }
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
.transactions-card { margin-top: 22px; }
.points-plus { color: #16a34a; }
@media (max-width: 860px) { .profile-grid { grid-template-columns: 1fr; } }
</style>
