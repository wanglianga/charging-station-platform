# 充电站管理平台

面向充电站运营方、运维工程师、车主和场地多方角色的综合管理系统。平台将充电下单、桩体握手、电量计费、故障中断、抢修派单、退款补偿和电费分摊连成完整闭环。

## 原始需求

> 搭建一个给充电站运营方、运维工程师、车主和场地方使用的充电站管理平台，Vue3 页面呈现站点地图、充电桩状态、故障派单和费用分摊，Spring Boot 保存充电流水、分表计量、故障记录和结算流水。运营方维护站点、桩位、功率、电价规则和场地方分成；车主发起充电、查看排队和申请退款；运维工程师处理枪线故障、模块过温、通信离线和急停恢复；场地方确认用电总表、物业分摊和停电通知。系统要把充电下单、桩体握手、电量计费、故障中断、抢修派单、退款补偿和电费分摊连成闭环。通信离线、充电中断、账单异常、场地方停电要分别影响订单和结算。
> 增加充电中断补偿。车辆正在充电时发生模块过温或急停，系统要记录已充电量、停机原因、车主等待时长和可切换桩位；车主选择继续充电、退款或换桩时，原订单和新订单要能合并结算。
> 增加场地方停电联动。物业计划停电时，场地方录入停电窗口，平台暂停对应时段预约和排队；已在充电车辆收到提前提醒，停电导致的未完成订单按实际电量结算。

## 技术栈

- **前端**: Vue3 + TypeScript + Vite + Tailwind CSS + Element Plus + ECharts + Leaflet
- **后端**: Spring Boot 3.x + MyBatis-Plus + Spring Security + JWT
- **数据库**: MySQL 8.x + Redis 7.x
- **部署**: Docker Compose (MySQL + Redis + Spring Boot + Nginx)

## 项目结构

```
├── frontend/                 # Vue3 前端项目
│   ├── src/
│   │   ├── api/             # API 请求层
│   │   ├── components/      # 公共组件
│   │   ├── composables/     # 组合式函数
│   │   ├── layout/          # 布局组件
│   │   ├── pages/           # 页面组件
│   │   ├── router/          # 路由配置
│   │   ├── stores/          # Pinia 状态管理
│   │   └── types/           # TypeScript 类型定义
│   ├── Dockerfile
│   ├── nginx.conf
│   └── .dockerignore
├── backend/                  # Spring Boot 后端项目
│   ├── src/main/java/com/charging/
│   │   ├── common/          # 公共工具类
│   │   ├── config/          # 配置类
│   │   ├── controller/      # REST 控制器
│   │   ├── dto/             # 数据传输对象
│   │   ├── entity/          # 实体类
│   │   ├── mapper/          # MyBatis-Plus Mapper
│   │   └── service/         # 业务逻辑层
│   ├── src/main/resources/
│   │   ├── db/migration/    # SQL 初始化脚本
│   │   └── application.yml  # 应用配置
│   ├── Dockerfile
│   └── .dockerignore
├── docker-compose.yml        # Docker Compose 编排
├── Dockerfile                # 根目录 Dockerfile (后端构建入口)
├── .dockerignore
└── README.md
```

## 核心功能

### 四大角色

| 角色 | 核心功能 |
|------|----------|
| 运营方 | 站点/桩位/电价/分成管理、仪表盘、订单审核、故障派单 |
| 运维工程师 | 故障处理（枪线/过温/离线/急停）、充电桩状态监控 |
| 车主 | 充电下单、排队查看、订单查询、退款申请 |
| 场地方 | 用电总表确认、物业分摊、停电通知、结算确认 |

### 业务闭环

充电下单 → 桩体握手 → 电量计费 → 充电完成/中断 → 故障工单 → 抢修派单 → 退款补偿 → 电费分摊

### 异常影响链

- **通信离线** → 订单标记异常，暂停计费
- **充电中断** → 自动生成中断订单记录
- **账单异常** → 结算标记争议状态
- **场地方停电** → 自动中断关联充电订单，暂停站点下单

## 启动方式

### 前置要求

- Docker & Docker Compose (一键启动)
- 或本地开发: Node.js 20+、Java 17+、MySQL 8.0、Redis 7

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

访问地址：http://localhost

### 本地开发

#### 1. 启动 MySQL 和 Redis

确保 MySQL 8.0 运行在 localhost:3306，Redis 运行在 localhost:6379。

执行数据库初始化：

```bash
mysql -u root -p < backend/src/main/resources/db/migration/init.sql
```

#### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：http://localhost:8080

API 文档：http://localhost:8080/swagger-ui.html

#### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：http://localhost:5173

## 演示账号

| 账号 | 密码 | 角色 | 可见功能 |
|------|------|------|---------|
| admin | 123456 | 运营方 | 全部功能 |
| engineer | 123456 | 运维工程师 | 仪表盘、地图、充电桩、故障 |
| owner | 123456 | 车主 | 站点地图、充电下单、订单管理 |
| siteowner | 123456 | 场地方 | 仪表盘、费用分摊、场地方门户 |

> 前端支持演示模式：后端未启动时自动回退 Mock 数据

## API 接口一览

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/auth/login | POST | 用户登录 |
| /api/stations | GET/POST | 站点列表/创建 |
| /api/stations/{id} | GET/PUT/DELETE | 站点详情/更新/删除 |
| /api/stations/{id}/piles | GET | 站点下充电桩 |
| /api/stations/{id}/pricing | GET/PUT | 电价规则 |
| /api/piles | GET | 充电桩列表 |
| /api/orders | GET/POST | 订单列表/创建 |
| /api/orders/{id}/start | POST | 开始充电 |
| /api/orders/{id}/stop | POST | 停止充电 |
| /api/fault-tickets | GET/POST | 故障工单列表/创建 |
| /api/fault-tickets/{id}/assign | PUT | 派单 |
| /api/fault-tickets/{id}/resolve | PUT | 解决故障 |
| /api/settlements | GET | 结算记录 |
| /api/settlements/{id}/confirm | PUT | 确认结算 |
| /api/meter-readings | GET/POST | 抄表记录 |
| /api/power-outages | GET/POST | 停电通知 |
| /api/refund-requests | POST | 退款申请 |

## 注意事项

- Docker 一键启动已包含 MySQL、Redis、后端和前端四个服务
- 前端 Nginx 配置了 `/api` 路径反向代理到后端
- 数据库密码为 `root123`，仅供开发演示使用
- 生产环境请修改 `application.yml` 中的 JWT 密钥和数据库密码
