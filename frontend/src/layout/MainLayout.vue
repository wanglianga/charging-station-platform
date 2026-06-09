<template>
  <div class="flex h-screen overflow-hidden">
    <aside class="w-60 flex-shrink-0 flex flex-col" style="background-color: var(--color-sidebar-bg)">
      <div class="h-16 flex items-center px-5 border-b border-white/10">
        <Zap class="w-7 h-7 text-amber-400 mr-2" />
        <span class="text-white font-bold text-lg tracking-wide">充电站管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu flex-1 overflow-y-auto"
        background-color="transparent"
        text-color="var(--color-sidebar-text)"
        active-text-color="#FFFFFF"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in visibleNavItems" :key="item.key" :index="item.path">
          <component :is="item.icon" class="w-5 h-5 mr-3" />
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="px-5 py-4 border-t border-white/10">
        <div class="text-xs text-teal-300/60">v1.0.0</div>
      </div>
    </aside>

    <div class="flex-1 flex flex-col overflow-hidden">
      <header class="h-16 flex items-center justify-between px-6 bg-white border-b border-gray-200 flex-shrink-0">
        <div class="flex items-center gap-2 text-gray-500">
          <component :is="currentNavItem?.icon" class="w-5 h-5" />
          <span class="font-medium text-gray-700">{{ currentNavItem?.label }}</span>
        </div>
        <div class="flex items-center gap-4">
          <el-tag :type="roleTagType" size="small" effect="plain">{{ roleLabel }}</el-tag>
          <span class="text-sm text-gray-600">{{ userStore.name || userStore.username }}</span>
          <el-button text @click="handleLogout">
            <LogOut class="w-4 h-4 mr-1" />
            退出
          </el-button>
        </div>
      </header>

      <main class="flex-1 overflow-auto" style="background-color: var(--color-body-bg)">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  LayoutDashboard,
  Map,
  Plug,
  AlertTriangle,
  Receipt,
  BatteryCharging,
  ClipboardList,
  Building2,
  Landmark,
  LogOut,
  Zap,
} from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { canAccess } = usePermission()

const navItems = [
  { key: 'dashboard', label: '仪表盘', path: '/dashboard', icon: LayoutDashboard },
  { key: 'map', label: '站点地图', path: '/map', icon: Map },
  { key: 'piles', label: '充电桩状态', path: '/piles', icon: Plug },
  { key: 'faults', label: '故障派单', path: '/faults', icon: AlertTriangle },
  { key: 'settlement', label: '费用分摊', path: '/settlement', icon: Receipt },
  { key: 'charging', label: '充电下单', path: '/charging', icon: BatteryCharging },
  { key: 'orders', label: '订单管理', path: '/orders', icon: ClipboardList },
  { key: 'stations', label: '站点管理', path: '/stations', icon: Building2 },
  { key: 'site-owner', label: '场地方门户', path: '/site-owner', icon: Landmark },
]

const visibleNavItems = computed(() => {
  const check = canAccess.value
  return navItems.filter((item) => check(item.key))
})

const activeMenu = computed(() => route.path)

const currentNavItem = computed(() =>
  navItems.find((item) => route.path.startsWith(item.path)),
)

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    OPERATOR: '运营方',
    ENGINEER: '运维工程师',
    CAR_OWNER: '车主',
    SITE_OWNER: '场地方',
  }
  return map[userStore.role] || userStore.role
})

const roleTagType = computed(() => {
  const map: Record<string, string> = {
    OPERATOR: '',
    ENGINEER: 'warning',
    CAR_OWNER: 'success',
    SITE_OWNER: 'info',
  }
  return map[userStore.role] || 'info'
})

function handleMenuSelect(path: string) {
  router.push(path)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>
