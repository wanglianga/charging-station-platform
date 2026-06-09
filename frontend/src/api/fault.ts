import { get as apiGet, put as apiPut } from './index'
import type { FaultTicket } from '@/types'

export function getFaultTickets(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/fault-tickets', { params })
}

export function getFaultTicket(id: number): Promise<FaultTicket> {
  return apiGet(`/fault-tickets/${id}`)
}

export function assignEngineer(id: number, engineerId: number) {
  return apiPut(`/fault-tickets/${id}/assign`, { engineerId })
}

export function updateFaultStatus(id: number, status: string) {
  return apiPut(`/fault-tickets/${id}/status`, { status })
}

export function resolveFault(id: number, data?: Record<string, unknown>) {
  return apiPut(`/fault-tickets/${id}/resolve`, data)
}

export function closeFault(id: number) {
  return apiPut(`/fault-tickets/${id}/close`)
}
