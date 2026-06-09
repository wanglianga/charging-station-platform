<template>
  <div class="page-container">
    <h2 class="page-title">充电下单</h2>

    <div v-if="!chargingActive" class="max-w-4xl mx-auto">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-5">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold text-gray-800">选择充电站</h3>
          <el-button text type="primary" @click="showQueueDrawer = true">
            <Users class="w-4 h-4 mr-1" />
            查看排队
          </el-button>
        </div>
        <el-select v-model="selectedStationId" placeholder="请选择充电站" class="w-full" size="large" @change="onStationChange">
          <el-option v-for="s in displayStationList" :key="s.id" :label="`${s.name} (${s.availablePiles}/${s.totalPiles}空闲)`" :value="s.id" />
        </el-select>
      </div>

      <div v-if="selectedStationId" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-5">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold text-gray-800">选择空闲桩位</h3>
          <el-tag v-if="stationQueueInfo" type="warning" size="small">
            排队 {{ stationQueueInfo.waitingCount}} 人 · 预计等待 {{ stationQueueInfo.estimatedWaitMinutes }} 分钟
          </el-tag>
        </div>
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
          <div v-if="idlePiles.length === 0 && displayPiles.length > 0" class="col-span-4">
            <div class="text-center py-6 text-gray-500">
              <Users class="w-8 h-8 mx-auto mb-2 text-amber-500" />
              <p class="font-medium">当前无空闲桩位</p>
              <p class="text-sm mt-1">可加入排队等待，预计等待 {{ stationQueueInfo?.estimatedWaitMinutes || 30 }} 分钟</p>
              <el-button type="warning" class="mt-3" @click="joinQueue">加入排队</el-button>
            </div>
          </div>
          <el-empty v-if="displayPiles.length === 0" description="暂无桩位数据" :image-size="40" />
        </div>
        <div v-if="displayPiles.length > idlePiles.length && idlePiles.length > 0" class="mt-4 pt-4 border-t border-gray-100">
          <div class="flex items-center gap-4 text-sm text-gray-500">
            <span>充电中: {{ displayPiles.filter(p => p.status === 'CHARGING').length }}</span>
            <span>故障: {{ displayPiles.filter(p => p.status === 'FAULT').length }}</span>
            <span>离线: {{ displayPiles.filter(p => p.status === 'OFFLINE').length }}</span>
            <span class="text-amber-600">排队: {{ stationQueueInfo?.waitingCount || 0 }} 人</span>
          </div>
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
        <el-button type="primary" size="large" class="w-full" :disabled="!selectedPileId" :loading="submitting" style="height: 48px; border-radius: 10px" @click="startCharging">
          <BatteryCharging class="w-5 h-5 mr-2" />
          开始充电
        </el-button>
      </div>
    </div>

    <div v-else class="max-w-lg mx-auto">
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 text-center">
        <div class="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-5" :class="orderStatus === 'HANDSHAKE' ? 'bg-blue-50' : 'bg-teal-50'">
          <BatteryCharging class="w-10 h-10 animate-pulse" :class="orderStatus === 'HANDSHAKE' ? 'text-blue-600' : 'text-teal-600'" />
        </div>
        <h3 class="text-lg font-bold text-gray-800 mb-2">
          {{ orderStatusLabel(orderStatus) }}
        </h3>
        <div v-if="orderStatus === 'HANDSHAKE'" class="text-gray-500 mb-4">桩体握手中，请稍候...</div>
        <div v-if="orderStatus === 'CHARGING'" class="text-4xl font-bold text-teal-700 my-4">{{ chargingData.chargedKwh.toFixed(2) }} <span class="text-lg text-gray-400">kWh</span></div>
        <div v-if="orderStatus === 'CHARGING'" class="grid grid-cols-2 gap-4 mb-6">
          <div class="p-3 bg-gray-50 rounded-lg">
            <div class="text-sm text-gray-500">已充时长</div>
            <div class="text-lg font-bold text-gray-800">{{ chargingData.duration }}</div>
          </div>
          <div class="p-3 bg-gray-50 rounded-lg">
            <div class="text-sm text-gray-500">当前费用</div>
            <div class="text-lg font-bold text-amber-600">¥{{ chargingData.totalFee.toFixed(2) }}</div>
          </div>
        </div>
        <div v-if="orderStatus === 'CHARGING'" class="mb-6">
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div class="h-full bg-teal-500 rounded-full transition-all" :style="{ width: chargingData.progress + '%' }"></div>
          </div>
          <div class="text-xs text-gray-400 mt-1">{{ chargingData.progress.toFixed(0) }}%</div>
        </div>
        <div v-if="orderStatus === 'HANDSHAKE'" class="mb-6">
          <div class="flex items-center justify-center gap-3">
            <div class="w-3 h-3 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0ms"></div>
            <div class="w-3 h-3 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 150ms"></div>
            <div class="w-3 h-3 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 300ms"></div>
          </div>
        </div>
        <div class="mb-4 p-3 bg-gray-50 rounded-lg text-left">
          <div class="text-sm font-medium text-gray-600 mb-2">订单状态变化</div>
          <div class="space-y-2">
            <div v-for="(change, idx) in statusChanges" :key="idx" class="flex items-center gap-2 text-sm">
              <span class="w-2 h-2 rounded-full" :class="idx === statusChanges.length - 1 ? 'bg-teal-500' : 'bg-gray-300'"></span>
              <span class="text-gray-500">{{ change.time }}</span>
              <el-tag size="small" :type="change.type">{{ change.label }}</el-tag>
              <span class="text-gray-400">{{ change.desc }}</span>
            </div>
          </div>
        </div>
        <div class="text-sm text-gray-400 mb-4">订单号: {{ currentOrderNo }}</div>
        <el-button v-if="orderStatus !== 'HANDSHAKE'" type="danger" size="large" class="w-full" style="height: 48px; border-radius: 10px" @click="stopCharging">
          停止充电
        </el-button>
        <el-button v-else size="large" class="w-full" style="height: 48px; border-radius: 10px" @click="cancelHandshake">
          取消
        </el-button>
      </div>
    </div>

    <el-drawer v-model="showQueueDrawer" title="排队信息" size="480px" @open="loadQueueInfo">
      <div v-if="queueInfoList.length === 0" class="text-center py-8 text-gray-400">暂无排队数据</div>
      <div v-for="qi in queueInfoList" :key="qi.stationId" class="mb-5 bg-gray-50 rounded-xl p-4">
        <div class="flex items-center justify-between mb-3">
          <h4 class="font-semibold text-gray-800">{{ qi.stationName }}</h4>
          <el-tag :type="qi.waitingCount > 0 ? 'warning' : 'success'" size="small">
            {{ qi.waitingCount > 0 ? `${qi.waitingCount}人排队` : '无需排队' }}
          </el-tag>
        </div>
        <div class="grid grid-cols-3 gap-3 mb-3 text-sm">
          <div class="text-center p-2 bg-white rounded-lg">
            <div class="text-gray-500">空闲桩位</div>
            <div class="text-lg font-bold text-green-600">{{ qi.idlePileCount }}/{{ qi.totalPileCount }}</div>
          </div>
          <div class="text-center p-2 bg-white rounded-lg">
            <div class="text-gray-500">排队人数</div>
            <div class="text-lg font-bold text-amber-600">{{ qi.waitingCount }}</div>
          </div>
          <div class="text-center p-2 bg-white rounded-lg">
            <div class="text-gray-500">预计等待</div>
            <div class="text-lg font-bold text-gray-800">{{ qi.estimatedWaitMinutes }}分</div>
          </div>
        </div>
        <div v-if="qi.queueItems.length > 0" class="bg-white rounded-lg p-3">
          <div class="text-sm font-medium text-gray-600 mb-2">排队列表</div>
          <div v-for="item in qi.queueItems" :key="item.id" class="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
            <div class="flex items-center gap-2">
              <span class="w-6 h-6 rounded-full bg-amber-100 text-amber-700 text-xs flex items-center justify-center font-bold">{{ item.position }}</span>
              <span class="text-sm text-gray-700">{{ item.userName }}</span>
            </div>
            <div class="text-xs text-gray-400">{{ pileTypeLabel(item.pileType) }} · 约{{ item.estimatedWaitMinutes }}分钟</div>
          </div>
        </div>
        <el-button v-if="qi.idlePileCount > 0" type="primary" size="small" class="w-full mt-3" @click="goCharging(qi.stationId)">
          前往充电
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plug, BatteryCharging, Users } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import type { Station, Pile, PileType, PricingRule, QueueInfo, OrderStatus } from '@/types'
import { getStations } from '@/api/station'
import { getPilesByStation } from '@/api/pile'
import { startCharging as startChargingApi, stopCharging as stopChargingApi } from '@/api/order'
import { useChargingStore } from '@/stores/charging'

const route = useRoute()
const router = useRouter()
const chargingStore = useChargingStore()
const selectedStationId = ref<number>()
const selectedPileId = ref<number>()
const chargingActive = ref(false)
const submitting = ref(false)
const showQueueDrawer = ref(false)
let chargingTimer: ReturnType<typeof setInterval> | null = null

const rawStationList = ref<Station[]>([])
const rawPiles = ref<Pile[]>([])
const orderStatus = ref<OrderStatus>('PENDING')
const currentOrderNo = ref('')
const currentOrderId = ref(0)

const chargingData = ref({
  chargedKwh: 0,
  totalFee: 0,
  duration: '00:00:00',
  progress: 0,
  startTime: 0,
})

const statusChanges = ref<{ time: string; label: string; desc: string; type: string }[]>([])

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
  { id: 8, stationId: 1, pileCode: 'P-008', power: 120, type: 'DC_FAST', status: 'CHARGING', createdAt: '2026-01-01' },
  { id: 9, stationId: 1, pileCode: 'P-009', power: 60, type: 'DC_SLOW', status: 'CHARGING', createdAt: '2026-01-01' },
  { id: 10, stationId: 1, pileCode: 'P-010', power: 120, type: 'DC_FAST', status: 'FAULT', createdAt: '2026-01-01' },
]

const displayStationList = computed(() => chargingStore.applyStationOverrides(rawStationList.value))
const displayPiles = computed(() => chargingStore.applyPileOverrides(rawPiles.value))

const stationQueueInfo = computed<QueueInfo | null>(() => {
  const station = displayStationList.value.find(s => s.id === selectedStationId.value)
  if (!station) return null
  const idle = displayPiles.value.filter(p => p.status === 'IDLE').length
  const charging = displayPiles.value.filter(p => p.status === 'CHARGING').length
  const waiting = Math.max(0, charging - idle)
  return {
    stationId: station.id,
    stationName: station.name,
    waitingCount: waiting,
    estimatedWaitMinutes: waiting * 30,
    idlePileCount: idle,
    totalPileCount: displayPiles.value.length,
    queueItems: waiting > 0 ? Array.from({ length: Math.min(waiting, 3) }, (_, i) => ({
      id: i + 1, userId: 100 + i, userName: `用户${100 + i}`, stationId: station.id, pileType: 'DC_FAST' as PileType, position: i + 1, estimatedWaitMinutes: (i + 1) * 30, createdAt: dayjs().add(-i * 10, 'minute').format('YYYY-MM-DD HH:mm:ss'),
    })) : [],
  }
})

const queueInfoList = ref<QueueInfo[]>([])

const idlePiles = computed(() => displayPiles.value.filter((p) => p.status === 'IDLE'))

function pileTypeLabel(t: PileType) {
  const m: Record<string, string> = { DC_FAST: '直流快充', DC_SLOW: '直流慢充', AC_SLOW: '交流慢充' }
  return m[t] || t
}

function orderStatusLabel(s: OrderStatus) {
  const m: Record<string, string> = { PENDING: '待确认', HANDSHAKE: '桩体握手中', CHARGING: '充电中', COMPLETED: '已完成', INTERRUPTED: '已中断', FAULT_INTERRUPT: '故障中断', REFUNDING: '退款中', REFUNDED: '已退款', OFFLINE_INTERRUPT: '离线中断' }
  return m[s] || s
}

function addStatusChange(status: OrderStatus, desc: string) {
  const typeMap: Record<string, string> = { PENDING: 'info', HANDSHAKE: '', CHARGING: 'success', COMPLETED: 'success', INTERRUPTED: 'warning', FAULT_INTERRUPT: 'danger' }
  const labelMap: Record<string, string> = { PENDING: '待确认', HANDSHAKE: '握手中', CHARGING: '充电中', COMPLETED: '已完成', INTERRUPTED: '已中断', FAULT_INTERRUPT: '故障中断' }
  statusChanges.value.push({
    time: dayjs().format('HH:mm:ss'),
    label: labelMap[status] || status,
    desc,
    type: typeMap[status] || 'info',
  })
}

async function onStationChange() {
  selectedPileId.value = undefined
  if (!selectedStationId.value) return
  try {
    const data = await getPilesByStation(selectedStationId.value)
    rawPiles.value = Array.isArray(data) ? data : MOCK_PILES.filter(p => p.stationId === selectedStationId.value)
  } catch {
    rawPiles.value = MOCK_PILES.filter(p => p.stationId === selectedStationId.value)
  }
}

async function startCharging() {
  if (!selectedPileId.value || !selectedStationId.value) return
  submitting.value = true

  let orderId = Date.now()
  let orderNo = 'ORD' + dayjs().format('YYYYMMDDHHmmss')

  try {
    const result = await startChargingApi(selectedPileId.value)
    const order = result?.data || result
    orderId = order?.id || orderId
    orderNo = order?.orderNo || orderNo
  } catch { /* demo fallback */ }

  currentOrderId.value = orderId
  currentOrderNo.value = orderNo

  const newOrder = {
    id: orderId,
    orderNo,
    userId: 0,
    pileId: selectedPileId.value,
    stationId: selectedStationId.value,
    status: 'PENDING' as OrderStatus,
    startKwh: 0,
    endKwh: 0,
    chargedKwh: 0,
    electricityFee: 0,
    serviceFee: 0,
    totalFee: 0,
    startTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    createdAt: dayjs().format('YYYY-MM-DD'),
  }

  chargingStore.startSession(newOrder, selectedPileId.value, selectedStationId.value)

  chargingActive.value = true
  statusChanges.value = []
  addStatusChange('PENDING', '订单已创建')
  orderStatus.value = 'PENDING'

  setTimeout(() => {
    orderStatus.value = 'HANDSHAKE'
    addStatusChange('HANDSHAKE', '桩体通信握手')
    chargingStore.updateOrderStatus('HANDSHAKE')
  }, 1000)

  setTimeout(() => {
    orderStatus.value = 'CHARGING'
    addStatusChange('CHARGING', '开始计量充电')
    chargingData.value = { chargedKwh: 0, totalFee: 0, duration: '00:00:00', progress: 0, startTime: Date.now() }
    chargingStore.updateOrderStatus('CHARGING', {
      startTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    })
    startChargingTimer()
  }, 3000)

  submitting.value = false
  ElMessage.success('充电请求已发送')
}

function cancelHandshake() {
  chargingStore.cancelSession()
  chargingActive.value = false
  orderStatus.value = 'PENDING'
  statusChanges.value = []
  currentOrderNo.value = ''
  currentOrderId.value = 0
  ElMessage.info('已取消')
}

function startChargingTimer() {
  if (chargingTimer) clearInterval(chargingTimer)
  chargingTimer = setInterval(() => {
    const elapsed = (Date.now() - chargingData.value.startTime) / 1000
    const hours = Math.floor(elapsed / 3600)
    const minutes = Math.floor((elapsed % 3600) / 60)
    const seconds = Math.floor(elapsed % 60)
    chargingData.value.duration = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    chargingData.value.chargedKwh += 0.15
    chargingData.value.totalFee = chargingData.value.chargedKwh * 0.8
    chargingData.value.progress = Math.min((chargingData.value.chargedKwh / 60) * 100, 100)
    chargingStore.updateChargingData(
      chargingData.value.chargedKwh,
      chargingData.value.chargedKwh * 0.7,
      chargingData.value.chargedKwh * 0.1,
      chargingData.value.totalFee,
    )
  }, 1000)
}

async function stopCharging() {
  if (chargingTimer) {
    clearInterval(chargingTimer)
    chargingTimer = null
  }
  try {
    if (currentOrderId.value) await stopChargingApi(currentOrderId.value)
  } catch { /* demo */ }
  addStatusChange('COMPLETED', `充电完成，共${chargingData.value.chargedKwh.toFixed(2)}kWh`)
  orderStatus.value = 'COMPLETED'

  const completedOrder = {
    id: currentOrderId.value,
    orderNo: currentOrderNo.value,
    userId: 0,
    pileId: selectedPileId.value || 0,
    stationId: selectedStationId.value || 0,
    status: 'COMPLETED' as OrderStatus,
    startKwh: 0,
    endKwh: chargingData.value.chargedKwh,
    chargedKwh: chargingData.value.chargedKwh,
    electricityFee: chargingData.value.chargedKwh * 0.7,
    serviceFee: chargingData.value.chargedKwh * 0.1,
    totalFee: chargingData.value.totalFee,
    startTime: dayjs(chargingData.value.startTime).format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    createdAt: dayjs().format('YYYY-MM-DD'),
  }

  chargingStore.endSession(completedOrder)

  setTimeout(() => {
    chargingActive.value = false
    orderStatus.value = 'PENDING'
    currentOrderNo.value = ''
    currentOrderId.value = 0
    ElMessage.success('充电已停止，订单已保存')
  }, 2000)
}

function loadQueueInfo() {
  const stations = displayStationList.value
  const result: QueueInfo[] = []
  for (const station of stations) {
    const stationPiles = station.id === selectedStationId.value
      ? displayPiles.value
      : []
    const idle = stationPiles.length > 0
      ? stationPiles.filter(p => p.status === 'IDLE').length
      : station.availablePiles
    const charging = stationPiles.length > 0
      ? stationPiles.filter(p => p.status === 'CHARGING').length
      : station.totalPiles - station.availablePiles
    const waiting = Math.max(0, Math.floor(charging * 0.3))
    result.push({
      stationId: station.id,
      stationName: station.name,
      waitingCount: waiting,
      estimatedWaitMinutes: waiting * 30,
      idlePileCount: idle,
      totalPileCount: station.totalPiles,
      queueItems: waiting > 0 ? Array.from({ length: Math.min(waiting, 5) }, (_, i) => ({
        id: i + 1, userId: 200 + i, userName: `用户${200 + i}`, stationId: station.id, pileType: (i % 2 === 0 ? 'DC_FAST' : 'AC_SLOW') as PileType, position: i + 1, estimatedWaitMinutes: (i + 1) * 30, createdAt: dayjs().add(-i * 8, 'minute').format('YYYY-MM-DD HH:mm:ss'),
      })) : [],
    })
  }
  queueInfoList.value = result
}

function joinQueue() {
  ElMessage.success('已加入排队，有空闲桩位时将通知您')
}

function goCharging(stationId: number) {
  showQueueDrawer.value = false
  selectedStationId.value = stationId
  onStationChange()
}

onMounted(async () => {
  try {
    const data = await getStations()
    rawStationList.value = Array.isArray(data) && data.length > 0 ? data : MOCK_STATIONS
  } catch {
    rawStationList.value = MOCK_STATIONS
  }

  if (chargingStore.isCharging && chargingStore.activeOrder) {
    restoreChargingSession()
  } else {
    const qStationId = route.query.stationId
    if (qStationId) {
      selectedStationId.value = Number(qStationId)
      onStationChange()
    }
    if (route.query.showQueue === '1') {
      showQueueDrawer.value = true
      loadQueueInfo()
    }
  }
})

function restoreChargingSession() {
  const order = chargingStore.activeOrder!
  const pileId = chargingStore.activePileId!
  const stationId = chargingStore.activeStationId!

  selectedStationId.value = stationId
  selectedPileId.value = pileId
  onStationChange()

  currentOrderNo.value = order.orderNo
  currentOrderId.value = order.id
  orderStatus.value = order.status
  chargingActive.value = true

  if (order.chargedKwh > 0) {
    chargingData.value.chargedKwh = order.chargedKwh
    chargingData.value.totalFee = order.totalFee
    chargingData.value.progress = Math.min(95, (order.chargedKwh / 60) * 100)
  }

  if (order.status === 'CHARGING') {
    chargingData.value.startTime = order.startTime ? new Date(order.startTime).getTime() : Date.now()
    startChargingTimer()
  }

  statusChanges.value = [
    { time: order.startTime || order.createdAt, label: '已创建', desc: '订单已创建', type: 'primary' },
    { time: order.startTime || order.createdAt, label: '握手中', desc: '桩体通信握手', type: 'warning' },
  ]
  if (order.status === 'CHARGING') {
    statusChanges.value.push({ time: order.startTime || order.createdAt, label: '充电中', desc: '开始计量充电', type: 'success' })
  }
}

onUnmounted(() => {
  if (chargingTimer) clearInterval(chargingTimer)
})
</script>
