<template>
  <div class="page-container">
    <h2 class="page-title">充电桩状态监控</h2>

    <div class="filter-bar">
      <el-select v-model="selectedStationId" placeholder="选择站点" class="w-64" @change="loadPiles">
        <el-option v-for="s in stationList" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-tag type="info" size="large">总计 {{ piles.length }} 个桩位</el-tag>
      <div class="flex-1"></div>
      <div v-if="queueInfo" class="flex items-center gap-3">
        <el-tag :type="queueInfo.idlePileCount > 0 ? 'success' : 'warning'" effect="plain">
          空闲 {{ queueInfo.idlePileCount }}/{{ queueInfo.totalPileCount }}
        </el-tag>
        <el-tag :type="queueInfo.waitingCount > 0 ? 'warning' : 'success'" effect="plain">
          排队 {{ queueInfo.waitingCount }} 人
        </el-tag>
        <el-tag v-if="queueInfo.waitingCount > 0" type="warning" effect="plain">
          预计等待 {{ queueInfo.estimatedWaitMinutes }} 分钟
        </el-tag>
      </div>
    </div>

    <div class="grid grid-cols-4 gap-4">
      <div
        v-for="pile in piles"
        :key="pile.id"
        class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 cursor-pointer hover:shadow-md transition-shadow"
        @click="openDrawer(pile)"
      >
        <div class="flex items-center justify-between mb-3">
          <span class="font-bold text-gray-800">{{ pile.pileCode }}</span>
          <el-tag :type="pileStatusType(pile.status)" size="small" effect="dark">{{ pileStatusLabel(pile.status) }}</el-tag>
        </div>
        <div class="space-y-2 text-sm text-gray-500">
          <div class="flex justify-between">
            <span>额定功率</span>
            <span class="text-gray-700 font-medium">{{ pile.power }} kW</span>
          </div>
          <div class="flex justify-between">
            <span>接口类型</span>
            <span class="text-gray-700 font-medium">{{ pileTypeLabel(pile.type) }}</span>
          </div>
          <div v-if="pile.status === 'CHARGING'" class="flex justify-between">
            <span>当前电量</span>
            <span class="text-teal-700 font-medium">{{ pile.currentOrder?.chargedKwh?.toFixed(2) || '0.00' }} kWh</span>
          </div>
        </div>
        <div class="mt-3 h-1.5 bg-gray-100 rounded-full overflow-hidden">
          <div
            class="h-full rounded-full transition-all"
            :class="pile.status === 'CHARGING' ? 'bg-teal-500' : pile.status === 'IDLE' ? 'bg-green-400' : pile.status === 'FAULT' ? 'bg-red-400' : 'bg-gray-300'"
            :style="{ width: pile.status === 'CHARGING' ? '65%' : pile.status === 'IDLE' ? '0%' : '100%' }"
          ></div>
        </div>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" :title="selectedPile?.pileCode + ' - 充电详情'" size="420px">
      <template v-if="selectedPile?.currentOrder">
        <div class="space-y-4">
          <div class="bg-teal-50 rounded-xl p-4">
            <div class="text-center">
              <div class="text-3xl font-bold text-teal-700">{{ selectedPile.currentOrder.chargedKwh?.toFixed(2) || '0.00' }}</div>
              <div class="text-sm text-teal-600 mt-1">已充电量 (kWh)</div>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <div class="text-lg font-bold text-gray-800">¥{{ selectedPile.currentOrder.totalFee?.toFixed(2) || '0.00' }}</div>
              <div class="text-xs text-gray-500">总费用</div>
            </div>
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <div class="text-lg font-bold text-gray-800">¥{{ selectedPile.currentOrder.electricityFee?.toFixed(2) || '0.00' }}</div>
              <div class="text-xs text-gray-500">电费</div>
            </div>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="订单号">{{ selectedPile.currentOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ selectedPile.currentOrder.startTime }}</el-descriptions-item>
            <el-descriptions-item label="服务费">¥{{ selectedPile.currentOrder.serviceFee?.toFixed(2) || '0.00' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
      <template v-else>
        <el-empty description="该桩位当前无充电订单" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { Station, Pile, PileStatus, PileType, ChargingOrder, QueueInfo } from '@/types'
import { getStations } from '@/api/station'
import { getPilesByStation } from '@/api/pile'
import { useChargingStore } from '@/stores/charging'

const chargingStore = useChargingStore()
const selectedStationId = ref<number>()
const rawStationList = ref<Station[]>([])
const rawPiles = ref<Pile[]>([])
const drawerVisible = ref(false)
const selectedPile = ref<Pile | null>(null)

const stationList = computed(() => chargingStore.applyStationOverrides(rawStationList.value))
const piles = computed(() => chargingStore.applyPileOverrides(rawPiles.value))

const queueInfo = computed<QueueInfo | null>(() => {
  const station = stationList.value.find(s => s.id === selectedStationId.value)
  if (!station) return null
  const idle = piles.value.filter(p => p.status === 'IDLE').length
  const charging = piles.value.filter(p => p.status === 'CHARGING').length
  const waiting = Math.max(0, Math.floor(charging * 0.3))
  return {
    stationId: station.id,
    stationName: station.name,
    waitingCount: waiting,
    estimatedWaitMinutes: waiting * 30,
    idlePileCount: idle,
    totalPileCount: piles.value.length,
    queueItems: [],
  }
})

const MOCK_STATIONS: Station[] = [
  { id: 1, name: '朝阳万达站', address: '朝阳区建国路93号', longitude: 116.474, latitude: 39.908, totalPiles: 8, availablePiles: 3, status: 'ACTIVE', commissionRate: 15, createdAt: '2026-01-01' },
  { id: 2, name: '海淀中关村站', address: '海淀区中关村大街27号', longitude: 116.316, latitude: 39.982, totalPiles: 12, availablePiles: 5, status: 'ACTIVE', commissionRate: 12, createdAt: '2026-01-15' },
]

const MOCK_PILES: (Pile & { currentOrder?: ChargingOrder })[] = [
  { id: 1, stationId: 1, pileCode: 'P-001', power: 120, type: 'DC_FAST', status: 'CHARGING', currentOrder: { id: 10, orderNo: 'ORD001', userId: 3, pileId: 1, stationId: 1, status: 'CHARGING', startKwh: 0, endKwh: 0, chargedKwh: 23.5, electricityFee: 28.2, serviceFee: 4.7, totalFee: 32.9, startTime: '2026-06-09 10:30:00', createdAt: '2026-06-09' }, createdAt: '2026-01-01' },
  { id: 2, stationId: 1, pileCode: 'P-002', power: 120, type: 'DC_FAST', status: 'IDLE', createdAt: '2026-01-01' },
  { id: 3, stationId: 1, pileCode: 'P-003', power: 60, type: 'DC_SLOW', status: 'FAULT', createdAt: '2026-01-01' },
  { id: 4, stationId: 1, pileCode: 'P-004', power: 7, type: 'AC_SLOW', status: 'IDLE', createdAt: '2026-01-01' },
  { id: 5, stationId: 1, pileCode: 'P-005', power: 120, type: 'DC_FAST', status: 'CHARGING', currentOrder: { id: 11, orderNo: 'ORD002', userId: 4, pileId: 5, stationId: 1, status: 'CHARGING', startKwh: 0, endKwh: 0, chargedKwh: 45.2, electricityFee: 54.24, serviceFee: 9.04, totalFee: 63.28, startTime: '2026-06-09 09:15:00', createdAt: '2026-06-09' }, createdAt: '2026-01-01' },
  { id: 6, stationId: 1, pileCode: 'P-006', power: 60, type: 'DC_SLOW', status: 'OFFLINE', createdAt: '2026-01-01' },
  { id: 7, stationId: 1, pileCode: 'P-007', power: 7, type: 'AC_SLOW', status: 'IDLE', createdAt: '2026-01-01' },
  { id: 8, stationId: 1, pileCode: 'P-008', power: 120, type: 'DC_FAST', status: 'EMERGENCY_STOP', createdAt: '2026-01-01' },
]

function pileStatusLabel(s: PileStatus) {
  const m: Record<string, string> = { IDLE: '空闲', CHARGING: '充电中', FAULT: '故障', OFFLINE: '离线', EMERGENCY_STOP: '急停' }
  return m[s] || s
}

function pileStatusType(s: PileStatus) {
  const m: Record<string, string> = { IDLE: 'success', CHARGING: '', FAULT: 'danger', OFFLINE: 'info', EMERGENCY_STOP: 'danger' }
  return m[s] || 'info'
}

function pileTypeLabel(t: PileType) {
  const m: Record<string, string> = { DC_FAST: '直流快充', DC_SLOW: '直流慢充', AC_SLOW: '交流慢充' }
  return m[t] || t
}

function openDrawer(pile: Pile) {
  selectedPile.value = pile
  drawerVisible.value = true
}

async function loadPiles() {
  if (!selectedStationId.value) return
  try {
    const data = await getPilesByStation(selectedStationId.value)
    rawPiles.value = Array.isArray(data) ? data : MOCK_PILES
  } catch {
    rawPiles.value = MOCK_PILES
  }
}

onMounted(async () => {
  try {
    const data = await getStations()
    rawStationList.value = Array.isArray(data) && data.length > 0 ? data : MOCK_STATIONS
  } catch {
    rawStationList.value = MOCK_STATIONS
  }
  if (stationList.value.length > 0) {
    selectedStationId.value = stationList.value[0].id
    loadPiles()
  }
})
</script>
