<template>
  <div class="page-container">
    <h2 class="page-title">充电下单</h2>

    <div v-if="!chargingActive" class="max-w-4xl mx-auto">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-5">
        <h3 class="font-semibold text-gray-800 mb-4">选择充电站</h3>
        <el-select v-model="selectedStationId" placeholder="请选择充电站" class="w-full" size="large" @change="onStationChange">
          <el-option v-for="s in stationList" :key="s.id" :label="`${s.name} (${s.availablePiles}/${s.totalPiles}空闲)`" :value="s.id" />
        </el-select>
      </div>

      <div v-if="selectedStationId" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-5">
        <h3 class="font-semibold text-gray-800 mb-4">选择空闲桩位</h3>
        <div class="grid grid-cols-4 gap-3">
          <div
            v-for="pile in idlePiles"
            :key="pile.id"
            class="p-4 rounded-xl border-2 cursor-pointer transition-all text-center"
            :class="selectedPileId === pile.id ? 'border-teal-600 bg-teal-50' : 'border-gray-200 hover:border-teal-300'"
            @click="selectedPileId = pile.id"
          >
            <Plug class="w-8 h-8 mx-auto mb-2" :class="selectedPileId === pile.id ? 'text-teal-600' : 'text-gray-400'" />
            <div class="font-bold text-gray-800">{{ pile.pileCode }}</div>
            <div class="text-sm text-gray-500">{{ pile.power }}kW · {{ pileTypeLabel(pile.type) }}</div>
          </div>
          <el-empty v-if="idlePiles.length === 0" description="暂无空闲桩位" :image-size="40" />
        </div>
      </div>

      <div v-if="selectedStationId" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h3 class="font-semibold text-gray-800 mb-4">当前电价</h3>
        <div class="grid grid-cols-3 gap-4 mb-5">
          <div class="p-4 rounded-xl bg-red-50 border border-red-100 text-center">
            <div class="text-sm text-red-600 mb-1">峰时</div>
            <div class="text-xl font-bold text-red-700">¥{{ pricing.peakPrice }}/kWh</div>
            <div class="text-xs text-red-400 mt-1">{{ pricing.peakHours }}</div>
          </div>
          <div class="p-4 rounded-xl bg-amber-50 border border-amber-100 text-center">
            <div class="text-sm text-amber-600 mb-1">平时</div>
            <div class="text-xl font-bold text-amber-700">¥{{ pricing.flatPrice }}/kWh</div>
            <div class="text-xs text-amber-400 mt-1">{{ pricing.flatHours }}</div>
          </div>
          <div class="p-4 rounded-xl bg-teal-50 border border-teal-100 text-center">
            <div class="text-sm text-teal-600 mb-1">谷时</div>
            <div class="text-xl font-bold text-teal-700">¥{{ pricing.valleyPrice }}/kWh</div>
            <div class="text-xs text-teal-400 mt-1">{{ pricing.valleyHours }}</div>
          </div>
        </div>
        <el-button type="primary" size="large" class="w-full" :disabled="!selectedPileId" style="height: 48px; border-radius: 10px" @click="startCharging">
          <BatteryCharging class="w-5 h-5 mr-2" />
          开始充电
        </el-button>
      </div>
    </div>

    <div v-else class="max-w-lg mx-auto">
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 text-center">
        <div class="w-20 h-20 rounded-full bg-teal-50 flex items-center justify-center mx-auto mb-5">
          <BatteryCharging class="w-10 h-10 text-teal-600 animate-pulse" />
        </div>
        <h3 class="text-lg font-bold text-gray-800 mb-2">充电中</h3>
        <div class="text-4xl font-bold text-teal-700 my-4">{{ chargingData.chargedKwh.toFixed(2) }} <span class="text-lg text-gray-400">kWh</span></div>
        <div class="grid grid-cols-2 gap-4 mb-6">
          <div class="p-3 bg-gray-50 rounded-lg">
            <div class="text-sm text-gray-500">已充时长</div>
            <div class="text-lg font-bold text-gray-800">{{ chargingData.duration }}</div>
          </div>
          <div class="p-3 bg-gray-50 rounded-lg">
            <div class="text-sm text-gray-500">当前费用</div>
            <div class="text-lg font-bold text-amber-600">¥{{ chargingData.totalFee.toFixed(2) }}</div>
          </div>
        </div>
        <div class="mb-6">
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div class="h-full bg-teal-500 rounded-full transition-all" :style="{ width: chargingData.progress + '%' }"></div>
          </div>
          <div class="text-xs text-gray-400 mt-1">{{ chargingData.progress.toFixed(0) }}%</div>
        </div>
        <el-button type="danger" size="large" class="w-full" style="height: 48px; border-radius: 10px" @click="stopCharging">
          停止充电
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Plug, BatteryCharging } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { Station, Pile, PileType, PricingRule } from '@/types'
import { getStations } from '@/api/station'
import { getPilesByStation } from '@/api/pile'
import { startCharging as startChargingApi, stopCharging as stopChargingApi } from '@/api/order'

const route = useRoute()
const selectedStationId = ref<number>()
const selectedPileId = ref<number>()
const chargingActive = ref(false)
let chargingTimer: ReturnType<typeof setInterval> | null = null

const stationList = ref<Station[]>([])
const piles = ref<Pile[]>([])

const chargingData = ref({
  chargedKwh: 0,
  totalFee: 0,
  duration: '00:00:00',
  progress: 0,
  startTime: 0,
  orderId: 0,
})

const pricing = ref<PricingRule>({
  id: 1, stationId: 0, peakPrice: 1.2, flatPrice: 0.8, valleyPrice: 0.4,
  peakHours: '08:00-11:00,18:00-21:00', flatHours: '07:00-08:00,11:00-18:00', valleyHours: '21:00-07:00',
  serviceFeeRate: 0.1,
})

const MOCK_STATIONS: Station[] = [
  { id: 1, name: '朝阳万达站', address: '朝阳区建国路93号', longitude: 116.474, latitude: 39.908, totalPiles: 8, availablePiles: 3, status: 'ACTIVE', commissionRate: 15, createdAt: '2026-01-01' },
  { id: 2, name: '海淀中关村站', address: '海淀区中关村大街27号', longitude: 116.316, latitude: 39.982, totalPiles: 12, availablePiles: 5, status: 'ACTIVE', commissionRate: 12, createdAt: '2026-01-15' },
]

const MOCK_PILES: Pile[] = [
  { id: 2, stationId: 1, pileCode: 'P-002', power: 120, type: 'DC_FAST', status: 'IDLE', createdAt: '2026-01-01' },
  { id: 4, stationId: 1, pileCode: 'P-004', power: 7, type: 'AC_SLOW', status: 'IDLE', createdAt: '2026-01-01' },
  { id: 7, stationId: 1, pileCode: 'P-007', power: 60, type: 'DC_SLOW', status: 'IDLE', createdAt: '2026-01-01' },
]

const idlePiles = computed(() => piles.value.filter((p) => p.status === 'IDLE'))

function pileTypeLabel(t: PileType) {
  const m: Record<string, string> = { DC_FAST: '直流快充', DC_SLOW: '直流慢充', AC_SLOW: '交流慢充' }
  return m[t] || t
}

async function onStationChange() {
  selectedPileId.value = undefined
  if (!selectedStationId.value) return
  try {
    const data = await getPilesByStation(selectedStationId.value)
    piles.value = Array.isArray(data) ? data.filter((p: Pile) => p.status === 'IDLE' || p.status === 'CHARGING' || p.status === 'FAULT' || p.status === 'OFFLINE') : MOCK_PILES
  } catch {
    piles.value = MOCK_PILES
  }
}

function startCharging() {
  if (!selectedPileId.value) return
  chargingActive.value = true
  chargingData.value = { chargedKwh: 0, totalFee: 0, duration: '00:00:00', progress: 0, startTime: Date.now(), orderId: Date.now() }
  chargingTimer = setInterval(() => {
    const elapsed = (Date.now() - chargingData.value.startTime) / 1000
    const hours = Math.floor(elapsed / 3600)
    const minutes = Math.floor((elapsed % 3600) / 60)
    const seconds = Math.floor(elapsed % 60)
    chargingData.value.duration = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    chargingData.value.chargedKwh += 0.15
    chargingData.value.totalFee = chargingData.value.chargedKwh * 0.8
    chargingData.value.progress = Math.min((chargingData.value.chargedKwh / 60) * 100, 100)
  }, 1000)
  ElMessage.success('开始充电(演示模式)')
}

async function stopCharging() {
  if (chargingTimer) {
    clearInterval(chargingTimer)
    chargingTimer = null
  }
  try {
    if (chargingData.value.orderId) await stopChargingApi(chargingData.value.orderId)
  } catch { /* demo */ }
  chargingActive.value = false
  ElMessage.success('充电已停止')
}

onMounted(async () => {
  try {
    const data = await getStations()
    stationList.value = Array.isArray(data) && data.length > 0 ? data : MOCK_STATIONS
  } catch {
    stationList.value = MOCK_STATIONS
  }
  const qStationId = route.query.stationId
  if (qStationId) {
    selectedStationId.value = Number(qStationId)
    onStationChange()
  }
})

onUnmounted(() => {
  if (chargingTimer) clearInterval(chargingTimer)
})
</script>
