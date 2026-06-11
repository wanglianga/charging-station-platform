CREATE DATABASE IF NOT EXISTS charging_station DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE charging_station;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(256) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `phone` VARCHAR(20),
  `role` ENUM('OPERATOR','ENGINEER','CAR_OWNER','SITE_OWNER') NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `station` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `address` VARCHAR(256) NOT NULL,
  `longitude` DECIMAL(10,7) NOT NULL,
  `latitude` DECIMAL(10,7) NOT NULL,
  `status` ENUM('ACTIVE','INACTIVE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
  `site_owner_id` BIGINT,
  `commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '场地方分成比例(%)',
  `property_share_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '物业分摊比例(%)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_site_owner` (`site_owner_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `station_id` BIGINT NOT NULL,
  `pile_code` VARCHAR(32) NOT NULL,
  `power` DECIMAL(8,2) NOT NULL COMMENT '额定功率(kW)',
  `type` ENUM('DC_FAST','DC_SLOW','AC_SLOW') NOT NULL,
  `status` ENUM('IDLE','CHARGING','FAULT','OFFLINE','EMERGENCY_STOP') NOT NULL DEFAULT 'IDLE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pile_code` (`pile_code`),
  KEY `idx_station` (`station_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pricing_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `station_id` BIGINT NOT NULL,
  `peak_price` DECIMAL(8,4) NOT NULL COMMENT '峰时电价(元/kWh)',
  `flat_price` DECIMAL(8,4) NOT NULL COMMENT '平时电价(元/kWh)',
  `valley_price` DECIMAL(8,4) NOT NULL COMMENT '谷时电价(元/kWh)',
  `peak_hours` VARCHAR(128) NOT NULL COMMENT '峰时时段',
  `flat_hours` VARCHAR(128) NOT NULL COMMENT '平时时段',
  `valley_hours` VARCHAR(128) NOT NULL COMMENT '谷时时段',
  `service_fee_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '服务费率(%)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_station` (`station_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `charging_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `pile_id` BIGINT NOT NULL,
  `station_id` BIGINT NOT NULL,
  `status` ENUM('PENDING','HANDSHAKE','CHARGING','COMPLETED','INTERRUPTED','FAULT_INTERRUPT','REFUNDING','REFUNDED','OFFLINE_INTERRUPT') NOT NULL DEFAULT 'PENDING',
  `start_kwh` DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
  `end_kwh` DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
  `charged_kwh` DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
  `electricity_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `service_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `total_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `start_time` DATETIME,
  `end_time` DATETIME,
  `interrupt_reason` VARCHAR(256),
  `linked_order_id` BIGINT COMMENT '关联/合并订单ID(换桩时关联原订单)',
  `outage_notice_id` BIGINT COMMENT '关联停电通知ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_pile` (`pile_id`),
  KEY `idx_station` (`station_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `fault_ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `station_id` BIGINT NOT NULL,
  `pile_id` BIGINT NOT NULL,
  `fault_type` ENUM('GUN_LINE_FAULT','MODULE_OVER_TEMP','COMM_OFFLINE','EMERGENCY_STOP') NOT NULL,
  `severity` ENUM('CRITICAL','HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
  `status` ENUM('PENDING','ASSIGNED','PROCESSING','RESOLVED','CLOSED') NOT NULL DEFAULT 'PENDING',
  `description` TEXT,
  `assigned_engineer_id` BIGINT,
  `priority_score` DECIMAL(10,4) COMMENT '优先级评分',
  `estimated_restore_time` DATETIME COMMENT '预计恢复时间',
  `fault_pile_count` INT DEFAULT 0 COMMENT '故障桩数',
  `waiting_car_owners` INT DEFAULT 0 COMMENT '排队车主数',
  `station_revenue` DECIMAL(12,2) COMMENT '站点月收入',
  `site_owner_request` VARCHAR(32) COMMENT '场地方要求(URGENT/HIGH/NORMAL)',
  `parts_distance` DECIMAL(8,2) COMMENT '备件距离(km)',
  `recommended_engineer_id` BIGINT COMMENT '推荐工程师ID',
  `recommended_route` VARCHAR(512) COMMENT '推荐路线说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `assigned_at` DATETIME,
  `resolved_at` DATETIME,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_station` (`station_id`),
  KEY `idx_pile` (`pile_id`),
  KEY `idx_engineer` (`assigned_engineer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority_score`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `settlement_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `station_id` BIGINT NOT NULL,
  `total_electricity_fee` DECIMAL(10,2) NOT NULL,
  `total_service_fee` DECIMAL(10,2) NOT NULL,
  `operator_share` DECIMAL(10,2) NOT NULL,
  `site_owner_share` DECIMAL(10,2) NOT NULL,
  `property_share` DECIMAL(10,2) NOT NULL,
  `status` ENUM('PENDING','CONFIRMED','DISPUTED','SETTLED') NOT NULL DEFAULT 'PENDING',
  `period` VARCHAR(20) NOT NULL COMMENT '结算周期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_at` DATETIME,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_station` (`station_id`),
  KEY `idx_period` (`period`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `meter_reading` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `station_id` BIGINT NOT NULL,
  `reading_type` ENUM('TOTAL','PROPERTY') NOT NULL COMMENT 'TOTAL=用电总表,PROPERTY=物业分表',
  `kwh` DECIMAL(12,4) NOT NULL,
  `reading_date` DATE NOT NULL,
  `confirmed_by` BIGINT,
  `confirmed_at` DATETIME,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_station_date` (`station_id`, `reading_date`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `power_outage_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `station_id` BIGINT NOT NULL,
  `site_owner_id` BIGINT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('SCHEDULED','ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  `affected_order_count` INT NOT NULL DEFAULT 0 COMMENT '受影响订单数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_station` (`station_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `refund_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('PENDING','APPROVED','REJECTED','COMPLETED') NOT NULL DEFAULT 'PENDING',
  `processed_by` BIGINT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` DATETIME,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `interruption_compensation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `station_id` BIGINT NOT NULL,
  `pile_id` BIGINT NOT NULL,
  `charged_kwh` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '中断时已充电量',
  `stop_reason` VARCHAR(256) NOT NULL COMMENT '停机原因:MODULE_OVER_TEMP/EMERGENCY_STOP',
  `waiting_minutes` INT NOT NULL DEFAULT 0 COMMENT '车主等待时长(分钟)',
  `switchable_piles` TEXT COMMENT '可切换桩位ID列表,逗号分隔',
  `decision` ENUM('PENDING','CONTINUE','REFUND','SWITCH') NOT NULL DEFAULT 'PENDING' COMMENT '车主选择',
  `switch_target_pile_id` BIGINT COMMENT '换桩目标桩位ID',
  `switch_order_id` BIGINT COMMENT '换桩后新订单ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `decided_at` DATETIME,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`decision`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `meter_reconciliation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `station_id` BIGINT NOT NULL COMMENT '站点ID',
  `period` VARCHAR(20) NOT NULL COMMENT '对账周期(yyyy-MM)',
  `start_date` DATE NOT NULL COMMENT '对账开始日期',
  `end_date` DATE NOT NULL COMMENT '对账结束日期',
  `total_meter_kwh` DECIMAL(12,4) NOT NULL COMMENT '总表读数(kWh)',
  `sub_meter_total_kwh` DECIMAL(12,4) COMMENT '分表合计电量(kWh)',
  `order_total_kwh` DECIMAL(12,4) COMMENT '订单电量合计(kWh)',
  `difference_kwh` DECIMAL(12,4) COMMENT '差异电量(kWh)',
  `difference_rate` DECIMAL(8,6) COMMENT '差异率',
  `status` ENUM('NORMAL','PENDING_REVIEW','CONFIRMED','DISPUTED') NOT NULL DEFAULT 'NORMAL' COMMENT '对账状态',
  `review_note` TEXT COMMENT '复核备注',
  `loss_reason` VARCHAR(512) COMMENT '损耗原因(线路损耗/离线补录/人工抄表误差)',
  `reviewed_by` BIGINT COMMENT '复核人ID',
  `reviewed_at` DATETIME COMMENT '复核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_station_period` (`station_id`, `period`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB;

-- Demo data: Users
-- Passwords are BCrypt encoded for '123456'
INSERT INTO `user` (`username`, `password`, `name`, `phone`, `role`) VALUES
('operator1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运营管理员-张伟', '13800000001', 'OPERATOR'),
('operator2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运营管理员-李芳', '13800000002', 'OPERATOR'),
('engineer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运维工程师-王强', '13800000003', 'ENGINEER'),
('engineer2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运维工程师-赵磊', '13800000004', 'ENGINEER'),
('carowner1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '车主-刘洋', '13800000005', 'CAR_OWNER'),
('carowner2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '车主-陈明', '13800000006', 'CAR_OWNER'),
('carowner3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '车主-孙丽', '13800000007', 'CAR_OWNER'),
('siteowner1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '场地方-周建国', '13800000008', 'SITE_OWNER'),
('siteowner2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '场地方-吴海燕', '13800000009', 'SITE_OWNER');

-- Demo data: Stations
INSERT INTO `station` (`name`, `address`, `longitude`, `latitude`, `status`, `site_owner_id`, `commission_rate`, `property_share_rate`) VALUES
('朝阳大悦城充电站', '北京市朝阳区朝阳北路101号', 116.4742850, 39.9204670, 'ACTIVE', 8, 25.00, 10.00),
('海淀中关村充电站', '北京市海淀区中关村大街27号', 116.3170940, 39.9829600, 'ACTIVE', 8, 20.00, 8.00),
('丰台科技园充电站', '北京市丰台区丰台科技园南区', 116.2912500, 39.8185400, 'ACTIVE', 9, 22.00, 12.00);

-- Demo data: Piles (4 per station = 12 total)
INSERT INTO `pile` (`station_id`, `pile_code`, `power`, `type`, `status`) VALUES
(1, 'CY-DC-001', 120.00, 'DC_FAST', 'IDLE'),
(1, 'CY-DC-002', 60.00, 'DC_SLOW', 'IDLE'),
(1, 'CY-AC-001', 7.00, 'AC_SLOW', 'CHARGING'),
(1, 'CY-DC-003', 120.00, 'DC_FAST', 'FAULT'),
(2, 'ZGC-DC-001', 120.00, 'DC_FAST', 'IDLE'),
(2, 'ZGC-DC-002', 60.00, 'DC_SLOW', 'IDLE'),
(2, 'ZGC-AC-001', 7.00, 'AC_SLOW', 'IDLE'),
(2, 'ZGC-DC-003', 120.00, 'DC_FAST', 'OFFLINE'),
(3, 'FT-DC-001', 120.00, 'DC_FAST', 'IDLE'),
(3, 'FT-DC-002', 60.00, 'DC_SLOW', 'CHARGING'),
(3, 'FT-AC-001', 7.00, 'AC_SLOW', 'IDLE'),
(3, 'FT-DC-003', 120.00, 'DC_FAST', 'IDLE');

-- Demo data: Pricing Rules
INSERT INTO `pricing_rule` (`station_id`, `peak_price`, `flat_price`, `valley_price`, `peak_hours`, `flat_hours`, `valley_hours`, `service_fee_rate`) VALUES
(1, 1.2000, 0.8000, 0.4000, '08:00-11:00,18:00-21:00', '07:00-08:00,11:00-18:00', '21:00-07:00', 15.00),
(2, 1.1000, 0.7500, 0.3500, '08:00-11:00,18:00-21:00', '07:00-08:00,11:00-18:00', '21:00-07:00', 12.00),
(3, 1.1500, 0.7800, 0.3800, '08:00-11:00,18:00-21:00', '07:00-08:00,11:00-18:00', '21:00-07:00', 13.00);

-- Demo data: Charging Orders
INSERT INTO `charging_order` (`order_no`, `user_id`, `pile_id`, `station_id`, `status`, `start_kwh`, `end_kwh`, `charged_kwh`, `electricity_fee`, `service_fee`, `total_fee`, `start_time`, `end_time`, `interrupt_reason`, `created_at`) VALUES
('ORD20260601001', 5, 3, 1, 'COMPLETED', 1000.0000, 1035.5000, 35.5000, 28.40, 4.26, 32.66, '2026-06-01 09:30:00', '2026-06-01 10:15:00', NULL, '2026-06-01 09:30:00'),
('ORD20260602001', 6, 10, 3, 'CHARGING', 5000.0000, 0.0000, 0.0000, 0.00, 0.00, 0.00, '2026-06-02 14:00:00', NULL, NULL, '2026-06-02 14:00:00'),
('ORD20260603001', 7, 4, 1, 'INTERRUPTED', 2000.0000, 2015.3000, 15.3000, 12.24, 1.84, 14.08, '2026-06-03 11:00:00', '2026-06-03 11:45:00', '桩体通信异常中断', '2026-06-03 11:00:00'),
('ORD20260604001', 5, 5, 2, 'COMPLETED', 3000.0000, 3060.0000, 60.0000, 45.00, 5.40, 50.40, '2026-06-04 08:00:00', '2026-06-04 09:30:00', NULL, '2026-06-04 08:00:00'),
('ORD20260605001', 6, 1, 1, 'COMPLETED', 1050.0000, 1080.0000, 30.0000, 24.00, 3.60, 27.60, '2026-06-05 19:00:00', '2026-06-05 19:30:00', NULL, '2026-06-05 19:00:00');

-- Demo data: Fault Tickets
INSERT INTO `fault_ticket` (`station_id`, `pile_id`, `fault_type`, `severity`, `status`, `description`, `assigned_engineer_id`, `created_at`, `assigned_at`, `resolved_at`) VALUES
(1, 4, 'GUN_LINE_FAULT', 'HIGH', 'ASSIGNED', '1号充电桩枪线接触不良，充电时频繁断连', 3, '2026-06-03 11:45:00', '2026-06-03 12:00:00', NULL),
(2, 8, 'COMM_OFFLINE', 'CRITICAL', 'PENDING', '充电桩通信离线超过2小时，无法远程监控', NULL, '2026-06-04 16:00:00', NULL, NULL),
(1, 4, 'MODULE_OVER_TEMP', 'MEDIUM', 'RESOLVED', '充电过程中模块温度超过85度', 3, '2026-05-28 10:00:00', '2026-05-28 10:30:00', '2026-05-28 14:00:00');

-- Demo data: Settlement Records
INSERT INTO `settlement_record` (`order_id`, `station_id`, `total_electricity_fee`, `total_service_fee`, `operator_share`, `site_owner_share`, `property_share`, `status`, `period`, `created_at`, `confirmed_at`) VALUES
(1, 1, 28.40, 4.26, 21.20, 8.17, 3.29, 'CONFIRMED', '2026-06', '2026-06-01 10:20:00', '2026-06-02 09:00:00'),
(4, 2, 45.00, 5.40, 37.51, 10.08, 4.81, 'PENDING', '2026-06', '2026-06-04 09:35:00', NULL),
(5, 1, 24.00, 3.60, 17.82, 6.90, 2.88, 'CONFIRMED', '2026-06', '2026-06-05 19:35:00', '2026-06-06 10:00:00');

-- Demo data: Meter Readings
INSERT INTO `meter_reading` (`station_id`, `reading_type`, `kwh`, `reading_date`, `confirmed_by`, `confirmed_at`) VALUES
(1, 'TOTAL', 52800.5000, '2026-05-31', 8, '2026-06-01 10:00:00'),
(1, 'PROPERTY', 12600.2000, '2026-05-31', 8, '2026-06-01 10:05:00'),
(2, 'TOTAL', 41200.8000, '2026-05-31', 8, '2026-06-01 10:10:00'),
(3, 'TOTAL', 35600.3000, '2026-05-31', NULL, NULL);

-- Demo data: Power Outage Notices
INSERT INTO `power_outage_notice` (`station_id`, `site_owner_id`, `start_time`, `end_time`, `reason`, `status`, `created_at`) VALUES
(1, 8, '2026-06-10 02:00:00', '2026-06-10 06:00:00', '配电室年度检修维护', 'SCHEDULED', '2026-06-05 14:00:00'),
(3, 9, '2026-06-15 22:00:00', '2026-06-16 06:00:00', '供电线路改造升级', 'SCHEDULED', '2026-06-06 09:00:00');

-- Demo data: Refund Requests
INSERT INTO `refund_request` (`order_id`, `user_id`, `amount`, `reason`, `status`, `processed_by`, `created_at`, `processed_at`) VALUES
(3, 7, 14.08, '充电中断导致未充满，要求全额退款', 'PENDING', NULL, '2026-06-03 12:00:00', NULL);

INSERT INTO `interruption_compensation` (`order_id`, `user_id`, `station_id`, `pile_id`, `charged_kwh`, `stop_reason`, `waiting_minutes`, `switchable_piles`, `decision`, `created_at`) VALUES
(3, 7, 1, 4, 15.3000, 'MODULE_OVER_TEMP', 25, '1,2', 'PENDING', '2026-06-03 11:45:00');

-- Demo data: Meter Reconciliations
INSERT INTO `meter_reconciliation` (`station_id`, `period`, `start_date`, `end_date`, `total_meter_kwh`, `sub_meter_total_kwh`, `order_total_kwh`, `difference_kwh`, `difference_rate`, `status`, `review_note`, `loss_reason`, `reviewed_by`, `reviewed_at`, `created_at`) VALUES
(1, '2026-05', '2026-05-01', '2026-05-31', 40200.0000, 40080.5000, 40000.3000, 199.7000, 0.00497, 'NORMAL', NULL, NULL, NULL, NULL, '2026-06-01 10:30:00'),
(2, '2026-05', '2026-05-01', '2026-05-31', 35600.0000, 33500.8000, 33450.2000, 2149.8000, 0.06039, 'PENDING_REVIEW', NULL, NULL, NULL, NULL, '2026-06-01 11:00:00'),
(3, '2026-05', '2026-05-01', '2026-05-31', 28500.0000, 28420.5000, 28400.8000, 99.2000, 0.00348, 'CONFIRMED', '差异在合理范围内，确认通过', '正常线路损耗约0.35%', 1, '2026-06-02 09:30:00', '2026-06-01 11:30:00');

-- Update Demo data: Fault Tickets with dispatch info
UPDATE `fault_ticket` SET `priority_score` = 68.5000, `estimated_restore_time` = '2026-06-03 13:30:00', `fault_pile_count` = 1, `waiting_car_owners` = 3, `station_revenue` = 8560.50, `site_owner_request` = 'HIGH', `parts_distance` = 3.50, `recommended_engineer_id` = 3, `recommended_route` = '工程师[王强]预计15分钟后到达朝阳大悦城充电站，总修复时间约60分钟' WHERE `id` = 1;
UPDATE `fault_ticket` SET `priority_score` = 82.3000, `estimated_restore_time` = NULL, `fault_pile_count` = 1, `waiting_car_owners` = 5, `station_revenue` = 12350.80, `site_owner_request` = 'URGENT', `parts_distance` = 8.20, `recommended_engineer_id` = NULL, `recommended_route` = NULL WHERE `id` = 2;
