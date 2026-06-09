import { get as apiGet, post as apiPost, put as apiPut } from './index'
import type { MeterReading, PowerOutageNotice } from '@/types'

export function getMeterReadings(stationId: number): Promise<any> {
  return apiGet(`/stations/${stationId}/meter-readings`)
}

export function confirmMeterReading(id: number) {
  return apiPut(`/meter-readings/${id}/confirm`)
}

export function getOutageNotices(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/power-outages', { params })
}

export function createOutageNotice(data: Partial<PowerOutageNotice>) {
  return apiPost('/power-outages', data)
}

export function cancelOutageNotice(id: number) {
  return apiPut(`/power-outages/${id}/cancel`)
}

export function getSiteOwnerStations(): Promise<any> {
  return apiGet('/site-owner/stations')
}

export function getSiteOwnerSettlements(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/site-owner/settlements', { params })
}

export function confirmSiteSettlement(id: number) {
  return apiPut(`/settlements/${id}/confirm`)
}
