import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/types'

const ROLE_NAV_ACCESS: Record<Role, string[]> = {
  OPERATOR: ['dashboard', 'map', 'piles', 'faults', 'settlement', 'orders', 'stations', 'site-owner'],
  ENGINEER: ['dashboard', 'map', 'piles', 'faults'],
  CAR_OWNER: ['map', 'charging', 'orders'],
  SITE_OWNER: ['dashboard', 'settlement', 'site-owner'],
}

export function usePermission() {
  const userStore = useUserStore()

  const canAccess = computed(() => {
    const allowed = ROLE_NAV_ACCESS[userStore.role] || []
    return (navKey: string) => allowed.includes(navKey)
  })

  const canAccessRoute = (routeName: string) => {
    const allowed = ROLE_NAV_ACCESS[userStore.role] || []
    return allowed.includes(routeName)
  }

  const hasRole = (roles: Role[]) => {
    return roles.includes(userStore.role)
  }

  return { canAccess, canAccessRoute, hasRole }
}
