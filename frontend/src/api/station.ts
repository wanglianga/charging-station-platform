import { get as apiGet, post as apiPost, put as apiPut, del as apiDel } from './index'
import type { Station, PricingRule } from '@/types'

export function getStations(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/stations', { params })
}

export function getStation(id: number): Promise<Station> {
  return apiGet(`/stations/${id}`)
}

export function createStation(data: Partial<Station>) {
  return apiPost('/stations', data)
}

export function updateStation(id: number, data: Partial<Station>) {
  return apiPut(`/stations/${id}`, data)
}

export function deleteStation(id: number) {
  return apiDel(`/stations/${id}`)
}

export function getPricingRule(stationId: number): Promise<PricingRule> {
  return apiGet(`/stations/${stationId}/pricing`)
}

export function updatePricingRule(stationId: number, data: Partial<PricingRule>) {
  return apiPut(`/stations/${stationId}/pricing`, data)
}
