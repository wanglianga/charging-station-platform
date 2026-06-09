<template>
  <div class="page-container">
    <h2 class="page-title">费用分摊</h2>

    <div class="grid grid-cols-4 gap-5 mb-6">
      <div class="stat-card">
        <div class="stat-label">总电费</div>
        <div class="stat-value text-teal-700">¥{{ summary.totalElectricityFee.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">场地方分成</div>
        <div class="stat-value text-amber-600">¥{{ summary.siteOwnerShare.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">物业分摊</div>
        <div class="stat-value text-blue-600">¥{{ summary.propertyShare.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">运营方收入</div>
        <div class="stat-value text-green-600">¥{{ summary.operatorShare.toFixed(2) }}</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker v-model="filterPeriod" type="month" placeholder="选择结算周期" format="YYYY-MM" value-format="YYYY-MM" />
      <el-select v-model="filterStatus" placeholder="结算状态" clearable class="w-36">
        <el-option label="待确认" value="PENDING" />
        <el-option label="已确认" value="CONFIRMED" />
        <el-option label="有争议" value="DISPUTED" />
        <el-option label="已结算" value="SETTLED" />
      </el-select>
      <el-button type="primary" @click="loadSettlements">查询</el-button>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100">
      <el-table :data="filteredRecords" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="period" label="结算周期" width="120" />
        <el-table-column label="总电费" width="120">
          <template #default="{ row }">¥{{ row.totalElectricityFee.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="服务费" width="120">
          <template #default="{ row }">¥{{ row.totalServiceFee.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="运营方" width="120">
          <template #default="{ row }">¥{{ row.operatorShare.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="场地方" width="120">
          <template #default="{ row }">¥{{ row.siteOwnerShare.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="物业" width="120">
          <template #default="{ row }">¥{{ row.propertyShare.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="settlementStatusType(row.status)" size="small">{{ settlementStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="primary" text size="small" @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" type="success" text size="small" @click="handleSettle(row)">结算</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { SettlementRecord, SettlementStatus } from '@/types'
import { getSettlements, confirmSettlement } from '@/api/settlement'

const filterPeriod = ref('')
const filterStatus = ref('')
const records = ref<SettlementRecord[]>([])

const summary = computed(() => {
  const s = { totalElectricityFee: 0, siteOwnerShare: 0, propertyShare: 0, operatorShare: 0 }
  filteredRecords.value.forEach((r) => {
    s.totalElectricityFee += r.totalElectricityFee
    s.siteOwnerShare += r.siteOwnerShare
    s.propertyShare += r.propertyShare
    s.operatorShare += r.operatorShare
  })
  return s
})

const filteredRecords = computed(() => {
  let result = records.value
  if (filterPeriod.value) result = result.filter((r) => r.period === filterPeriod.value)
  if (filterStatus.value) result = result.filter((r) => r.status === filterStatus.value)
  return result
})

const MOCK_RECORDS: SettlementRecord[] = [
  { id: 1, orderId: 1, stationId: 1, totalElectricityFee: 2820, totalServiceFee: 470, operatorShare: 1830.5, siteOwnerShare: 958.5, propertyShare: 501, status: 'PENDING', period: '2026-06', createdAt: '2026-06-01' },
  { id: 2, orderId: 2, stationId: 1, totalElectricityFee: 3420, totalServiceFee: 570, operatorShare: 2220, siteOwnerShare: 1162.8, propertyShare: 607.2, status: 'CONFIRMED', period: '2026-05', createdAt: '2026-05-01', confirmedAt: '2026-05-15' },
  { id: 3, orderId: 3, stationId: 2, totalElectricityFee: 2180, totalServiceFee: 261.6, operatorShare: 1936.5, siteOwnerShare: 327.36, propertyShare: 177.74, status: 'SETTLED', period: '2026-05', createdAt: '2026-05-01', confirmedAt: '2026-05-16' },
  { id: 4, orderId: 4, stationId: 3, totalElectricityFee: 1560, totalServiceFee: 280.8, operatorShare: 1123.2, siteOwnerShare: 514.08, propertyShare: 203.52, status: 'DISPUTED', period: '2026-04', createdAt: '2026-04-01', confirmedAt: '2026-04-20' },
]

function settlementStatusLabel(s: SettlementStatus) {
  const m: Record<string, string> = { PENDING: '待确认', CONFIRMED: '已确认', DISPUTED: '有争议', SETTLED: '已结算' }
  return m[s] || s
}
function settlementStatusType(s: SettlementStatus) {
  const m: Record<string, string> = { PENDING: 'warning', CONFIRMED: '', DISPUTED: 'danger', SETTLED: 'success' }
  return m[s] || 'info'
}

async function handleConfirm(record: SettlementRecord) {
  try {
    await confirmSettlement(record.id)
    loadSettlements()
  } catch {
    record.status = 'CONFIRMED'
    record.confirmedAt = new Date().toLocaleString()
    ElMessage.success('确认成功(演示)')
  }
}

async function handleSettle(record: SettlementRecord) {
  record.status = 'SETTLED'
  ElMessage.success('结算完成(演示)')
}

async function loadSettlements() {
  try {
    const data = await getSettlements({ period: filterPeriod.value, status: filterStatus.value })
    records.value = Array.isArray(data?.records) ? data.records : MOCK_RECORDS
  } catch {
    records.value = MOCK_RECORDS
  }
}

onMounted(() => loadSettlements())
</script>
