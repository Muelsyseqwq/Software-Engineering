<template>
  <section v-loading="loading" class="page-card role-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">STORE_MANAGER 店长</p>
        <h1>门店经营管理</h1>
        <p>处理本店营收、订单、猫咪、员工排班、运营活动与菜品价格。</p>
      </div>
      <el-button type="primary" @click="refreshCurrentTab">刷新</el-button>
    </header>

    <el-tabs v-model="activeTab" class="manager-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="门店概览" name="overview">
        <div class="overview-grid">
          <el-card class="section-card">
            <template #header>
              <div class="card-header">
                <span>门店信息</span>
                <div class="header-actions" v-if="store">
                  <el-button size="small" @click="openStoreDialog">编辑门店信息</el-button>
                  <el-button
                    size="small"
                    :type="store.status === 'OPEN' ? 'warning' : 'success'"
                    :loading="savingStatus"
                    @click="toggleStoreStatus"
                  >
                    {{ store.status === 'OPEN' ? '设为歇业' : '设为营业' }}
                  </el-button>
                </div>
              </div>
            </template>
            <div v-if="store" class="store-grid">
              <span>门店：{{ store.name }}</span>
              <span>城市：{{ store.city }}</span>
              <span>地址：{{ store.address }}</span>
              <span>电话：{{ store.phone || '未填写' }}</span>
              <span>营业时间：{{ formatTime(store.openingTime) }} - {{ formatTime(store.closingTime) }}</span>
              <span>状态：<el-tag :type="storeStatusTag(store.status)">{{ storeStatusLabel(store.status) }}</el-tag></span>
              <span class="store-description">介绍：{{ store.description || '暂无介绍' }}</span>
            </div>
            <el-empty v-else description="暂无门店信息" />
          </el-card>

          <el-card class="section-card">
            <template #header>
              <div class="card-header">
                <span>经营指标</span>
                <el-date-picker
                  v-model="metricsRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  size="small"
                  style="width: 260px"
                  @change="loadMetrics"
                />
              </div>
            </template>
            <div v-if="metrics" class="metrics-grid">
              <div class="metric-card"><span>营收</span><strong>¥{{ money(metrics.revenue) }}</strong></div>
              <div class="metric-card"><span>支付订单</span><strong>{{ metrics.paidOrderCount }}</strong></div>
              <div class="metric-card"><span>完成订单</span><strong>{{ metrics.completedOrderCount }}</strong></div>
              <div class="metric-card"><span>预约数</span><strong>{{ metrics.reservationCount }}</strong></div>
              <div class="metric-card"><span>翻台率</span><strong>{{ percent(metrics.tableTurnoverRate) }}</strong></div>
              <div class="metric-card"><span>坪效</span><strong>¥{{ money(metrics.revenuePerSquareMeter) }}/㎡</strong></div>
              <div class="metric-card"><span>客单价</span><strong>¥{{ money(metrics.averageOrderValue) }}</strong></div>
              <div class="metric-card"><span>门店面积</span><strong>{{ metrics.areaSquareMeter || 0 }}㎡</strong></div>
            </div>
            <el-empty v-else description="暂无经营指标" />
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="桌位与预约" name="tables">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>桌位管理</span>
              <el-button size="small" type="primary" @click="openCreateDialog">新增桌位</el-button>
            </div>
          </template>
          <el-table :data="tables" border empty-text="暂无桌位数据">
            <el-table-column prop="tableNo" label="桌号" min-width="120" />
            <el-table-column prop="capacity" label="容量" width="100" />
            <el-table-column prop="area" label="区域" min-width="140" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="tableStatusTag(row.status)">{{ tableStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>本店预约</span>
              <div class="reservation-filters">
                <el-date-picker v-model="reservationQuery.date" type="date" value-format="YYYY-MM-DD" placeholder="预约日期" clearable size="small" style="width: 150px" @change="loadReservations" />
                <el-select v-model="reservationQuery.status" placeholder="预约状态" clearable size="small" style="width: 140px" @change="loadReservations">
                  <el-option label="待到店" value="RESERVED" />
                  <el-option label="已到店" value="CHECKED_IN" />
                  <el-option label="已完成" value="COMPLETED" />
                  <el-option label="已取消" value="CANCELLED" />
                </el-select>
              </div>
            </div>
          </template>
          <el-table :data="reservations" border empty-text="暂无预约数据">
            <el-table-column prop="reservationNo" label="预约号" min-width="180" />
            <el-table-column prop="customerName" label="顾客" width="120" />
            <el-table-column prop="contactPhone" label="手机号" min-width="130" />
            <el-table-column prop="partySize" label="人数" width="80" />
            <el-table-column prop="tableNo" label="桌号" width="100" />
            <el-table-column prop="slotTime" label="时段" min-width="150" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><el-tag :type="reservationStatusTag(row.status)">{{ reservationStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="创建时间" min-width="170" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'RESERVED'" size="small" type="success" @click="handleReservationStatus(row, 'CHECKED_IN')">到店</el-button>
                <el-button v-if="row.status === 'CHECKED_IN'" size="small" type="primary" @click="handleReservationStatus(row, 'COMPLETED')">完成</el-button>
                <el-button v-if="row.status === 'RESERVED'" size="small" type="danger" @click="handleReservationStatus(row, 'CANCELLED')">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="订单与营收" name="orders">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>本店订单</span>
              <div class="reservation-filters">
                <el-date-picker v-model="orderRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" size="small" style="width: 240px" @change="loadOrders" />
                <el-select v-model="orderQuery.status" placeholder="订单状态" clearable size="small" style="width: 140px" @change="loadOrders">
                  <el-option label="已创建" value="CREATED" />
                  <el-option label="已支付" value="PAID" />
                  <el-option label="制作中" value="PREPARING" />
                  <el-option label="已完成" value="COMPLETED" />
                  <el-option label="已取消" value="CANCELLED" />
                </el-select>
              </div>
            </div>
          </template>
          <el-table :data="orders" border empty-text="暂无订单数据">
            <el-table-column prop="orderNo" label="订单号" min-width="180" />
            <el-table-column prop="customerName" label="顾客" width="120" />
            <el-table-column prop="tableNo" label="桌号" width="90" />
            <el-table-column prop="itemSummary" label="菜品" min-width="180" show-overflow-tooltip />
            <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
            <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="orderStatusTag(row.status)">{{ orderStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="170" />
            <el-table-column label="操作" width="100"><template #default="{ row }"><el-button size="small" @click="openOrderDetail(row.id)">详情</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="猫咪状态" name="cats">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>本店猫咪</span>
              <el-select v-model="catQuery.status" placeholder="猫咪状态" clearable size="small" style="width: 140px" @change="loadCats">
                <el-option label="可互动" value="AVAILABLE" />
                <el-option label="休息中" value="RESTING" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </div>
          </template>
          <el-table :data="cats" border empty-text="暂无猫咪数据">
            <el-table-column prop="name" label="猫咪" width="120" />
            <el-table-column prop="breed" label="品种" width="120" />
            <el-table-column prop="age" label="年龄" width="80" />
            <el-table-column prop="gender" label="性别" width="80" />
            <el-table-column prop="healthStatus" label="健康状态" min-width="120" />
            <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="catStatusTag(row.status)">{{ catStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="员工与排班" name="staff">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>本店员工</span>
              <el-button size="small" type="primary" @click="openShiftDialog()">新增排班</el-button>
            </div>
          </template>
          <el-table :data="staffRows" border empty-text="暂无员工数据">
            <el-table-column prop="nickname" label="姓名" width="120" />
            <el-table-column prop="username" label="账号" width="130" />
            <el-table-column prop="roleCode" label="岗位" width="130" />
            <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="staffStatusTag(row.status)">{{ staffStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="今日排班" min-width="180"><template #default="{ row }">{{ formatShift(row.todayShiftStartTime, row.todayShiftEndTime) }} {{ row.todayShiftStatus ? `(${shiftStatusLabel(row.todayShiftStatus)})` : '' }}</template></el-table-column>
            <el-table-column prop="activeLeaveStatus" label="请假" width="100" />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openLeaveDialog(row)">放假</el-button>
                <el-button size="small" @click="openShiftDialog(row)">排班</el-button>
                <el-button size="small" type="danger" @click="handleDismissStaff(row)">开除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-card class="section-card">
          <template #header><div class="card-header"><span>排班表</span><el-date-picker v-model="shiftRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" size="small" style="width: 240px" @change="loadShifts" /></div></template>
          <el-table :data="shifts" border empty-text="暂无排班数据">
            <el-table-column prop="shiftDate" label="日期" width="120" />
            <el-table-column prop="nickname" label="员工" width="120" />
            <el-table-column prop="roleCode" label="岗位" width="130" />
            <el-table-column label="时间" width="150"><template #default="{ row }">{{ formatShift(row.startTime, row.endTime) }}</template></el-table-column>
            <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="shiftStatusTag(row.status)">{{ shiftStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="100"><template #default="{ row }"><el-button size="small" @click="openShiftDialog(undefined, row)">编辑</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="活动确认" name="activities">
        <el-card class="section-card">
          <template #header><div class="card-header"><span>运营活动</span><el-select v-model="activityQuery.status" clearable size="small" placeholder="处理状态" style="width: 140px" @change="loadActivities"><el-option label="待确认" value="PENDING" /><el-option label="已接受" value="ACCEPTED" /><el-option label="已拒绝" value="REJECTED" /></el-select></div></template>
          <el-table :data="activities" border empty-text="暂无活动数据">
            <el-table-column prop="title" label="活动" min-width="160" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column prop="startAt" label="开始" min-width="160" />
            <el-table-column prop="endAt" label="结束" min-width="160" />
            <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="activityStatusTag(row.acceptStatus)">{{ activityStatusLabel(row.acceptStatus) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="160"><template #default="{ row }"><el-button size="small" type="success" @click="handleActivityDecision(row, 'ACCEPTED')">接受</el-button><el-button size="small" type="danger" @click="handleActivityDecision(row, 'REJECTED')">拒绝</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="菜品价格" name="dishes">
        <el-card class="section-card">
          <template #header><div class="card-header"><span>本店菜品</span><el-select v-model="dishQuery.status" clearable size="small" placeholder="菜品状态" style="width: 140px" @change="loadDishes"><el-option label="上架" value="ON_SHELF" /><el-option label="下架" value="OFF_SHELF" /></el-select></div></template>
          <el-table :data="dishes" border empty-text="暂无菜品数据">
            <el-table-column prop="name" label="菜品" min-width="140" />
            <el-table-column label="价格" width="120"><template #default="{ row }">¥{{ money(row.price) }}</template></el-table-column>
            <el-table-column prop="stock" label="库存" width="90" />
            <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="dishStatusTag(row.status)">{{ dishStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="170"><template #default="{ row }"><el-button size="small" @click="openPriceDialog(row)">改价</el-button><el-button size="small" @click="openPriceHistory(row)">历史</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="storeDialogVisible" title="编辑门店信息" width="560px" :lock-scroll="false">
      <el-form :model="storeForm" label-position="top">
        <el-form-item label="门店名称"><el-input v-model="storeForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="storeForm.city" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="storeForm.address" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="storeForm.phone" /></el-form-item>
        <div class="form-row">
          <el-form-item label="开始营业"><el-time-select v-model="storeForm.openingTime" start="06:00" step="00:30" end="23:30" /></el-form-item>
          <el-form-item label="结束营业"><el-time-select v-model="storeForm.closingTime" start="06:00" step="00:30" end="23:30" /></el-form-item>
        </div>
        <el-form-item label="门店介绍"><el-input v-model="storeForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="storeDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingStore" @click="handleStoreSubmit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑桌位' : '新增桌位'" width="480px" :lock-scroll="false">
      <el-form :model="form" label-position="top">
        <el-form-item label="桌号"><el-input v-model="form.tableNo" placeholder="A01" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" :max="20" controls-position="right" /></el-form-item>
        <el-form-item label="区域"><el-input v-model="form.area" placeholder="猫咪互动区" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="可用" value="AVAILABLE" /><el-option label="使用中" value="OCCUPIED" /><el-option label="已预约" value="RESERVED" /><el-option label="清洁中" value="CLEANING" /><el-option label="停用" value="DISABLED" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="savingTable" @click="handleSubmit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="leaveDialogVisible" title="给员工放假" width="460px" :lock-scroll="false">
      <el-form :model="leaveForm" label-position="top">
        <el-form-item label="请假类型"><el-select v-model="leaveForm.leaveType"><el-option label="事假" value="PERSONAL" /><el-option label="病假" value="SICK" /><el-option label="年假" value="ANNUAL" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="leaveDates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width: 100%" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="leaveForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="leaveDialogVisible = false">取消</el-button><el-button type="primary" @click="handleGrantLeave">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="shiftDialogVisible" :title="editingShiftId ? '编辑排班' : '新增排班'" width="520px" :lock-scroll="false">
      <el-form :model="shiftForm" label-position="top">
        <el-form-item label="员工"><el-select v-model="shiftForm.userId" filterable><el-option v-for="staff in activeStaffOptions" :key="staff.userId" :label="staff.nickname || staff.username || String(staff.userId)" :value="staff.userId" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="shiftForm.shiftDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <div class="form-row"><el-form-item label="开始"><el-time-select v-model="shiftForm.startTime" start="06:00" step="00:30" end="23:30" /></el-form-item><el-form-item label="结束"><el-time-select v-model="shiftForm.endTime" start="06:00" step="00:30" end="23:30" /></el-form-item></div>
        <el-form-item label="状态"><el-select v-model="shiftForm.status"><el-option label="已排班" value="SCHEDULED" /><el-option label="请假" value="ON_LEAVE" /><el-option label="已取消" value="CANCELLED" /><el-option label="已完成" value="COMPLETED" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="shiftForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="shiftDialogVisible = false">取消</el-button><el-button type="primary" @click="handleShiftSubmit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="priceDialogVisible" title="修改菜品价格" width="460px" :lock-scroll="false">
      <el-form :model="priceForm" label-position="top">
        <el-form-item label="新价格"><el-input-number v-model="priceForm.newPrice" :min="0.01" :precision="2" :step="1" controls-position="right" /></el-form-item>
        <el-form-item label="改价原因"><el-input v-model="priceForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="priceDialogVisible = false">取消</el-button><el-button type="primary" @click="handlePriceSubmit">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="orderDrawerVisible" title="订单详情" size="520px">
      <div v-if="orderDetail" class="drawer-content">
        <p><strong>订单号：</strong>{{ orderDetail.orderNo }}</p>
        <p><strong>顾客：</strong>{{ orderDetail.customerName || '散客' }}</p>
        <p><strong>金额：</strong>¥{{ money(orderDetail.totalAmount) }}</p>
        <p><strong>状态：</strong>{{ orderStatusLabel(orderDetail.status) }}</p>
        <el-table :data="orderDetail.items" border size="small"><el-table-column prop="dishName" label="菜品" /><el-table-column label="单价"><template #default="{ row }">¥{{ money(row.unitPrice) }}</template></el-table-column><el-table-column prop="quantity" label="数量" /><el-table-column label="小计"><template #default="{ row }">¥{{ money(row.subtotal) }}</template></el-table-column></el-table>
      </div>
    </el-drawer>

    <el-drawer v-model="priceHistoryVisible" title="价格历史" size="480px">
      <el-table :data="priceHistory" border size="small"><el-table-column label="原价"><template #default="{ row }">¥{{ money(row.oldPrice) }}</template></el-table-column><el-table-column label="新价"><template #default="{ row }">¥{{ money(row.newPrice) }}</template></el-table-column><el-table-column prop="reason" label="原因" /><el-table-column prop="createdAt" label="时间" min-width="150" /></el-table>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createManagerShift,
  createManagerTable,
  decideManagerActivity,
  dismissManagerStaff,
  fetchManagerActivities,
  fetchManagerCats,
  fetchManagerDishPriceHistory,
  fetchManagerDishes,
  fetchManagerMetrics,
  fetchManagerOrderDetail,
  fetchManagerOrders,
  fetchManagerReservations,
  fetchManagerShifts,
  fetchManagerStaff,
  fetchManagerStore,
  fetchManagerTables,
  grantManagerStaffLeave,
  updateManagerDishPrice,
  updateManagerReservationStatus,
  updateManagerShift,
  updateManagerStore,
  updateManagerStoreStatus,
  updateManagerTable,
  type DishPriceHistoryRow,
  type GrantLeavePayload,
  type ManagerActivityRow,
  type ManagerCatStatusRow,
  type ManagerDishRow,
  type ManagerMetricsSummary,
  type ManagerOrderDetail,
  type ManagerOrderQuery,
  type ManagerOrderRow,
  type ManagerReservationQuery,
  type ManagerReservationRow,
  type ManagerShiftPayload,
  type ManagerShiftRow,
  type ManagerStaffRow,
  type ManagerStoreInfo,
  type ManagerTableRow,
  type UpdateManagerStorePayload
} from '@/api/manager'

type TagType = 'success' | 'info' | 'warning' | 'danger' | 'primary'

const activeTab = ref('overview')
const loadedTabs = new Set<string>()
const store = ref<ManagerStoreInfo>()
const metrics = ref<ManagerMetricsSummary>()
const tables = ref<ManagerTableRow[]>([])
const reservations = ref<ManagerReservationRow[]>([])
const orders = ref<ManagerOrderRow[]>([])
const orderDetail = ref<ManagerOrderDetail>()
const cats = ref<ManagerCatStatusRow[]>([])
const staffRows = ref<ManagerStaffRow[]>([])
const shifts = ref<ManagerShiftRow[]>([])
const activities = ref<ManagerActivityRow[]>([])
const dishes = ref<ManagerDishRow[]>([])
const priceHistory = ref<DishPriceHistoryRow[]>([])
const loading = ref(false)
const savingStore = ref(false)
const savingStatus = ref(false)
const savingTable = ref(false)
const dialogVisible = ref(false)
const storeDialogVisible = ref(false)
const leaveDialogVisible = ref(false)
const shiftDialogVisible = ref(false)
const priceDialogVisible = ref(false)
const orderDrawerVisible = ref(false)
const priceHistoryVisible = ref(false)
const editingId = ref<number>()
const editingShiftId = ref<number>()
const selectedStaff = ref<ManagerStaffRow>()
const selectedDish = ref<ManagerDishRow>()
const metricsRange = ref<string[]>([])
const orderRange = ref<string[]>([])
const shiftRange = ref<string[]>([])
const leaveDates = ref<string[]>([])

const form = reactive<ManagerTableRow>({ tableNo: '', capacity: 2, area: '', status: 'AVAILABLE' })
const storeForm = reactive<UpdateManagerStorePayload>({ name: '', city: '', address: '', phone: '', openingTime: '10:00', closingTime: '22:00', description: '' })
const reservationQuery = reactive<ManagerReservationQuery>({ status: '', date: '' })
const orderQuery = reactive<ManagerOrderQuery>({ status: '' })
const catQuery = reactive<{ status?: string }>({ status: '' })
const activityQuery = reactive<{ status?: string }>({ status: '' })
const dishQuery = reactive<{ status?: string }>({ status: '' })
const leaveForm = reactive<GrantLeavePayload>({ leaveType: 'PERSONAL', startDate: '', endDate: '', reason: '' })
const shiftForm = reactive<ManagerShiftPayload>({ userId: 0, roleCode: 'STAFF', shiftDate: today(), startTime: '09:00', endTime: '18:00', status: 'SCHEDULED', remark: '' })
const priceForm = reactive({ newPrice: 0, reason: '' })

const activeStaffOptions = computed(() => staffRows.value.filter((staff) => staff.status !== 'DISMISSED'))

async function loadData() {
  loading.value = true
  try {
    const [storeData, metricData] = await Promise.all([fetchManagerStore(), fetchManagerMetrics(cleanMetricsQuery())])
    store.value = storeData
    metrics.value = metricData
    loadedTabs.add('overview')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '店长数据加载失败')
  } finally {
    loading.value = false
  }
}

async function refreshCurrentTab() {
  loadedTabs.delete(activeTab.value)
  await loadTab(activeTab.value)
}

async function handleTabChange(name: string | number) {
  await loadTab(String(name))
}

async function loadTab(name: string) {
  if (loadedTabs.has(name)) return
  loading.value = true
  try {
    if (name === 'overview') await loadData()
    if (name === 'tables') await Promise.all([loadTables(), loadReservations()])
    if (name === 'orders') await loadOrders()
    if (name === 'cats') await loadCats()
    if (name === 'staff') await Promise.all([loadStaff(), loadShifts()])
    if (name === 'activities') await loadActivities()
    if (name === 'dishes') await loadDishes()
    loadedTabs.add(name)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMetrics() { metrics.value = await fetchManagerMetrics(cleanMetricsQuery()) }
async function loadTables() { tables.value = await fetchManagerTables() }
async function loadReservations() { reservations.value = await fetchManagerReservations(cleanReservationQuery()) }
async function loadOrders() { orders.value = await fetchManagerOrders(cleanOrderQuery()) }
async function loadCats() { cats.value = await fetchManagerCats({ status: catQuery.status || undefined }) }
async function loadStaff() { staffRows.value = await fetchManagerStaff() }
async function loadShifts() { shifts.value = await fetchManagerShifts(cleanShiftQuery()) }
async function loadActivities() { activities.value = await fetchManagerActivities({ status: activityQuery.status || undefined }) }
async function loadDishes() { dishes.value = await fetchManagerDishes({ status: dishQuery.status || undefined }) }

function cleanMetricsQuery() { return { from: metricsRange.value?.[0], to: metricsRange.value?.[1] } }
function cleanReservationQuery() { return { status: reservationQuery.status || undefined, date: reservationQuery.date || undefined } }
function cleanOrderQuery() { return { status: orderQuery.status || undefined, from: orderRange.value?.[0], to: orderRange.value?.[1] } }
function cleanShiftQuery() { return { from: shiftRange.value?.[0], to: shiftRange.value?.[1] } }

function openStoreDialog() {
  if (!store.value) return
  Object.assign(storeForm, { name: store.value.name, city: store.value.city, address: store.value.address, phone: store.value.phone || '', openingTime: formatTime(store.value.openingTime), closingTime: formatTime(store.value.closingTime), description: store.value.description || '' })
  storeDialogVisible.value = true
}
function resetForm() { editingId.value = undefined; Object.assign(form, { tableNo: '', capacity: 2, area: '', status: 'AVAILABLE' }) }
function openCreateDialog() { resetForm(); dialogVisible.value = true }
function openEditDialog(row: ManagerTableRow) { editingId.value = row.id; Object.assign(form, row, { status: row.status === 'UNAVAILABLE' ? 'DISABLED' : row.status }); dialogVisible.value = true }
function openLeaveDialog(row: ManagerStaffRow) { selectedStaff.value = row; Object.assign(leaveForm, { leaveType: 'PERSONAL', startDate: '', endDate: '', reason: '' }); leaveDates.value = []; leaveDialogVisible.value = true }
function openShiftDialog(staff?: ManagerStaffRow, row?: ManagerShiftRow) {
  editingShiftId.value = row?.id
  Object.assign(shiftForm, { userId: row?.userId || staff?.userId || activeStaffOptions.value[0]?.userId || 0, roleCode: row?.roleCode || staff?.roleCode || 'STAFF', shiftDate: row?.shiftDate || today(), startTime: formatTime(row?.startTime) === '--:--' ? '09:00' : formatTime(row?.startTime), endTime: formatTime(row?.endTime) === '--:--' ? '18:00' : formatTime(row?.endTime), status: row?.status || 'SCHEDULED', remark: row?.remark || '' })
  shiftDialogVisible.value = true
}
function openPriceDialog(row: ManagerDishRow) { selectedDish.value = row; Object.assign(priceForm, { newPrice: Number(row.price), reason: '' }); priceDialogVisible.value = true }

async function handleStoreSubmit() {
  if (!storeForm.name.trim() || !storeForm.city.trim() || !storeForm.address.trim()) return ElMessage.warning('请填写门店名称、城市和地址')
  if (!storeForm.openingTime || !storeForm.closingTime || storeForm.openingTime >= storeForm.closingTime) return ElMessage.warning('请填写正确的营业时间')
  savingStore.value = true
  try { store.value = await updateManagerStore({ ...storeForm }); ElMessage.success('门店信息已保存'); storeDialogVisible.value = false } catch (error) { ElMessage.error(error instanceof Error ? error.message : '门店信息保存失败') } finally { savingStore.value = false }
}
async function toggleStoreStatus() {
  if (!store.value) return
  const targetStatus = store.value.status === 'OPEN' ? 'CLOSED' : 'OPEN'
  savingStatus.value = true
  try { await updateManagerStoreStatus(targetStatus); store.value = await fetchManagerStore(); ElMessage.success(targetStatus === 'OPEN' ? '门店已设为营业' : '门店已设为歇业') } catch (error) { ElMessage.error(error instanceof Error ? error.message : '营业状态更新失败') } finally { savingStatus.value = false }
}
async function handleSubmit() {
  if (!form.tableNo.trim()) return ElMessage.warning('请填写桌号')
  if (!form.capacity || form.capacity <= 0) return ElMessage.warning('容量必须大于 0')
  savingTable.value = true
  try { if (editingId.value) await updateManagerTable(editingId.value, form); else await createManagerTable(form); ElMessage.success('桌位保存成功'); dialogVisible.value = false; await loadTables() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '桌位保存失败') } finally { savingTable.value = false }
}
async function handleReservationStatus(row: ManagerReservationRow, status: string) {
  if (status === 'CANCELLED') { try { await ElMessageBox.confirm('取消预约后会释放对应时段库存，确认取消吗？', '取消预约', { type: 'warning' }) } catch { return } }
  try { await updateManagerReservationStatus(row.id, status); ElMessage.success(`预约已更新为${reservationStatusLabel(status)}`); await loadReservations() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '预约状态更新失败') }
}
async function openOrderDetail(id: number) { orderDetail.value = await fetchManagerOrderDetail(id); orderDrawerVisible.value = true }
async function handleDismissStaff(row: ManagerStaffRow) {
  try { await ElMessageBox.confirm(`确认开除 ${row.nickname || row.username || '该员工'} 吗？`, '开除员工', { type: 'warning' }) } catch { return }
  try { await dismissManagerStaff(row.userStoreRoleId, '店长操作'); ElMessage.success('员工已标记为离职'); await loadStaff() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '开除失败') }
}
async function handleGrantLeave() {
  if (!selectedStaff.value) return
  if (!leaveDates.value?.[0] || !leaveDates.value?.[1]) return ElMessage.warning('请选择请假日期')
  try { await grantManagerStaffLeave(selectedStaff.value.userId, { ...leaveForm, startDate: leaveDates.value[0], endDate: leaveDates.value[1] }); ElMessage.success('请假已记录'); leaveDialogVisible.value = false; await Promise.all([loadStaff(), loadShifts()]) } catch (error) { ElMessage.error(error instanceof Error ? error.message : '放假失败') }
}
async function handleShiftSubmit() {
  if (!shiftForm.userId) return ElMessage.warning('请选择员工')
  try { if (editingShiftId.value) await updateManagerShift(editingShiftId.value, shiftForm); else await createManagerShift(shiftForm); ElMessage.success('排班已保存'); shiftDialogVisible.value = false; await Promise.all([loadStaff(), loadShifts()]) } catch (error) { ElMessage.error(error instanceof Error ? error.message : '排班保存失败') }
}
async function handleActivityDecision(row: ManagerActivityRow, acceptStatus: string) {
  try { await decideManagerActivity(row.activityStoreId, { acceptStatus }); ElMessage.success(`活动已${acceptStatus === 'ACCEPTED' ? '接受' : '拒绝'}`); await loadActivities() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '活动处理失败') }
}
async function handlePriceSubmit() {
  if (!selectedDish.value || !priceForm.newPrice || priceForm.newPrice <= 0) return ElMessage.warning('请输入正确的新价格')
  try { await updateManagerDishPrice(selectedDish.value.id, { newPrice: priceForm.newPrice, reason: priceForm.reason }); ElMessage.success('价格已更新'); priceDialogVisible.value = false; await loadDishes() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '价格更新失败') }
}
async function openPriceHistory(row: ManagerDishRow) { priceHistory.value = await fetchManagerDishPriceHistory(row.id); priceHistoryVisible.value = true }

function today() { return new Date().toISOString().slice(0, 10) }
function formatTime(value?: string) { return value ? value.slice(0, 5) : '--:--' }
function formatShift(start?: string, end?: string) { return start && end ? `${formatTime(start)}-${formatTime(end)}` : '未排班' }
function money(value?: number) { return Number(value || 0).toFixed(2) }
function percent(value?: number) { return `${(Number(value || 0) * 100).toFixed(0)}%` }
function label(status: string, map: Record<string, string>) { return map[status] || status }
function tag(status: string, map: Record<string, TagType>) { return map[status] || 'info' }
function storeStatusLabel(status: string) { return label(status, { OPEN: '营业中', CLOSED: '已歇业', PREPARING: '筹备中' }) }
function storeStatusTag(status: string) { return tag(status, { OPEN: 'success', CLOSED: 'info', PREPARING: 'warning' }) }
function tableStatusLabel(status: string) { return label(status, { AVAILABLE: '可用', OCCUPIED: '使用中', RESERVED: '已预约', CLEANING: '清洁中', DISABLED: '停用', UNAVAILABLE: '不可用' }) }
function tableStatusTag(status: string) { return tag(status, { AVAILABLE: 'success', OCCUPIED: 'warning', RESERVED: 'primary', CLEANING: 'warning', DISABLED: 'info', UNAVAILABLE: 'info' }) }
function reservationStatusLabel(status: string) { return label(status, { PENDING_PAYMENT: '待支付', RESERVED: '待到店', CHECKED_IN: '已到店', COMPLETED: '已完成', CANCELLED: '已取消' }) }
function reservationStatusTag(status: string) { return tag(status, { PENDING_PAYMENT: 'warning', RESERVED: 'primary', CHECKED_IN: 'success', COMPLETED: 'info', CANCELLED: 'danger' }) }
function orderStatusLabel(status: string) { return label(status, { CREATED: '已创建', PAID: '已支付', PREPARING: '制作中', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款' }) }
function orderStatusTag(status: string) { return tag(status, { CREATED: 'info', PAID: 'success', PREPARING: 'warning', COMPLETED: 'primary', CANCELLED: 'danger', REFUNDING: 'warning', REFUNDED: 'info' }) }
function catStatusLabel(status: string) { return label(status, { AVAILABLE: '可互动', RESTING: '休息中', DISABLED: '停用' }) }
function catStatusTag(status: string) { return tag(status, { AVAILABLE: 'success', RESTING: 'warning', DISABLED: 'info' }) }
function staffStatusLabel(status: string) { return label(status, { ACTIVE: '在职', SUSPENDED: '暂停', DISMISSED: '已离职' }) }
function staffStatusTag(status: string) { return tag(status, { ACTIVE: 'success', SUSPENDED: 'warning', DISMISSED: 'danger' }) }
function shiftStatusLabel(status: string) { return label(status, { SCHEDULED: '已排班', ON_LEAVE: '请假', CANCELLED: '已取消', COMPLETED: '已完成' }) }
function shiftStatusTag(status: string) { return tag(status, { SCHEDULED: 'primary', ON_LEAVE: 'warning', CANCELLED: 'danger', COMPLETED: 'success' }) }
function activityStatusLabel(status: string) { return label(status, { PENDING: '待确认', ACCEPTED: '已接受', REJECTED: '已拒绝' }) }
function activityStatusTag(status: string) { return tag(status, { PENDING: 'warning', ACCEPTED: 'success', REJECTED: 'danger' }) }
function dishStatusLabel(status: string) { return label(status, { ON_SHELF: '上架', OFF_SHELF: '下架', SOLD_OUT: '售罄' }) }
function dishStatusTag(status: string) { return tag(status, { ON_SHELF: 'success', OFF_SHELF: 'info', SOLD_OUT: 'warning' }) }

onMounted(loadData)
</script>

<style scoped>
.role-page { display: grid; gap: 18px; }
.page-header, .card-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.manager-tabs { display: block; }
.overview-grid, .manager-tabs :deep(.el-tab-pane) { display: grid; gap: 18px; }
.header-actions, .reservation-filters { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
.eyebrow { margin: 0 0 8px; color: #d97706; font-weight: 800; letter-spacing: 0.08em; }
.store-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.store-description { grid-column: 1 / -1; }
.metrics-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.metric-card { border: 1px solid #f3d7aa; border-radius: 14px; padding: 14px; background: #fffaf2; display: grid; gap: 8px; }
.metric-card span { color: #8a5a1f; font-size: 13px; }
.metric-card strong { font-size: 22px; color: #92400e; }
.form-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.drawer-content { display: grid; gap: 10px; }

@media (max-width: 768px) {
  .page-header, .card-header { flex-direction: column; }
  .store-grid, .form-row, .metrics-grid { grid-template-columns: 1fr; }
  .reservation-filters { justify-content: flex-start; }
}
</style>
