<template>
  <div class="page-container">
    <h2 class="page-title">故障派单</h2>

    <div class="filter-bar">
      <el-select v-model="filter.status" placeholder="工单状态" clearable class="w-36">
        <el-option label="待处理" value="PENDING" />
        <el-option label="已派单" value="ASSIGNED" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已恢复" value="RESOLVED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-select v-model="filter.faultType" placeholder="故障类型" clearable class="w-40">
        <el-option label="枪线故障" value="GUN_LINE_FAULT" />
        <el-option label="模块过温" value="MODULE_OVER_TEMP" />
        <el-option label="通信离线" value="COMM_OFFLINE" />
        <el-option label="急停触发" value="EMERGENCY_STOP" />
      </el-select>
      <el-button type="primary" @click="loadFaults">查询</el-button>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100">
      <el-table :data="filteredTickets" stripe>
        <el-table-column prop="id" label="工单号" width="80" />
        <el-table-column label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="severityTagType(row.severity)" size="small" effect="dark">{{ severityLabel(row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="故障类型" width="120">
          <template #default="{ row }">{{ faultTypeLabel(row.faultType) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="faultStatusType(row.status)" size="small">{{ faultStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedEngineerName" label="处理人" width="100">
          <template #default="{ row }">{{ row.assignedEngineerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="primary" text size="small" @click="openAssignDialog(row)">派单</el-button>
            <el-button v-if="row.status === 'ASSIGNED'" type="warning" text size="small" @click="changeStatus(row, 'PROCESSING')">开始处理</el-button>
            <el-button v-if="row.status === 'PROCESSING'" type="success" text size="small" @click="changeStatus(row, 'RESOLVED')">已恢复</el-button>
            <el-button v-if="row.status === 'RESOLVED'" type="info" text size="small" @click="changeStatus(row, 'CLOSED')">关闭</el-button>
            <el-button text size="small" @click="openTimeline(row)">跟踪</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="assignDialogVisible" title="派单给工程师" width="440px">
      <el-form label-width="80px">
        <el-form-item label="工程师">
          <el-select v-model="assignEngineerId" placeholder="选择工程师" class="w-full">
            <el-option v-for="e in engineers" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="timelineVisible" title="工单处理进度" width="500px">
      <el-timeline v-if="selectedTicket">
        <el-timeline-item :timestamp="selectedTicket.createdAt" placement="top" type="warning">
          创建故障工单
        </el-timeline-item>
        <el-timeline-item v-if="selectedTicket.assignedAt" :timestamp="selectedTicket.assignedAt" placement="top" type="primary">
          派单给 {{ selectedTicket.assignedEngineerName }}
        </el-timeline-item>
        <el-timeline-item v-if="['PROCESSING','RESOLVED','CLOSED'].includes(selectedTicket.status)" timestamp="处理中" placement="top" type="primary">
          工程师开始处理
        </el-timeline-item>
        <el-timeline-item v-if="['RESOLVED','CLOSED'].includes(selectedTicket.status)" :timestamp="selectedTicket.resolvedAt || ''" placement="top" type="success">
          故障已恢复
        </el-timeline-item>
        <el-timeline-item v-if="selectedTicket.status === 'CLOSED'" timestamp="已关闭" placement="top" type="info">
          工单关闭
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FaultTicket, FaultSeverity, FaultStatus, FaultType } from '@/types'
import { getFaultTickets, assignEngineer, updateFaultStatus } from '@/api/fault'

const filter = reactive({ status: '', faultType: '' })
const tickets = ref<FaultTicket[]>([])
const assignDialogVisible = ref(false)
const timelineVisible = ref(false)
const selectedTicket = ref<FaultTicket | null>(null)
const assignEngineerId = ref<number>()

const engineers = [
  { id: 2, name: '张工程师' },
  { id: 5, name: '刘工程师' },
  { id: 6, name: '陈工程师' },
]

const MOCK_TICKETS: FaultTicket[] = [
  { id: 1, stationId: 1, pileId: 3, faultType: 'GUN_LINE_FAULT', severity: 'CRITICAL', status: 'PENDING', description: '3号桩枪线故障，无法充电', affectedOrderIds: [], createdAt: '2026-06-09 10:00' },
  { id: 2, stationId: 2, pileId: 6, faultType: 'MODULE_OVER_TEMP', severity: 'HIGH', status: 'ASSIGNED', description: '6号桩模块过温告警', assignedEngineerId: 2, assignedEngineerName: '张工程师', affectedOrderIds: [], createdAt: '2026-06-09 09:30', assignedAt: '2026-06-09 09:45' },
  { id: 3, stationId: 3, pileId: 9, faultType: 'COMM_OFFLINE', severity: 'MEDIUM', status: 'PROCESSING', description: '9号桩通信离线', assignedEngineerId: 5, assignedEngineerName: '刘工程师', affectedOrderIds: [], createdAt: '2026-06-09 08:45', assignedAt: '2026-06-09 09:00' },
  { id: 4, stationId: 1, pileId: 8, faultType: 'EMERGENCY_STOP', severity: 'CRITICAL', status: 'RESOLVED', description: '8号桩急停触发', assignedEngineerId: 2, assignedEngineerName: '张工程师', affectedOrderIds: [], createdAt: '2026-06-08 16:00', assignedAt: '2026-06-08 16:10', resolvedAt: '2026-06-08 17:30' },
  { id: 5, stationId: 4, pileId: 12, faultType: 'GUN_LINE_FAULT', severity: 'LOW', status: 'CLOSED', description: '12号桩枪线接触不良', assignedEngineerId: 6, assignedEngineerName: '陈工程师', affectedOrderIds: [], createdAt: '2026-06-07 14:00', assignedAt: '2026-06-07 14:20', resolvedAt: '2026-06-07 15:00' },
]

const filteredTickets = computed(() => {
  let result = tickets.value
  if (filter.status) result = result.filter((t) => t.status === filter.status)
  if (filter.faultType) result = result.filter((t) => t.faultType === filter.faultType)
  return result
})

function severityLabel(s: FaultSeverity) {
  const m: Record<string, string> = { CRITICAL: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return m[s] || s
}
function severityTagType(s: FaultSeverity) {
  const m: Record<string, string> = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return m[s] || 'info'
}
function faultTypeLabel(t: FaultType) {
  const m: Record<string, string> = { GUN_LINE_FAULT: '枪线故障', MODULE_OVER_TEMP: '模块过温', COMM_OFFLINE: '通信离线', EMERGENCY_STOP: '急停触发' }
  return m[t] || t
}
function faultStatusLabel(s: FaultStatus) {
  const m: Record<string, string> = { PENDING: '待处理', ASSIGNED: '已派单', PROCESSING: '处理中', RESOLVED: '已恢复', CLOSED: '已关闭' }
  return m[s] || s
}
function faultStatusType(s: FaultStatus) {
  const m: Record<string, string> = { PENDING: 'danger', ASSIGNED: 'warning', PROCESSING: '', RESOLVED: 'success', CLOSED: 'info' }
  return m[s] || 'info'
}

function openAssignDialog(ticket: FaultTicket) {
  selectedTicket.value = ticket
  assignEngineerId.value = undefined
  assignDialogVisible.value = true
}

function openTimeline(ticket: FaultTicket) {
  selectedTicket.value = ticket
  timelineVisible.value = true
}

async function confirmAssign() {
  if (!assignEngineerId.value || !selectedTicket.value) return
  try {
    await assignEngineer(selectedTicket.value.id, assignEngineerId.value)
    ElMessage.success('派单成功')
    assignDialogVisible.value = false
    loadFaults()
  } catch {
    const ticket = tickets.value.find((t) => t.id === selectedTicket.value!.id)
    if (ticket) {
      ticket.status = 'ASSIGNED'
      ticket.assignedEngineerId = assignEngineerId.value
      ticket.assignedEngineerName = engineers.find((e) => e.id === assignEngineerId.value)?.name
      ticket.assignedAt = new Date().toLocaleString()
    }
    ElMessage.success('派单成功(演示)')
    assignDialogVisible.value = false
  }
}

async function changeStatus(ticket: FaultTicket, status: FaultStatus) {
  try {
    await updateFaultStatus(ticket.id, status)
    loadFaults()
  } catch {
    ticket.status = status
    if (status === 'RESOLVED') ticket.resolvedAt = new Date().toLocaleString()
    ElMessage.success('状态已更新(演示)')
  }
}

async function loadFaults() {
  try {
    const data = await getFaultTickets({ status: filter.status, faultType: filter.faultType })
    tickets.value = Array.isArray(data?.records) ? data.records : MOCK_TICKETS
  } catch {
    tickets.value = MOCK_TICKETS
  }
}

onMounted(() => loadFaults())
</script>
