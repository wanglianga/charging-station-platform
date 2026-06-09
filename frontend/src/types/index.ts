export type Role = 'OPERATOR' | 'ENGINEER' | 'CAR_OWNER' | 'SITE_OWNER'

export interface User {
  id: number
  username: string
  name: string
  phone?: string
  role: Role
  createdAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: Pick<User, 'id' | 'username' | 'role' | 'name'>
}

export type StationStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'

export interface Station {
  id: number
  name: string
  address: string
  longitude: number
  latitude: number
  totalPiles: number
  availablePiles: number
  status: StationStatus
  siteOwnerId?: number
  siteOwnerName?: string
  commissionRate: number
  propertyShareRate?: number
  createdAt: string
}

export type PileType = 'DC_FAST' | 'DC_SLOW' | 'AC_SLOW'
export type PileStatus = 'IDLE' | 'CHARGING' | 'FAULT' | 'OFFLINE' | 'EMERGENCY_STOP'

export interface Pile {
  id: number
  stationId: number
  pileCode: string
  power: number
  type: PileType
  status: PileStatus
  currentOrder?: ChargingOrder
  createdAt: string
}

export interface PricingRule {
  id: number
  stationId: number
  peakPrice: number
  flatPrice: number
  valleyPrice: number
  peakHours: string
  flatHours: string
  valleyHours: string
  serviceFeeRate: number
}

export type OrderStatus =
  | 'PENDING'
  | 'HANDSHAKE'
  | 'CHARGING'
  | 'COMPLETED'
  | 'INTERRUPTED'
  | 'FAULT_INTERRUPT'
  | 'REFUNDING'
  | 'REFUNDED'
  | 'OFFLINE_INTERRUPT'

export interface ChargingOrder {
  id: number
  orderNo: string
  userId: number
  pileId: number
  stationId: number
  status: OrderStatus
  startKwh: number
  endKwh: number
  chargedKwh: number
  electricityFee: number
  serviceFee: number
  totalFee: number
  startTime?: string
  endTime?: string
  interruptReason?: string
  createdAt: string
}

export type FaultType = 'GUN_LINE_FAULT' | 'MODULE_OVER_TEMP' | 'COMM_OFFLINE' | 'EMERGENCY_STOP'
export type FaultSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type FaultStatus = 'PENDING' | 'ASSIGNED' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

export interface FaultTicket {
  id: number
  stationId: number
  pileId: number
  faultType: FaultType
  severity: FaultSeverity
  status: FaultStatus
  description: string
  assignedEngineerId?: number
  assignedEngineerName?: string
  createdAt: string
  assignedAt?: string
  resolvedAt?: string
  affectedOrderIds: number[]
}

export type SettlementStatus = 'PENDING' | 'CONFIRMED' | 'DISPUTED' | 'SETTLED'

export interface SettlementRecord {
  id: number
  orderId: number
  stationId: number
  totalElectricityFee: number
  totalServiceFee: number
  operatorShare: number
  siteOwnerShare: number
  propertyShare: number
  status: SettlementStatus
  period: string
  createdAt: string
  confirmedAt?: string
}

export type MeterReadingType = 'TOTAL' | 'PROPERTY'

export interface MeterReading {
  id: number
  stationId: number
  readingType: MeterReadingType
  kwh: number
  readingDate: string
  confirmedBy?: number
  confirmedAt?: string
}

export type OutageStatus = 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'

export interface PowerOutageNotice {
  id: number
  stationId: number
  siteOwnerId: number
  startTime: string
  endTime: string
  reason: string
  status: OutageStatus
  affectedOrderIds: number[]
}

export type RefundStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED'

export interface RefundRequest {
  id: number
  orderId: number
  userId: number
  amount: number
  reason: string
  status: RefundStatus
  processedBy?: number
  createdAt: string
  processedAt?: string
}
