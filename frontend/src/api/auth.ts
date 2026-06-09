import { post as apiPost, get as apiGet } from './index'
import type { LoginRequest, LoginResponse } from '@/types'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return apiPost<LoginResponse>('/auth/login', data)
}

export function getCurrentUser() {
  return apiGet('/auth/me')
}
