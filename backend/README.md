# 充电站管理平台

充电站管理平台是一个面向充电站运营方、运维工程师、车主和场地多方角色的综合管理系统。平台将充电下单、桩体握手、电量计费、故障中断、抢修派单、退款补偿和电费分摊连成完整闭环。

## 原始需求

> Create a complete Spring Boot 3.x backend project at d:\code\solocode-wl\wl-278\backend for a charging station management platform.
>
> The project must include:
>
> 1. Maven pom.xml with these dependencies:
>    - Spring Boot 3.2.x (spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-redis, spring-boot-starter-validation)
>    - MyBatis-Plus 3.5.x (mybatis-plus-spring-boot3-starter)
>    - MySQL Connector (mysql-connector-j)
>    - JWT (java-jwt 4.4.x)
>    - Lombok
>    - SpringDoc OpenAPI (springdoc-openapi-starter-webmvc-ui 2.3.x)
>
> 2. Application main class: com.charging.ChargingStationApplication
>
> 3. Package structure: com.charging.{config,controller,service,service.impl,mapper,entity,dto,common}
>
> 4. Entity classes for all 9 tables (user, station, pile, pricing_rule, charging_order, fault_ticket, settlement_record, meter_reading, power_outage_notice, refund_request) with proper MyBatis-Plus annotations
>
> 5. Mapper interfaces extending BaseMapper for each entity
>
> 6. Service interfaces and implementations for:
>    - AuthService (login, JWT generation)
>    - StationService (CRUD, listing)
>    - PileService (CRUD, status update)
>    - OrderService (create order, start charging, complete charging, interrupt, refund)
>    - FaultService (create ticket, assign, resolve, close)
>    - SettlementService (calculate settlement, confirm)
>    - SiteOwnerService (meter reading, outage notice)
>
> 7. REST Controllers for each service with proper request mapping
>
> 8. DTO classes for request/response objects
>
> 9. Common classes: Result<T> wrapper class, JwtUtil, SecurityConfig (permit /api/auth/**, authenticate everything else with JWT filter)
>
> 10. application.yml with MySQL, Redis, MyBatis-Plus, JWT config
>
> 11. SQL migration file with all CREATE TABLE statements and INSERT statements for demo data
>
> 12. A CORS configuration allowing localhost:5173

## 技术栈

- **后端**: Spring Boot 3.2.5 + Spring Security + MyBatis-Plus 3.5.6 + JWT (java-jwt 4.4.0)
- **数据库**: MySQL 8.0 + Redis 7
- **API 文档**: SpringDoc OpenAPI 2.3.0 (Swagger UI)
- **构建工具**: Maven
- **Java 版本**: 17

## 项目结构

```
backend/
├── pom.xml
├── Dockerfile
├── .dockerignore
└── src/main/
    ├── java/com/charging/
    │   ├── ChargingStationApplication.java
    │   ├── common/
    │   │   ├── Result.java
    │   │   ├── JwtUtil.java
    │   │   └── JwtAuthenticationFilter.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── CorsConfig.java
    │   │   └── MyBatisPlusConfig.java
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Station.java
    │   │   ├── Pile.java
    │   │   ├── PricingRule.java
    │   │   ├── ChargingOrder.java
    │   │   ├── FaultTicket.java
    │   │   ├── SettlementRecord.java
    │   │   ├── MeterReading.java
    │   │   ├── PowerOutageNotice.java
    │   │   └── RefundRequest.java
    │   ├── mapper/
    │   │   ├── UserMapper.java
    │   │   ├── StationMapper.java
    │   │   ├── PileMapper.java
    │   │   ├── PricingRuleMapper.java
    │   │   ├── ChargingOrderMapper.java
    │   │   ├── FaultTicketMapper.java
    │   │   ├── SettlementRecordMapper.java
    │   │   ├── MeterReadingMapper.java
    │   │   ├── PowerOutageNoticeMapper.java
    │   │   └── RefundRequestMapper.java
    │   ├── dto/
    │   │   ├── LoginRequest.java
    │   │   ├── LoginResponse.java
    │   │   ├── StationRequest.java
    │   │   ├── PileRequest.java
    │   │   ├── PileStatusUpdateRequest.java
    │   │   ├── OrderCreateRequest.java
    │   │   ├── OrderCompleteRequest.java
    │   │   ├── OrderInterruptRequest.java
    │   │   ├── FaultTicketRequest.java
    │   │   ├── FaultAssignRequest.java
    │   │   ├── FaultResolveRequest.java
    │   │   ├── SettlementConfirmRequest.java
    │   │   ├── MeterReadingRequest.java
    │   │   ├── OutageNoticeRequest.java
    │   │   └── RefundRequestDTO.java
    │   ├── service/
    │   │   ├── AuthService.java
    │   │   ├── StationService.java
    │   │   ├── PileService.java
    │   │   ├── OrderService.java
    │   │   ├── FaultService.java
    │   │   ├── SettlementService.java
    │   │   └── SiteOwnerService.java
    │   ├── service/impl/
    │   │   ├── AuthServiceImpl.java
    │   │   ├── StationServiceImpl.java
    │   │   ├── PileServiceImpl.java
    │   │   ├── OrderServiceImpl.java
    │   │   ├── FaultServiceImpl.java
    │   │   ├── SettlementServiceImpl.java
    │   │   └── SiteOwnerServiceImpl.java
    │   └── controller/
    │       ├── AuthController.java
    │       ├── StationController.java
    │       ├── PileController.java
    │       ├── OrderController.java
    │       ├── FaultController.java
    │       ├── SettlementController.java
    │       └── SiteOwnerController.java
    └── resources/
        ├── application.yml
        └── db/migration/init.sql
```

## API 接口一览

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | /api/auth/login | 用户登录 |
| 站点 | GET | /api/stations | 站点列表 |
| 站点 | GET | /api/stations/{id} | 站点详情 |
| 站点 | POST | /api/stations | 创建站点 |
| 站点 | PUT | /api/stations/{id} | 更新站点 |
| 站点 | DELETE | /api/stations/{id} | 删除站点 |
| 充电桩 | GET | /api/piles | 充电桩列表 |
| 充电桩 | GET | /api/piles/stations/{stationId} | 站点下充电桩 |
| 充电桩 | GET | /api/piles/{id} | 充电桩详情 |
| 充电桩 | POST | /api/piles | 创建充电桩 |
| 充电桩 | PUT | /api/piles/{id}/status | 更新充电桩状态 |
| 订单 | GET | /api/orders | 订单列表 |
| 订单 | GET | /api/orders/{id} | 订单详情 |
| 订单 | POST | /api/orders | 创建订单 |
| 订单 | POST | /api/orders/{id}/start | 开始充电 |
| 订单 | POST | /api/orders/{id}/complete | 完成充电 |
| 订单 | POST | /api/orders/{id}/interrupt | 中断充电 |
| 订单 | POST | /api/orders/refund | 申请退款 |
| 故障 | GET | /api/faults | 故障工单列表 |
| 故障 | POST | /api/faults | 创建故障工单 |
| 故障 | PUT | /api/faults/{id}/assign | 派单 |
| 故障 | PUT | /api/faults/{id}/resolve | 处理故障 |
| 故障 | PUT | /api/faults/{id}/close | 关闭工单 |
| 结算 | GET | /api/settlements | 结算列表 |
| 结算 | POST | /api/settlements | 计算结算 |
| 结算 | PUT | /api/settlements/{id}/confirm | 确认结算 |
| 场地方 | GET | /api/meter-readings | 抄表记录列表 |
| 场地方 | POST | /api/meter-readings | 创建抄表记录 |
| 场地方 | POST | /api/meter-readings/{id}/confirm | 确认抄表 |
| 场地方 | GET | /api/outage-notices | 停电通知列表 |
| 场地方 | POST | /api/outage-notices | 创建停电通知 |

## 启动方式

### 前置要求

- Docker & Docker Compose（推荐方式）
- 或本地开发：JDK 17+, Maven 3.9+, MySQL 8.0, Redis 7

### Docker 一键启动（推荐）

```bash
docker compose up --build
```

后台运行：

```bash
docker compose up --build -d
```

停止服务：

```bash
docker compose down
```

访问地址：
- 后端 API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- MySQL: localhost:3306 (用户名: root, 密码: root123)
- Redis: localhost:6379

### 本地开发启动

#### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/db/migration/init.sql
```

#### 2. 安装依赖

```bash
cd backend
mvn install -DskipTests
```

#### 3. 启动服务

```bash
cd backend
mvn spring-boot:run
```

访问地址：
- 后端 API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 环境变量说明

| 变量名 | 含义 | 必填 | 默认值 |
|--------|------|------|--------|
| SPRING_DATASOURCE_URL | MySQL 连接地址 | 否 | jdbc:mysql://localhost:3306/charging_station |
| SPRING_DATASOURCE_USERNAME | MySQL 用户名 | 否 | root |
| SPRING_DATASOURCE_PASSWORD | MySQL 密码 | 否 | root123 |
| SPRING_DATA_REDIS_HOST | Redis 主机 | 否 | localhost |
| SPRING_DATA_REDIS_PORT | Redis 端口 | 否 | 6379 |

## 演示账号

所有账号密码均为 `123456`（BCrypt 加密存储）。

| 账号 | 角色 | 姓名 |
|------|------|------|
| operator1 | 运营方 | 张伟 |
| operator2 | 运营方 | 李芳 |
| engineer1 | 运维工程师 | 王强 |
| engineer2 | 运维工程师 | 赵磊 |
| carowner1 | 车主 | 刘洋 |
| carowner2 | 车主 | 陈明 |
| carowner3 | 车主 | 孙丽 |
| siteowner1 | 场地方 | 周建国 |
| siteowner2 | 场地方 | 吴海燕 |
