<template>
  <div class="page-container flex gap-5 h-[calc(100vh-7.5rem)]">
    <div class="w-72 flex-shrink-0 bg-white rounded-xl shadow-sm border border-gray-100 p-4 overflow-y-auto">
      <h3 class="font-semibold text-gray-800 mb-4">站点筛选</h3>
      <el-form label-position="top" size="default">
        <el-form-item label="站点名称">
          <el-input v-model="filter.name" placeholder="搜索站点名称" clearable />
        </el-form-item>
        <el-form-item label="站点状态">
          <el-select v-model="filter.status" placeholder="全部状态" clearable class="w-full">
            <el-option label="运营中" value="ACTIVE" />
            <el-option label="已停用" value="INACTIVE" />
            <el-option label="维护中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="桩位类型">
          <el-select v-model="filter.pileType" placeholder="全部类型" clearable class="w-full">
            <el-option label="直流快充" value="DC_FAST" />
            <el-option label="直流慢充" value="DC_SLOW" />
            <el-option label="交流慢充" value="AC_SLOW" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="mt-4 pt-4 border-t border-gray-100">
        <h4 class="text-sm font-medium text-gray-500 mb-3">图例说明</h4>
        <div class="space-y-2 text-sm">
          <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-full bg-green-500"></span>空闲</div>
          <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-full bg-blue-500"></span>充电中</div>
          <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-full bg-red-500"></span>故障</div>
          <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-full bg-gray-400"></span>离线</div>
        </div>
      </div>
    </div>

    <div class="flex-1 rounded-xl overflow-hidden shadow-sm border border-gray-100">
      <div ref="mapContainer" class="w-full h-full"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import L from 'leaflet'
import type { Station } from '@/types'
import { getStations } from '@/api/station'
import { useChargingStore } from '@/stores/charging'
import 'leaflet/dist/leaflet.css'

const router = useRouter()
const chargingStore = useChargingStore()
const mapContainer = ref<HTMLDivElement>()
let map: L.Map | null = null
const markers: L.Marker[] = []

const filter = reactive({
  name: '',
  status: '',
  pileType: '',
})

const MOCK_STATIONS: Station[] = [
  { id: 1, name: '朝阳万达站', address: '朝阳区建国路93号', longitude: 116.474, latitude: 39.908, totalPiles: 8, availablePiles: 3, status: 'ACTIVE', siteOwnerName: '万达物业', commissionRate: 15, createdAt: '2026-01-01' },
  { id: 2, name: '海淀中关村站', address: '海淀区中关村大街27号', longitude: 116.316, latitude: 39.982, totalPiles: 12, availablePiles: 5, status: 'ACTIVE', siteOwnerName: '中关村物业', commissionRate: 12, createdAt: '2026-01-15' },
  { id: 3, name: '西城金融街站', address: '西城区金融大街甲15号', longitude: 116.356, latitude: 39.913, totalPiles: 6, availablePiles: 0, status: 'MAINTENANCE', siteOwnerName: '金融街物业', commissionRate: 18, createdAt: '2026-02-01' },
  { id: 4, name: '丰台科技园站', address: '丰台区科技园区2号', longitude: 116.293, latitude: 39.842, totalPiles: 10, availablePiles: 7, status: 'ACTIVE', siteOwnerName: '丰台物业', commissionRate: 10, createdAt: '2026-03-01' },
  { id: 5, name: '通州运河站', address: '通州区运河东大街', longitude: 116.662, latitude: 39.902, totalPiles: 8, availablePiles: 4, status: 'ACTIVE', siteOwnerName: '通州物业', commissionRate: 13, createdAt: '2026-03-15' },
]

const rawStations = ref<Station[]>(MOCK_STATIONS)
const stations = computed(() => chargingStore.applyStationOverrides(rawStations.value))

function getStatusColor(station: Station): string {
  if (station.status === 'MAINTENANCE') return '#EF4444'
  if (station.status === 'INACTIVE') return '#9CA3AF'
  if (station.availablePiles === 0) return '#F59E0B'
  return '#10B981'
}

function getStatusText(station: Station): string {
  if (station.status === 'MAINTENANCE') return '维护中'
  if (station.status === 'INACTIVE') return '已停用'
  if (station.availablePiles === 0) return '已满'
  return '空闲'
}

function createMarker(station: Station): L.Marker {
  const color = getStatusColor(station)
  const icon = L.divIcon({
    html: `<div style="background:${color};width:28px;height:28px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;"><span style="color:white;font-size:10px;font-weight:bold;">${station.availablePiles}</span></div>`,
    className: '',
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })

  const marker = L.marker([station.latitude, station.longitude], { icon }).addTo(map!)

  const queueCount = Math.max(0, Math.floor((station.totalPiles - station.availablePiles) * 0.3))
  const waitMin = queueCount * 30
  const popupContent = `
    <div style="min-width:220px;padding:4px;">
      <h4 style="font-size:14px;font-weight:600;margin-bottom:8px;color:#0F766E;">${station.name}</h4>
      <p style="font-size:12px;color:#6B7280;margin-bottom:4px;">📍 ${station.address}</p>
      <p style="font-size:12px;color:#6B7280;margin-bottom:4px;">⚡ 空闲桩位: ${station.availablePiles}/${station.totalPiles}</p>
      <p style="font-size:12px;color:${queueCount > 0 ? '#D97706' : '#10B981'};margin-bottom:4px;">👥 排队: ${queueCount}人${queueCount > 0 ? ' · 约' + waitMin + '分钟' : ' · 无需等待'}</p>
      <p style="font-size:12px;color:#6B7280;margin-bottom:8px;">📊 状态: ${getStatusText(station)}</p>
      <div style="display:flex;gap:6px;">
        <button onclick="window.__mapNavCharging__(${station.id})" style="flex:1;padding:6px;background:#0F766E;color:white;border:none;border-radius:6px;cursor:pointer;font-size:12px;">去充电</button>
        <button onclick="window.__mapViewQueue__(${station.id})" style="flex:1;padding:6px;background:#F59E0B;color:white;border:none;border-radius:6px;cursor:pointer;font-size:12px;">查看排队</button>
      </div>
    </div>
  `
  marker.bindPopup(popupContent)
  return marker
}

;(window as any).__mapNavCharging__ = (stationId: number) => {
  router.push({ path: '/charging', query: { stationId: String(stationId) } })
}

;(window as any).__mapViewQueue__ = (stationId: number) => {
  router.push({ path: '/charging', query: { stationId: String(stationId), showQueue: '1' } })
}

function renderMarkers(list: Station[]) {
  markers.forEach((m) => m.remove())
  markers.length = 0
  list.forEach((s) => markers.push(createMarker(s)))
}

const filteredStations = () => {
  let result = stations.value
  if (filter.name) {
    result = result.filter((s) => s.name.includes(filter.name))
  }
  if (filter.status) {
    result = result.filter((s) => s.status === filter.status)
  }
  return result
}

watch(filter, () => {
  renderMarkers(filteredStations())
})

watch(() => [chargingStore.stationAvailableOverrides, chargingStore.pileOverrides], () => {
  renderMarkers(filteredStations())
}, { deep: true })

onMounted(async () => {
  try {
    const data = await getStations()
    if (Array.isArray(data) && data.length > 0) rawStations.value = data
  } catch { /* use mock */ }

  if (mapContainer.value) {
    map = L.map(mapContainer.value).setView([39.92, 116.40], 11)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap',
    }).addTo(map)
    renderMarkers(filteredStations())
  }
})

onUnmounted(() => {
  map?.remove()
  map = null
})
</script>
