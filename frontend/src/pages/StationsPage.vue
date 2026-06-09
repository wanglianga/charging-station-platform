<template>
  <div class="page-container">
    <h2 class="page-title">站点管理</h2>

    <div class="filter-bar">
      <el-button type="primary" @click="openForm()">
        <Plus class="w-4 h-4 mr-1" />新增站点
      </el-button>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100">
      <el-table :data="stationList" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="站点名称" width="160" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="桩位数" width="100">
          <template #default="{ row }">{{ row.totalPiles }}个</template>
        </el-table-column>
        <el-table-column label="空闲数" width="100">
          <template #default="{ row }"><span :class="row.availablePiles > 0 ? 'text-green-600' : 'text-red-500'">{{ row.availablePiles }}个</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="stationStatusType(row.status)" size="small">{{ stationStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分成比例" width="100">
          <template #default="{ row }">{{ row.commissionRate }}%</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openForm(row)">编辑</el-button>
            <el-button text size="small" @click="managePiles(row)">桩位</el-button>
            <el-popconfirm title="确认删除该站点？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="editingStation ? '编辑站点' : '新增站点'" width="720px" destroy-on-close>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="basicFormRef" :model="stationForm" :rules="basicRules" label-width="90px">
            <el-form-item label="站点名称" prop="name">
              <el-input v-model="stationForm.name" placeholder="请输入站点名称" />
            </el-form-item>
            <el-form-item label="站点地址" prop="address">
              <el-input v-model="stationForm.address" placeholder="请输入地址" />
            </el-form-item>
            <div class="grid grid-cols-2 gap-4">
              <el-form-item label="经度" prop="longitude">
                <el-input-number v-model="stationForm.longitude" :precision="6" :controls="false" class="w-full" />
              </el-form-item>
              <el-form-item label="纬度" prop="latitude">
                <el-input-number v-model="stationForm.latitude" :precision="6" :controls="false" class="w-full" />
              </el-form-item>
            </div>
            <el-form-item label="站点状态">
              <el-select v-model="stationForm.status" class="w-full">
                <el-option label="运营中" value="ACTIVE" />
                <el-option label="已停用" value="INACTIVE" />
                <el-option label="维护中" value="MAINTENANCE" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="桩位配置" name="piles">
          <el-button type="primary" size="small" class="mb-3" @click="addPileRow">
            <Plus class="w-3 h-3 mr-1" />添加桩位
          </el-button>
          <el-table :data="pileList" border size="small">
            <el-table-column label="桩号" width="120">
              <template #default="{ row }"><el-input v-model="row.pileCode" size="small" placeholder="如P-001" /></template>
            </el-table-column>
            <el-table-column label="功率(kW)" width="120">
              <template #default="{ row }"><el-input-number v-model="row.power" size="small" :min="1" :controls="false" class="w-full" /></template>
            </el-table-column>
            <el-table-column label="类型" width="140">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option label="直流快充" value="DC_FAST" />
                  <el-option label="直流慢充" value="DC_SLOW" />
                  <el-option label="交流慢充" value="AC_SLOW" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ $index }">
                <el-button type="danger" text size="small" @click="pileList.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="电价规则" name="pricing">
          <el-form label-width="100px">
            <div class="grid grid-cols-3 gap-4">
              <el-form-item label="峰时电价"><el-input-number v-model="pricingForm.peakPrice" :precision="4" :min="0" :controls="false" class="w-full" /></el-form-item>
              <el-form-item label="平时电价"><el-input-number v-model="pricingForm.flatPrice" :precision="4" :min="0" :controls="false" class="w-full" /></el-form-item>
              <el-form-item label="谷时电价"><el-input-number v-model="pricingForm.valleyPrice" :precision="4" :min="0" :controls="false" class="w-full" /></el-form-item>
            </div>
            <el-form-item label="峰时时段"><el-input v-model="pricingForm.peakHours" placeholder="如 08:00-11:00,18:00-21:00" /></el-form-item>
            <el-form-item label="平时时段"><el-input v-model="pricingForm.flatHours" /></el-form-item>
            <el-form-item label="谷时时段"><el-input v-model="pricingForm.valleyHours" /></el-form-item>
            <el-form-item label="服务费率(%)"><el-input-number v-model="pricingForm.serviceFeeRate" :precision="2" :min="0" :max="100" :controls="false" class="w-48" /></el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="分成设置" name="commission">
          <el-form label-width="120px">
            <el-form-item label="场地方分成(%)">
              <el-input-number v-model="stationForm.commissionRate" :min="0" :max="100" :precision="2" :controls="false" class="w-48" />
            </el-form-item>
            <el-form-item label="物业分摊(%)">
              <el-input-number v-model="stationForm.propertyShareRate" :min="0" :max="100" :precision="2" :controls="false" class="w-48" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Station, StationStatus, Pile, PricingRule } from '@/types'
import { getStations, createStation, updateStation, deleteStation } from '@/api/station'

const stationList = ref<Station[]>([])
const formVisible = ref(false)
const activeTab = ref('basic')
const editingStation = ref<Station | null>(null)
const basicFormRef = ref<FormInstance>()

const stationForm = reactive<Partial<Station>>({
  name: '', address: '', longitude: 116.4, latitude: 39.9, status: 'ACTIVE', commissionRate: 15, propertyShareRate: 8,
})

const pileList = ref<Partial<Pile>[]>([])
const pricingForm = reactive<Partial<PricingRule>>({
  peakPrice: 1.2, flatPrice: 0.8, valleyPrice: 0.4,
  peakHours: '08:00-11:00,18:00-21:00', flatHours: '07:00-08:00,11:00-18:00', valleyHours: '21:00-07:00',
  serviceFeeRate: 10,
})

const basicRules: FormRules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入站点地址', trigger: 'blur' }],
}

const MOCK_STATIONS: Station[] = [
  { id: 1, name: '朝阳万达站', address: '朝阳区建国路93号', longitude: 116.474, latitude: 39.908, totalPiles: 8, availablePiles: 3, status: 'ACTIVE', siteOwnerName: '万达物业', commissionRate: 15, propertyShareRate: 8, createdAt: '2026-01-01' },
  { id: 2, name: '海淀中关村站', address: '海淀区中关村大街27号', longitude: 116.316, latitude: 39.982, totalPiles: 12, availablePiles: 5, status: 'ACTIVE', siteOwnerName: '中关村物业', commissionRate: 12, propertyShareRate: 6, createdAt: '2026-01-15' },
  { id: 3, name: '西城金融街站', address: '西城区金融大街甲15号', longitude: 116.356, latitude: 39.913, totalPiles: 6, availablePiles: 0, status: 'MAINTENANCE', siteOwnerName: '金融街物业', commissionRate: 18, propertyShareRate: 10, createdAt: '2026-02-01' },
]

function stationStatusLabel(s: StationStatus) {
  const m: Record<string, string> = { ACTIVE: '运营中', INACTIVE: '已停用', MAINTENANCE: '维护中' }
  return m[s] || s
}
function stationStatusType(s: StationStatus) {
  const m: Record<string, string> = { ACTIVE: 'success', INACTIVE: 'info', MAINTENANCE: 'warning' }
  return m[s] || 'info'
}

function openForm(station?: Station) {
  editingStation.value = station || null
  activeTab.value = 'basic'
  if (station) {
    Object.assign(stationForm, { ...station })
  } else {
    Object.assign(stationForm, { name: '', address: '', longitude: 116.4, latitude: 39.9, status: 'ACTIVE', commissionRate: 15, propertyShareRate: 8 })
  }
  pileList.value = []
  formVisible.value = true
}

function managePiles(station: Station) {
  openForm(station)
  activeTab.value = 'piles'
}

function addPileRow() {
  pileList.value.push({ pileCode: '', power: 60, type: 'DC_FAST' })
}

async function submitForm() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid) { activeTab.value = 'basic'; return }

  try {
    if (editingStation.value) {
      await updateStation(editingStation.value.id, stationForm)
    } else {
      await createStation(stationForm)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadStations()
  } catch {
    if (editingStation.value) {
      const idx = stationList.value.findIndex((s) => s.id === editingStation.value!.id)
      if (idx >= 0) Object.assign(stationList.value[idx], stationForm)
    } else {
      stationList.value.push({ ...stationForm, id: Date.now(), totalPiles: pileList.value.length, availablePiles: pileList.value.length, createdAt: new Date().toISOString() } as Station)
    }
    ElMessage.success('保存成功(演示)')
    formVisible.value = false
  }
}

async function handleDelete(station: Station) {
  try {
    await deleteStation(station.id)
    loadStations()
  } catch {
    stationList.value = stationList.value.filter((s) => s.id !== station.id)
    ElMessage.success('删除成功(演示)')
  }
}

async function loadStations() {
  try {
    const data = await getStations()
    stationList.value = Array.isArray(data) && data.length > 0 ? data : MOCK_STATIONS
  } catch {
    stationList.value = MOCK_STATIONS
  }
}

onMounted(() => loadStations())
</script>
