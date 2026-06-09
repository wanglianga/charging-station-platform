import { get as apiGet, post as apiPost, put as apiPut, del as apiDel } from './index'
import type { Pile } from '@/types'

export function getPiles(params?: Record<string, unknown>): Promise<any> {
  return apiGet('/piles', { params })
}

export function getPilesByStation(stationId: number): Promise<Pile[]> {
  return apiGet(`/stations/${stationId}/piles`)
}

export function getPile(id: number): Promise<Pile> {
  return apiGet(`/piles/${id}`)
}

export function createPile(data: Partial<Pile>) {
  return apiPost('/piles', data)
}

export function updatePile(id: number, data: Partial<Pile>) {
  return apiPut(`/piles/${id}`, data)
}

export function deletePile(id: number) {
  return apiDel(`/piles/${id}`)
}
