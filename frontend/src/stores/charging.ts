import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ChargingOrder, OrderStatus, Pile, Station, PileStatus } from '@/types'

export const useChargingStore = defineStore('charging', () => {
  const activeOrder = ref<ChargingOrder | null>(null)
  const activePileId = ref<number | null>(null)
  const activeStationId = ref<number | null>(null)
  const pileOverrides = ref<Map<number, PileStatus>>(new Map())
  const stationAvailableOverrides = ref<Map<number, number>>(new Map())

  const isCharging = computed(() =>
    activeOrder.value !== null && ['PENDING', 'HANDSHAKE', 'CHARGING'].includes(activeOrder.value.status),
  )

  function startSession(order: ChargingOrder, pileId: number, stationId: number) {
    activeOrder.value = { ...order }
    activePileId.value = pileId
    activeStationId.value = stationId
    pileOverrides.value.set(pileId, 'CHARGING')
    const currentAvail = stationAvailableOverrides.value.get(stationId) ?? 0
    stationAvailableOverrides.value.set(stationId, currentAvail - 1)
    saveOrderToList(activeOrder.value)
  }

  function updateOrderStatus(status: OrderStatus, extra?: Partial<ChargingOrder>) {
    if (!activeOrder.value) return
    activeOrder.value = { ...activeOrder.value, status, ...extra }
    updateOrderInList(activeOrder.value)
  }

  function updateChargingData(chargedKwh: number, electricityFee: number, serviceFee: number, totalFee: number) {
    if (!activeOrder.value) return
    activeOrder.value = {
      ...activeOrder.value,
      chargedKwh,
      electricityFee,
      serviceFee,
      totalFee,
    }
    updateOrderInList(activeOrder.value)
  }

  function endSession(completedOrder?: ChargingOrder) {
    if (completedOrder) {
      activeOrder.value = { ...completedOrder }
      updateOrderInList(activeOrder.value)
    }

    if (activePileId.value !== null) {
      pileOverrides.value.set(activePileId.value, 'IDLE')
    }
    if (activeStationId.value !== null) {
      const currentAvail = stationAvailableOverrides.value.get(activeStationId.value) ?? 0
      stationAvailableOverrides.value.set(activeStationId.value, currentAvail + 1)
    }

    setTimeout(() => {
      activeOrder.value = null
      activePileId.value = null
      activeStationId.value = null
    }, 100)
  }

  function cancelSession() {
    if (activePileId.value !== null) {
      pileOverrides.value.delete(activePileId.value)
    }
    if (activeStationId.value !== null) {
      const currentAvail = stationAvailableOverrides.value.get(activeStationId.value) ?? 0
      stationAvailableOverrides.value.set(activeStationId.value, currentAvail + 1)
      removeOrderFromList(activeOrder.value?.id || 0)
    }
    activeOrder.value = null
    activePileId.value = null
    activeStationId.value = null
  }

  function getPileStatus(pileId: number, defaultStatus: PileStatus): PileStatus {
    return pileOverrides.value.get(pileId) ?? defaultStatus
  }

  function getStationAvailablePiles(stationId: number, defaultAvailable: number): number {
    const override = stationAvailableOverrides.value.get(stationId)
    if (override !== undefined) {
      return Math.max(0, defaultAvailable + override)
    }
    return defaultAvailable
  }

  function applyPileOverrides(piles: Pile[]): Pile[] {
    return piles.map((p) => {
      const override = pileOverrides.value.get(p.id)
      if (override && override !== p.status) {
        return { ...p, status: override }
      }
      return p
    })
  }

  function applyStationOverrides(stations: Station[]): Station[] {
    return stations.map((s) => {
      const avail = getStationAvailablePiles(s.id, s.availablePiles)
      if (avail !== s.availablePiles) {
        return { ...s, availablePiles: avail }
      }
      return s
    })
  }

  function saveOrderToList(order: ChargingOrder) {
    try {
      const key = 'charging_orders'
      const existing: ChargingOrder[] = JSON.parse(localStorage.getItem(key) || '[]')
      const idx = existing.findIndex((o) => o.id === order.id)
      if (idx >= 0) {
        existing[idx] = { ...order }
      } else {
        existing.unshift({ ...order })
      }
      if (existing.length > 50) existing.length = 50
      localStorage.setItem(key, JSON.stringify(existing))
    } catch {
      /* ignore */
    }
  }

  function updateOrderInList(order: ChargingOrder) {
    saveOrderToList(order)
  }

  function removeOrderFromList(orderId: number) {
    try {
      const key = 'charging_orders'
      const existing: ChargingOrder[] = JSON.parse(localStorage.getItem(key) || '[]')
      const filtered = existing.filter((o) => o.id !== orderId)
      localStorage.setItem(key, JSON.stringify(filtered))
    } catch {
      /* ignore */
    }
  }

  function loadLocalOrders(): ChargingOrder[] {
    try {
      return JSON.parse(localStorage.getItem('charging_orders') || '[]')
    } catch {
      return []
    }
  }

  return {
    activeOrder,
    activePileId,
    activeStationId,
    isCharging,
    pileOverrides,
    stationAvailableOverrides,
    startSession,
    updateOrderStatus,
    updateChargingData,
    endSession,
    cancelSession,
    getPileStatus,
    getStationAvailablePiles,
    applyPileOverrides,
    applyStationOverrides,
    loadLocalOrders,
    saveOrderToList,
  }
})
