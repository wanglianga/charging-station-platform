import { get as apiGet, post as apiPost, put as apiPut } from './index'
import type { ChargingOrder, RefundRequest } from '@/types'

export function getOrders(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/orders', { params })
}

export function getOrder(id: number): Promise<ChargingOrder> {
  return apiGet(`/orders/${id}`)
}

export function startCharging(pileId: number) {
  return apiPost('/orders/start', { pileId })
}

export function stopCharging(orderId: number) {
  return apiPost(`/orders/${orderId}/stop`)
}

export function requestRefund(data: { orderId: number; reason: string; amount: number }): Promise<RefundRequest> {
  return apiPost('/refund-requests', data)
}

export function approveRefund(id: number) {
  return apiPut(`/refund-requests/${id}/approve`)
}

export function rejectRefund(id: number) {
  return apiPut(`/refund-requests/${id}/reject`)
}

export function getInterruptionDetail(orderId: number): Promise<any> {
  return apiGet(`/orders/${orderId}/interruption-detail`)
}

export function handleCompensationDecision(data: { compensationId: number; decision: string; switchTargetPileId?: number }): Promise<any> {
  return apiPost('/orders/compensation-decision', data)
}
