<template>
  <div class="page-container">
    <h2 class="page-title">仪表盘</h2>

    <div class="grid grid-cols-4 gap-5 mb-6">
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <span class="stat-label">总站点数</span>
          <Building2 class="w-8 h-8 text-teal-600 opacity-70" />
        </div>
        <div class="stat-value text-teal-700">{{ stats.totalStations }}</div>
      </div>
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <span class="stat-label">充电中订单</span>
          <BatteryCharging class="w-8 h-8 text-blue-500 opacity-70" />
        </div>
        <div class="stat-value text-blue-600">{{ stats.chargingOrders }}</div>
      </div>
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <span class="stat-label">待处理故障</span>
          <AlertTriangle class="w-8 h-8 text-amber-500 opacity-70" />
        </div>
        <div class="stat-value text-amber-600">{{ stats.pendingFaults }}</div>
      </div>
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <span class="stat-label">今日收入</span>
          <TrendingUp class="w-8 h-8 text-green-500 opacity-70" />
        </div>
        <div class="stat-value text-green-600">¥{{ stats.todayRevenue.toFixed(2) }}</div>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-5">
      <div class="col-span-2 bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 class="font-semibold text-gray-800 mb-4">近7日收入趋势</h3>
        <v-chart :option="revenueChartOption" style="height: 320px" autoresize />
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 class="font-semibold text-gray-800 mb-4">故障告警</h3>
        <div class="space-y-3">
          <div
            v-for="fault in faultAlerts"
            :key="fault.id"
            class="p-3 rounded-lg border"
            :class="severityClass(fault.severity)"
          >
            <div class="flex items-center justify-between mb-1">
              <el-tag :type="severityTagType(fault.severity)" size="small">{{ severityLabel(fault.severity) }}</el-tag>
              <span class="text-xs text-gray-400">{{ fault.createdAt }}</span>
            </div>
            <p class="text-sm text-gray-700">{{ fault.description }}</p>
          </div>
          <el-empty v-if="faultAlerts.length === 0" description="暂无告警" :image-size="60" />
        </div>
      </div>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 mt-5">
      <h3 class="font-semibold text-gray-800 mb-4">最近订单</h3>
      <el-table :data="recentOrders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="stationName" label="站点" width="160" />
        <el-table-column prop="chargedKwh" label="充电量(kWh)" width="120">
          <template #default="{ row }">{{ row.chargedKwh.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalFee" label="总费用" width="100">
          <template #default="{ row }">¥{{ row.totalFee.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Building2, BatteryCharging, AlertTriangle, TrendingUp } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { ChargingOrder, FaultTicket, FaultSeverity, OrderStatus } from '@/types'
import { getOrders } from '@/api/order'
import { getFaultTickets } from '@/api/fault'
import { getStations } from '@/api/station'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const stats = ref({
  totalStations: 0,
  chargingOrders: 0,
  pendingFaults: 0,
  todayRevenue: 0,
})

const recentOrders = ref<(ChargingOrder & { stationName?: string })[]>([])
const faultAlerts = ref<FaultTicket[]>([])

const MOCK_STATS = { totalStations: 12, chargingOrders: 8, pendingFaults: 3, todayRevenue: 3568.5 }
const MOCK_ORDERS: (ChargingOrder & { stationName?: string })[] = [
  { id: 1, orderNo: 'ORD20260609001', userId: 3, pileId: 1, stationId: 1, status: 'CHARGING', startKwh: 0, endKwh: 0, chargedKwh: 23.5, electricityFee: 28.2, serviceFee: 4.7, totalFee: 32.9, startTime: '2026-06-09 10:30:00', createdAt: '2026-06-09 10:30:00', stationName: '朝阳万达站' },
  { id: 2, orderNo: 'ORD20260609002', userId: 4, pileId: 2, stationId: 1, status: 'COMPLETED', startKwh: 0, endKwh: 0, chargedKwh: 45.2, electricityFee: 54.24, serviceFee: 9.04, totalFee: 63.28, startTime: '2026-06-09 09:15:00', endTime: '2026-06-09 11:20:00', createdAt: '2026-06-09 09:15:00', stationName: '朝阳万达站' },
  { id: 3, orderNo: 'ORD20260609003', userId: 5, pileId: 5, stationId: 2, status: 'COMPLETED', startKwh: 0, endKwh: 0, chargedKwh: 18.7, electricityFee: 22.44, serviceFee: 3.74, totalFee: 26.18, startTime: '2026-06-09 08:00:00', endTime: '2026-06-09 09:30:00', createdAt: '2026-06-09 08:00:00', stationName: '海淀中关村站' },
]
const MOCK_FAULTS: FaultTicket[] = [
  { id: 1, stationId: 1, pileId: 3, faultType: 'GUN_LINE_FAULT', severity: 'CRITICAL', status: 'PENDING', description: '3号桩枪线故障，无法充电', affectedOrderIds: [], createdAt: '2026-06-09 10:00' },
  { id: 2, stationId: 2, pileId: 6, faultType: 'MODULE_OVER_TEMP', severity: 'HIGH', status: 'ASSIGNED', description: '6号桩模块过温告警', assignedEngineerId: 2, assignedEngineerName: '张工程师', affectedOrderIds: [], createdAt: '2026-06-09 09:30' },
  { id: 3, stationId: 3, pileId: 9, faultType: 'COMM_OFFLINE', severity: 'MEDIUM', status: 'PENDING', description: '9号桩通信离线', affectedOrderIds: [], createdAt: '2026-06-09 08:45' },
]

const revenueChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['6/3', '6/4', '6/5', '6/6', '6/7', '6/8', '6/9'],
  },
  yAxis: { type: 'value', name: '元' },
  series: [
    {
      name: '收入',
      type: 'line',
      smooth: true,
      data: [2850, 3120, 2960, 3450, 3680, 3210, 3568],
      lineStyle: { color: '#0F766E', width: 3 },
      itemStyle: { color: '#0F766E' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(15,118,110,0.25)' },
            { offset: 1, color: 'rgba(15,118,110,0.02)' },
          ],
        },
      },
    },
  ],
}))

function severityLabel(s: FaultSeverity) {
  const m: Record<string, string> = { CRITICAL: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return m[s] || s
}

function severityTagType(s: FaultSeverity) {
  const m: Record<string, string> = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return m[s] || 'info'
}

function severityClass(s: FaultSeverity) {
  const m: Record<string, string> = {
    CRITICAL: 'border-red-200 bg-red-50',
    HIGH: 'border-amber-200 bg-amber-50',
    MEDIUM: 'border-blue-200 bg-blue-50',
    LOW: 'border-gray-200 bg-gray-50',
  }
  return m[s] || ''
}

function orderStatusLabel(s: OrderStatus) {
  const m: Record<string, string> = { PENDING: '待确认', HANDSHAKE: '握手中', CHARGING: '充电中', COMPLETED: '已完成', INTERRUPTED: '已中断', FAULT_INTERRUPT: '故障中断', REFUNDING: '退款中', REFUNDED: '已退款', OFFLINE_INTERRUPT: '离线中断' }
  return m[s] || s
}

function orderStatusType(s: OrderStatus) {
  const m: Record<string, string> = { PENDING: 'info', HANDSHAKE: 'info', CHARGING: '', COMPLETED: 'success', INTERRUPTED: 'warning', FAULT_INTERRUPT: 'danger', REFUNDING: 'warning', REFUNDED: 'info', OFFLINE_INTERRUPT: 'danger' }
  return m[s] || 'info'
}

onMounted(async () => {
  try {
    const [stationsData, ordersData, faultsData] = await Promise.all([
      getStations(),
      getOrders({ page: 1, size: 5 }),
      getFaultTickets({ status: 'PENDING', size: 5 }),
    ])
    stats.value.totalStations = stationsData?.length || MOCK_STATS.totalStations
    recentOrders.value = ordersData?.records || MOCK_ORDERS
    faultAlerts.value = faultsData?.records || MOCK_FAULTS
    stats.value.chargingOrders = MOCK_STATS.chargingOrders
    stats.value.pendingFaults = faultAlerts.value.length
    stats.value.todayRevenue = MOCK_STATS.todayRevenue
  } catch {
    stats.value = { ...MOCK_STATS }
    recentOrders.value = MOCK_ORDERS
    faultAlerts.value = MOCK_FAULTS
  }
})
</script>
