<template>
  <div class="page-container">
    <h2 class="page-title">场地方门户</h2>

    <el-tabs v-model="activeTab" type="border-card" class="rounded-xl overflow-hidden">
      <el-tab-pane label="用电总表确认" name="meter">
        <div class="filter-bar">
          <el-select v-model="meterStationId" placeholder="选择站点" class="w-64" @change="loadMeters">
            <el-option v-for="s in myStations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <el-table :data="meterReadings" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ row.readingType === 'TOTAL' ? '用电总表' : '物业分表' }}</template>
          </el-table-column>
          <el-table-column label="读数(kWh)" width="140">
            <template #default="{ row }">{{ row.kwh.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="readingDate" label="抄表日期" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.confirmedAt ? 'success' : 'warning'" size="small">{{ row.confirmedAt ? '已确认' : '待确认' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="!row.confirmedAt" type="primary" text size="small" @click="confirmReading(row)">确认</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="物业分成汇总" name="share">
        <div class="grid grid-cols-3 gap-5 mb-5">
          <div class="stat-card">
            <div class="stat-label">本月总分成</div>
            <div class="stat-value text-amber-600">¥{{ shareSummary.totalShare.toFixed(2) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">已结算</div>
            <div class="stat-value text-green-600">¥{{ shareSummary.settled.toFixed(2) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">待结算</div>
            <div class="stat-value text-teal-700">¥{{ shareSummary.pending.toFixed(2) }}</div>
          </div>
        </div>
        <el-table :data="shareRecords" stripe>
          <el-table-column prop="period" label="结算周期" width="120" />
          <el-table-column label="场地方分成" width="140">
            <template #default="{ row }">¥{{ row.siteOwnerShare.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="物业分摊" width="140">
            <template #default="{ row }">¥{{ row.propertyShare.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SETTLED' ? 'success' : row.status === 'CONFIRMED' ? '' : 'warning'" size="small">
                {{ { PENDING: '待确认', CONFIRMED: '已确认', DISPUTED: '有争议', SETTLED: '已结算' }[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" type="primary" text size="small" @click="confirmSettle(row)">确认</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="停电通知" name="outage">
        <div class="filter-bar">
          <el-button type="primary" @click="outageDialogVisible = true">
            <Plus class="w-4 h-4 mr-1" />发布停电通知
          </el-button>
        </div>
        <el-table :data="outageNotices" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="开始时间" width="160">
            <template #default="{ row }">{{ row.startTime }}</template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template #default="{ row }">{{ row.endTime }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="{ SCHEDULED: 'info', ACTIVE: 'danger', COMPLETED: 'success', CANCELLED: 'warning' }[row.status]" size="small">
                {{ { SCHEDULED: '计划中', ACTIVE: '停电中', COMPLETED: '已完成', CANCELLED: '已取消' }[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="受影响订单" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.affectedOrderCount" type="warning" size="small">{{ row.affectedOrderCount }}单</el-tag>
              <el-button v-else type="primary" text size="small" @click="viewAffectedOrders(row)">查看</el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button v-if="row.status === 'SCHEDULED'" type="primary" text size="small" @click="activateOutageNotice(row)">激活停电</el-button>
              <el-button v-if="row.status === 'SCHEDULED'" type="danger" text size="small" @click="cancelOutage(row)">取消</el-button>
              <el-button v-if="row.status === 'ACTIVE'" type="success" text size="small" @click="settleIncompleteOrders(row.id); loadOutages()">结算</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="结算确认" name="settlement">
        <el-table :data="settlementRecords" stripe>
          <el-table-column prop="period" label="结算周期" width="120" />
          <el-table-column label="总电费" width="130">
            <template #default="{ row }">¥{{ row.totalElectricityFee.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="场地方分成" width="130">
            <template #default="{ row }">¥{{ row.siteOwnerShare.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="物业分摊" width="130">
            <template #default="{ row }">¥{{ row.propertyShare.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SETTLED' ? 'success' : 'warning'" size="small">
                {{ { PENDING: '待确认', CONFIRMED: '已确认', DISPUTED: '有争议', SETTLED: '已结算' }[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" type="primary" text size="small" @click="confirmSettle(row)">确认</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showAffectedDialog" title="停电受影响订单" width="600px">
      <el-table :data="affectedOrders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column label="已充电量" width="120">
          <template #default="{ row }">{{ row.chargedKwh?.toFixed(2) || '0.00' }} kWh</template>
        </el-table-column>
        <el-table-column label="当前费用" width="120">
          <template #default="{ row }">¥{{ row.totalFee?.toFixed(2) || '0.00' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CHARGING' ? 'danger' : 'warning'" size="small">{{ row.status === 'CHARGING' ? '充电中' : '已中断' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 p-3 bg-amber-50 border border-amber-100 rounded-lg">
        <div class="text-sm text-amber-700">停电后，这些正在充电的订单将按实际电量结算。激活停电后，受影响订单会自动中断并结算。</div>
      </div>
      <template #footer>
        <el-button @click="showAffectedDialog = false">关闭</el-button>
        <el-button type="primary" :loading="settling" @click="settleOrders">按实际电量结算</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="outageDialogVisible" title="发布停电通知" width="500px">
      <el-form :model="outageForm" label-width="90px">
        <el-form-item label="站点">
          <el-select v-model="outageForm.stationId" class="w-full">
            <el-option v-for="s in myStations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="outageForm.startTime" type="datetime" placeholder="选择开始时间" class="w-full" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="outageForm.endTime" type="datetime" placeholder="选择结束时间" class="w-full" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="outageForm.reason" type="textarea" :rows="3" placeholder="请说明停电原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitOutage">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { Station, MeterReading, SettlementRecord, PowerOutageNotice, OutageAffectedOrder } from '@/types'
import { getSiteOwnerStations, getMeterReadings, confirmMeterReading, getSiteOwnerSettlements, confirmSiteSettlement, getOutageNotices, createOutageNotice, cancelOutageNotice, getOutageAffectedOrders, activateOutage, settleIncompleteOrders } from '@/api/site-owner'

const activeTab = ref('meter')
const meterStationId = ref<number>()
const myStations = ref<Station[]>([])
const meterReadings = ref<MeterReading[]>([])
const shareRecords = ref<SettlementRecord[]>([])
const settlementRecords = ref<SettlementRecord[]>([])
const outageNotices = ref<PowerOutageNotice[]>([])
const outageDialogVisible = ref(false)

const outageForm = reactive({
  stationId: undefined as number | undefined,
  startTime: '',
  endTime: '',
  reason: '',
})

const affectedOrders = ref<OutageAffectedOrder[]>([])
const showAffectedDialog = ref(false)
const currentOutageId = ref<number>(0)
const settling = ref(false)

const MOCK_STATIONS: Station[] = [
  { id: 1, name: '朝阳万达站', address: '朝阳区建国路93号', longitude: 116.474, latitude: 39.908, totalPiles: 8, availablePiles: 3, status: 'ACTIVE', commissionRate: 15, createdAt: '2026-01-01' },
  { id: 3, name: '西城金融街站', address: '西城区金融大街甲15号', longitude: 116.356, latitude: 39.913, totalPiles: 6, availablePiles: 0, status: 'MAINTENANCE', commissionRate: 18, createdAt: '2026-02-01' },
]

const MOCK_METERS: MeterReading[] = [
  { id: 1, stationId: 1, readingType: 'TOTAL', kwh: 12580.5, readingDate: '2026-06-01' },
  { id: 2, stationId: 1, readingType: 'PROPERTY', kwh: 3145.12, readingDate: '2026-06-01' },
  { id: 3, stationId: 1, readingType: 'TOTAL', kwh: 11820.3, readingDate: '2026-05-01', confirmedBy: 4, confirmedAt: '2026-05-05' },
]

const MOCK_SETTLEMENTS: SettlementRecord[] = [
  { id: 1, orderId: 1, stationId: 1, totalElectricityFee: 2820, totalServiceFee: 470, operatorShare: 1830.5, siteOwnerShare: 958.5, propertyShare: 501, status: 'PENDING', period: '2026-06', createdAt: '2026-06-01' },
  { id: 2, orderId: 2, stationId: 1, totalElectricityFee: 3420, totalServiceFee: 570, operatorShare: 2220, siteOwnerShare: 1162.8, propertyShare: 607.2, status: 'SETTLED', period: '2026-05', createdAt: '2026-05-01', confirmedAt: '2026-05-16' },
]

const MOCK_OUTAGES: PowerOutageNotice[] = [
  { id: 1, stationId: 1, siteOwnerId: 4, startTime: '2026-06-15 08:00', endTime: '2026-06-15 18:00', reason: '配电房设备检修', status: 'SCHEDULED', affectedOrderIds: [] },
  { id: 2, stationId: 3, siteOwnerId: 4, startTime: '2026-06-05 06:00', endTime: '2026-06-05 12:00', reason: '线路升级改造', status: 'COMPLETED', affectedOrderIds: [] },
]

const shareSummary = computed(() => {
  let totalShare = 0, settled = 0, pending = 0
  shareRecords.value.forEach((r) => {
    totalShare += r.siteOwnerShare + r.propertyShare
    if (r.status === 'SETTLED') settled += r.siteOwnerShare + r.propertyShare
    else pending += r.siteOwnerShare + r.propertyShare
  })
  return { totalShare, settled, pending }
})

async function loadMeters() {
  if (!meterStationId.value) return
  try {
    const data = await getMeterReadings(meterStationId.value)
    meterReadings.value = Array.isArray(data) ? data : MOCK_METERS
  } catch {
    meterReadings.value = MOCK_METERS
  }
}

async function confirmReading(reading: MeterReading) {
  try {
    await confirmMeterReading(reading.id)
    loadMeters()
  } catch {
    reading.confirmedAt = new Date().toLocaleString()
    ElMessage.success('确认成功(演示)')
  }
}

async function confirmSettle(record: SettlementRecord) {
  try {
    await confirmSiteSettlement(record.id)
    loadAll()
  } catch {
    record.status = 'CONFIRMED'
    ElMessage.success('确认成功(演示)')
  }
}

async function cancelOutage(notice: PowerOutageNotice) {
  try {
    await cancelOutageNotice(notice.id)
    loadOutages()
  } catch {
    notice.status = 'CANCELLED'
    ElMessage.success('已取消(演示)')
  }
}

async function submitOutage() {
  try {
    await createOutageNotice(outageForm as any)
    outageDialogVisible.value = false
    loadOutages()
  } catch {
    outageNotices.value.unshift({
      id: Date.now(), stationId: outageForm.stationId!, siteOwnerId: 4,
      startTime: outageForm.startTime, endTime: outageForm.endTime,
      reason: outageForm.reason, status: 'SCHEDULED', affectedOrderIds: [],
    })
    ElMessage.success('发布成功(演示)')
    outageDialogVisible.value = false
  }
}

async function loadAll() {
  try {
    const data = await getSiteOwnerSettlements()
    shareRecords.value = Array.isArray(data?.records) ? data.records : MOCK_SETTLEMENTS
    settlementRecords.value = shareRecords.value
  } catch {
    shareRecords.value = MOCK_SETTLEMENTS
    settlementRecords.value = MOCK_SETTLEMENTS
  }
}

async function loadOutages() {
  try {
    const data = await getOutageNotices()
    outageNotices.value = Array.isArray(data?.records) ? data.records : MOCK_OUTAGES
  } catch {
    outageNotices.value = MOCK_OUTAGES
  }
}

async function viewAffectedOrders(notice: PowerOutageNotice) {
  currentOutageId.value = notice.id
  try {
    const data = await getOutageAffectedOrders(notice.id, notice.stationId, notice.startTime, notice.endTime)
    affectedOrders.value = Array.isArray(data?.data) ? data.data : Array.isArray(data) ? data : []
  } catch {
    affectedOrders.value = [
      { orderId: 1, orderNo: 'ORD20260602001', userId: 6, chargedKwh: 12.5, totalFee: 10.0, status: 'CHARGING' },
      { orderId: 2, orderNo: 'ORD20260602002', userId: 7, chargedKwh: 8.3, totalFee: 6.64, status: 'CHARGING' },
    ]
  }
  showAffectedDialog.value = true
}

async function activateOutageNotice(notice: PowerOutageNotice) {
  try {
    await activateOutage(notice.id)
    ElMessage.success('停电已激活，相关充电订单已中断')
    loadOutages()
  } catch {
    notice.status = 'ACTIVE'
    ElMessage.success('停电已激活(演示)')
  }
}

async function settleOrders() {
  settling.value = true
  try {
    await settleIncompleteOrders(currentOutageId.value)
    ElMessage.success('未完成订单已按实际电量结算')
    showAffectedDialog.value = false
    loadOutages()
  } catch {
    ElMessage.success('结算完成(演示)')
    showAffectedDialog.value = false
  }
  settling.value = false
}

onMounted(async () => {
  try {
    const data = await getSiteOwnerStations()
    myStations.value = Array.isArray(data) && data.length > 0 ? data : MOCK_STATIONS
  } catch {
    myStations.value = MOCK_STATIONS
  }
  if (myStations.value.length > 0) {
    meterStationId.value = myStations.value[0].id
    loadMeters()
  }
  loadAll()
  loadOutages()
})
</script>
