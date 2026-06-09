<template>
  <div class="page-container">
    <h2 class="page-title">订单管理</h2>

    <div class="filter-bar">
      <el-select v-model="filter.status" placeholder="订单状态" clearable class="w-36">
        <el-option label="待确认" value="PENDING" />
        <el-option label="充电中" value="CHARGING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已中断" value="INTERRUPTED" />
        <el-option label="故障中断" value="FAULT_INTERRUPT" />
        <el-option label="退款中" value="REFUNDING" />
        <el-option label="已退款" value="REFUNDED" />
      </el-select>
      <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
      <el-button type="primary" @click="loadOrders">查询</el-button>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100">
      <el-table :data="filteredOrders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="充电量(kWh)" width="130">
          <template #default="{ row }">{{ row.chargedKwh.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="电费" width="100">
          <template #default="{ row }">¥{{ row.electricityFee.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="服务费" width="100">
          <template #default="{ row }">¥{{ row.serviceFee.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="总费用" width="110">
          <template #default="{ row }"><span class="font-semibold">¥{{ row.totalFee.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160">
          <template #default="{ row }">{{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="userStore.isCarOwner && ['COMPLETED','INTERRUPTED','FAULT_INTERRUPT'].includes(row.status)" type="warning" text size="small" @click="openRefundDialog(row)">退款</el-button>
            <el-button v-if="userStore.isAdmin && row.status === 'REFUNDING'" type="success" text size="small" @click="approveRefund(row)">批准</el-button>
            <el-button v-if="userStore.isAdmin && row.status === 'REFUNDING'" type="danger" text size="small" @click="rejectRefund(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" title="订单详情" size="480px">
      <template v-if="selectedOrder">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ selectedOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="orderStatusType(selectedOrder.status)" size="small">{{ orderStatusLabel(selectedOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="充电量">{{ selectedOrder.chargedKwh.toFixed(2) }} kWh</el-descriptions-item>
          <el-descriptions-item label="电费">¥{{ selectedOrder.electricityFee.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="服务费">¥{{ selectedOrder.serviceFee.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="总费用">¥{{ selectedOrder.totalFee.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ selectedOrder.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ selectedOrder.endTime || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedOrder.interruptReason" label="中断原因">{{ selectedOrder.interruptReason }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="440px">
      <el-form label-width="80px">
        <el-form-item label="退款金额">
          <el-input-number v-model="refundAmount" :min="0.01" :max="selectedOrder?.totalFee || 0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundReason" type="textarea" :rows="3" placeholder="请说明退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ChargingOrder, OrderStatus } from '@/types'
import { getOrders, requestRefund, approveRefund as approveRefundApi, rejectRefund as rejectRefundApi } from '@/api/order'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const filter = reactive({ status: '', dateRange: null as string[] | null })
const orders = ref<ChargingOrder[]>([])
const detailVisible = ref(false)
const refundDialogVisible = ref(false)
const selectedOrder = ref<ChargingOrder | null>(null)
const refundAmount = ref(0)
const refundReason = ref('')

const filteredOrders = computed(() => {
  let result = orders.value
  if (filter.status) result = result.filter((o) => o.status === filter.status)
  return result
})

const MOCK_ORDERS: ChargingOrder[] = [
  { id: 1, orderNo: 'ORD20260609001', userId: 3, pileId: 1, stationId: 1, status: 'CHARGING', startKwh: 0, endKwh: 0, chargedKwh: 23.5, electricityFee: 28.2, serviceFee: 4.7, totalFee: 32.9, startTime: '2026-06-09 10:30:00', createdAt: '2026-06-09' },
  { id: 2, orderNo: 'ORD20260609002', userId: 3, pileId: 2, stationId: 1, status: 'COMPLETED', startKwh: 0, endKwh: 45.2, chargedKwh: 45.2, electricityFee: 54.24, serviceFee: 9.04, totalFee: 63.28, startTime: '2026-06-09 09:15:00', endTime: '2026-06-09 11:20:00', createdAt: '2026-06-09' },
  { id: 3, orderNo: 'ORD20260609003', userId: 4, pileId: 5, stationId: 2, status: 'INTERRUPTED', startKwh: 0, endKwh: 10.3, chargedKwh: 10.3, electricityFee: 12.36, serviceFee: 2.06, totalFee: 14.42, startTime: '2026-06-09 08:00:00', endTime: '2026-06-09 08:45:00', interruptReason: '通信中断', createdAt: '2026-06-09' },
  { id: 4, orderNo: 'ORD20260608001', userId: 3, pileId: 3, stationId: 1, status: 'FAULT_INTERRUPT', startKwh: 0, endKwh: 5.1, chargedKwh: 5.1, electricityFee: 6.12, serviceFee: 1.02, totalFee: 7.14, startTime: '2026-06-08 14:00:00', endTime: '2026-06-08 14:20:00', interruptReason: '枪线故障', createdAt: '2026-06-08' },
  { id: 5, orderNo: 'ORD20260607001', userId: 3, pileId: 1, stationId: 1, status: 'REFUNDING', startKwh: 0, endKwh: 8.2, chargedKwh: 8.2, electricityFee: 9.84, serviceFee: 1.64, totalFee: 11.48, startTime: '2026-06-07 16:00:00', endTime: '2026-06-07 16:30:00', interruptReason: '充电中断', createdAt: '2026-06-07' },
]

function orderStatusLabel(s: OrderStatus) {
  const m: Record<string, string> = { PENDING: '待确认', HANDSHAKE: '握手中', CHARGING: '充电中', COMPLETED: '已完成', INTERRUPTED: '已中断', FAULT_INTERRUPT: '故障中断', REFUNDING: '退款中', REFUNDED: '已退款', OFFLINE_INTERRUPT: '离线中断' }
  return m[s] || s
}

function orderStatusType(s: OrderStatus) {
  const m: Record<string, string> = { PENDING: 'info', HANDSHAKE: 'info', CHARGING: '', COMPLETED: 'success', INTERRUPTED: 'warning', FAULT_INTERRUPT: 'danger', REFUNDING: 'warning', REFUNDED: 'info', OFFLINE_INTERRUPT: 'danger' }
  return m[s] || 'info'
}

function openDetail(order: ChargingOrder) {
  selectedOrder.value = order
  detailVisible.value = true
}

function openRefundDialog(order: ChargingOrder) {
  selectedOrder.value = order
  refundAmount.value = order.totalFee
  refundReason.value = ''
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!selectedOrder.value || !refundReason.value) return
  try {
    await requestRefund({ orderId: selectedOrder.value.id, reason: refundReason.value, amount: refundAmount.value })
    ElMessage.success('退款申请已提交')
    refundDialogVisible.value = false
    loadOrders()
  } catch {
    selectedOrder.value.status = 'REFUNDING'
    ElMessage.success('退款申请已提交(演示)')
    refundDialogVisible.value = false
  }
}

async function approveRefund(order: ChargingOrder) {
  try {
    await approveRefundApi(order.id)
    loadOrders()
  } catch {
    order.status = 'REFUNDED'
    ElMessage.success('退款已批准(演示)')
  }
}

async function rejectRefund(order: ChargingOrder) {
  try {
    await rejectRefundApi(order.id)
    loadOrders()
  } catch {
    order.status = 'COMPLETED'
    ElMessage.success('退款已拒绝(演示)')
  }
}

async function loadOrders() {
  try {
    const data = await getOrders({ status: filter.status })
    orders.value = Array.isArray(data?.records) ? data.records : MOCK_ORDERS
  } catch {
    orders.value = MOCK_ORDERS
  }
}

onMounted(() => loadOrders())
</script>
