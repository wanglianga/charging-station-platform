import { get as apiGet, put as apiPut } from './index'
import type { SettlementRecord } from '@/types'

export function getSettlements(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/settlements', { params })
}

export function getSettlement(id: number): Promise<SettlementRecord> {
  return apiGet(`/settlements/${id}`)
}

export function confirmSettlement(id: number) {
  return apiPut(`/settlements/${id}/confirm`)
}

export function disputeSettlement(id: number, reason: string) {
  return apiPut(`/settlements/${id}/dispute`, { reason })
}

export function getSettlementSummary(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/settlements/summary', { params })
}
