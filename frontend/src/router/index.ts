import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layout/MainLayout.vue'
import LoginPage from '@/pages/LoginPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import MapPage from '@/pages/MapPage.vue'
import PilesPage from '@/pages/PilesPage.vue'
import FaultsPage from '@/pages/FaultsPage.vue'
import SettlementPage from '@/pages/SettlementPage.vue'
import ChargingPage from '@/pages/ChargingPage.vue'
import OrdersPage from '@/pages/OrdersPage.vue'
import StationsPage from '@/pages/StationsPage.vue'
import SiteOwnerPage from '@/pages/SiteOwnerPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardPage,
          meta: { roles: ['OPERATOR', 'ENGINEER', 'SITE_OWNER'] },
        },
        {
          path: 'map',
          name: 'map',
          component: MapPage,
          meta: { roles: ['OPERATOR', 'ENGINEER', 'CAR_OWNER'] },
        },
        {
          path: 'piles',
          name: 'piles',
          component: PilesPage,
          meta: { roles: ['OPERATOR', 'ENGINEER'] },
        },
        {
          path: 'faults',
          name: 'faults',
          component: FaultsPage,
          meta: { roles: ['OPERATOR', 'ENGINEER'] },
        },
        {
          path: 'settlement',
          name: 'settlement',
          component: SettlementPage,
          meta: { roles: ['OPERATOR', 'SITE_OWNER'] },
        },
        {
          path: 'charging',
          name: 'charging',
          component: ChargingPage,
          meta: { roles: ['CAR_OWNER'] },
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrdersPage,
          meta: { roles: ['OPERATOR', 'CAR_OWNER'] },
        },
        {
          path: 'stations',
          name: 'stations',
          component: StationsPage,
          meta: { roles: ['OPERATOR'] },
        },
        {
          path: 'site-owner',
          name: 'site-owner',
          component: SiteOwnerPage,
          meta: { roles: ['SITE_OWNER'] },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.path === '/login' && userStore.isLoggedIn) {
    return next('/dashboard')
  }

  if (to.meta.roles && userStore.isLoggedIn) {
    const allowed = to.meta.roles as string[]
    if (!allowed.includes(userStore.role)) {
      const roleHome: Record<string, string> = {
        OPERATOR: '/dashboard',
        ENGINEER: '/dashboard',
        CAR_OWNER: '/charging',
        SITE_OWNER: '/site-owner',
      }
      return next(roleHome[userStore.role] || '/dashboard')
    }
  }

  next()
})

export default router
