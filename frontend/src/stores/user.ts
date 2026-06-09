import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Role, LoginResponse } from '@/types'
import { login as loginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || 0)
  const username = ref(localStorage.getItem('username') || '')
  const name = ref(localStorage.getItem('name') || '')
  const role = ref<Role>((localStorage.getItem('role') as Role) || 'CAR_OWNER')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'OPERATOR')
  const isEngineer = computed(() => role.value === 'ENGINEER')
  const isCarOwner = computed(() => role.value === 'CAR_OWNER')
  const isSiteOwner = computed(() => role.value === 'SITE_OWNER')

  function setAuth(data: LoginResponse) {
    token.value = data.token
    userId.value = data.user.id
    username.value = data.user.username
    name.value = data.user.name
    role.value = data.user.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.user.id))
    localStorage.setItem('username', data.user.username)
    localStorage.setItem('name', data.user.name)
    localStorage.setItem('role', data.user.role)
  }

  async function login(usernameVal: string, passwordVal: string) {
    const data = await loginApi({ username: usernameVal, password: passwordVal })
    setAuth(data)
    return data
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    name.value = ''
    role.value = 'CAR_OWNER'
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('name')
    localStorage.removeItem('role')
  }

  return {
    token,
    userId,
    username,
    name,
    role,
    isLoggedIn,
    isAdmin,
    isEngineer,
    isCarOwner,
    isSiteOwner,
    setAuth,
    login,
    logout,
  }
})
