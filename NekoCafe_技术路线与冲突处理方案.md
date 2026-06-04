# NekoCafé 智慧餐饮预约平台：技术路线与冲突处理方案

> 选题：T-01 NekoCafé 智慧餐饮预约平台  
> 技术方向：Web 前端 + Java 后端 + PostgreSQL + Redis  
> 用途：开题报告、概要设计说明书、详细设计说明书、开发分工与答辩说明参考

---

## 1. 技术路线总览

本项目建议采用 **前后端分离 + Java 单体后端 + PostgreSQL + Redis + Docker Compose** 的技术路线。

推荐整体架构如下：

```text
Web 前端
  ↓ HTTP / REST API
Spring Boot Java 后端
  ↓
PostgreSQL + Redis
  ↓
Docker Compose 一键部署
```

课程设计阶段不建议一开始采用微服务架构。微服务虽然看起来更高级，但会明显增加服务拆分、接口调用、部署、日志排查和数据一致性处理的复杂度。对于本项目，采用 **模块化单体架构** 更容易落地，也更适合学生团队开发。

---

## 2. 推荐技术栈

### 2.1 前端技术栈

推荐使用：

```text
Vue 3 + Vite + TypeScript + Vue Router + Pinia + Element Plus + ECharts
```

| 技术 | 用途 |
|---|---|
| Vue 3 | 构建 Web 用户端和后台管理端 |
| Vite | 前端工程构建工具，启动快、配置简单 |
| TypeScript | 提升代码可维护性和类型安全 |
| Vue Router | 前端路由管理 |
| Pinia | 前端状态管理 |
| Element Plus | 后台管理页面 UI 组件库 |
| ECharts | 数据看板图表展示 |

选择 Vue 3 的原因：

- 适合快速开发后台管理页面；
- Element Plus 表格、表单、弹窗等组件成熟；
- 文档和示例多，学习成本较低；
- 适合课程设计中快速完成用户端和管理端页面。

---

### 2.2 后端技术栈

推荐使用：

```text
Spring Boot 3 + Spring Web + Spring Security + JWT + MyBatis-Plus + PostgreSQL + Redis
```

| 技术 | 用途 |
|---|---|
| Spring Boot 3 | Java 后端主框架 |
| Spring Web | 提供 RESTful API |
| Spring Security | 用户认证与权限控制 |
| JWT | 登录状态保持和接口鉴权 |
| MyBatis-Plus | 数据库访问与 CRUD 简化 |
| PostgreSQL | 主业务数据库 |
| Redis | 缓存、验证码、幂等控制、临时锁 |
| Validation | 参数校验 |
| Knife4j / Swagger | 接口文档 |
| Lombok | 简化 Java 样板代码 |
| Docker Compose | 本地开发和最终部署 |

选择 Spring Boot 的原因：

- Java 生态成熟；
- REST API 开发方便；
- 和数据库、Redis、权限认证集成方便；
- 老师和评委通常更容易认可；
- 文档资料多，适合课程设计团队协作。

---

### 2.3 数据库与缓存

#### 主数据库

推荐使用：

```text
PostgreSQL 16
```

原因：

- 选题文档推荐 PostgreSQL；
- 支持较强的数据约束；
- 适合处理预约、订单、时间段等复杂业务；
- 对事务、索引、并发控制支持成熟。

如果团队更熟悉 MySQL，也可以替换为 MySQL，但本项目优先推荐 PostgreSQL。

#### 缓存数据库

推荐使用：

```text
Redis 7
```

Redis 主要用于：

- 缓存门店、菜品等高频访问数据；
- 存储验证码；
- 防止重复提交；
- 辅助实现订单超时处理；
- 缓存数据看板统计结果。

---

### 2.4 消息队列

消息队列可选，推荐预留 RabbitMQ。

```text
RabbitMQ，可选
```

可以用于：

- 订单创建后的异步通知；
- 支付超时释放桌位；
- 异步更新数据看板统计；
- 发送预约提醒或异常提醒。

但对于课程设计，第一阶段可以先不引入 RabbitMQ，使用 Spring 定时任务完成超时订单扫描即可。

推荐策略：

```text
基础版本：Spring @Scheduled 定时任务
进阶版本：RabbitMQ 延迟队列
```

---

## 3. 系统架构建议

### 3.1 架构类型

推荐采用：

```text
前后端分离 + 模块化单体后端
```

不要一开始做微服务。

原因：

- 项目规模适中，单体架构足够；
- 微服务会增加部署和联调难度；
- 课程设计更看重功能完整、流程闭环和文档规范；
- 模块化单体也可以体现良好的软件设计思想。

---

### 3.2 后端模块划分

后端可以按业务模块拆分 package：

```text
nekocafe-backend
├── auth             登录认证模块
├── user             用户与会员模块
├── store            门店模块
├── table            桌位模块
├── reservation      预约模块
├── menu             菜品模块
├── order            订单模块
├── payment          支付沙箱模块
├── cat              猫咪档案模块
├── recommend        推荐模块
├── dashboard        数据看板模块
└── admin            后台管理模块
```

每个业务模块内部建议采用常见分层：

```text
controller  接口层
service     业务层
mapper      数据访问层
entity      数据库实体
dto         请求和响应对象
vo          前端展示对象
```

示例包结构：

```text
com.nekocafe
├── NekocafeApplication.java
├── common
│   ├── result
│   ├── exception
│   ├── config
│   └── utils
├── auth
│   ├── controller
│   ├── service
│   └── dto
├── user
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   └── dto
├── store
├── table
├── reservation
├── order
├── payment
├── cat
├── recommend
└── dashboard
```

---

## 4. 前端页面规划

### 4.1 顾客端页面

顾客端 Web 页面可以先实现 H5 / Web，不急于开发小程序。

建议页面：

```text
首页
门店列表页
门店详情页
桌位预约页
菜品点单页
猫咪档案展示页
推荐结果页
我的预约
我的订单
会员中心
登录 / 注册页
```

### 4.2 后台管理端页面

后台管理端主要服务店员、店长、猫咪管家、总部运营等角色。

建议页面：

```text
后台登录页
工作台首页
预约管理
订单管理
桌位管理
菜品管理
猫咪档案管理
会员管理
数据看板
活动管理，可选
```

### 4.3 数据看板页面

使用 ECharts 展示：

- 预约量趋势；
- 订单量趋势；
- 营业额统计；
- 翻台率统计；
- 会员复购率；
- 猫咪互动热度榜；
- 门店人气排行；
- 菜品销量排行。

---

## 5. 数据库核心表设计建议

### 5.1 用户与权限

```text
user
role
user_role
member_account
```

### 5.2 门店与桌位

```text
store
dining_table
reservation_slot
```

### 5.3 预约与订单

```text
reservation
food_order
food_order_item
payment_record
refund_record
```

### 5.4 菜品

```text
dish_category
dish
```

### 5.5 猫咪特色功能

```text
cat_profile
cat_health_record
cat_interaction_record
```

### 5.6 推荐功能

```text
user_preference
recommend_record
```

### 5.7 运营活动

```text
coupon
activity
dashboard_stat，可选
```

---

## 6. 预约与订单冲突处理方案

本项目中最需要重点处理的冲突包括：

```text
1. 同一桌位同一时间被多人预约
2. 同一个订单重复提交
3. 菜品库存不足导致超卖
4. 支付接口重复调用
5. 多个店员同时修改同一个订单状态
6. 预约取消和支付完成同时发生
```

---

## 7. 桌位预约冲突处理

### 7.1 推荐设计：固定预约时段

不建议一开始支持任意时间范围预约，例如：

```text
13:17 - 14:43
```

这样冲突判断较复杂。

推荐设计为固定时间段：

```text
10:00 - 11:30
11:30 - 13:00
13:00 - 14:30
14:30 - 16:00
16:00 - 17:30
18:00 - 19:30
19:30 - 21:00
```

或者每 30 分钟一个 slot。

---

### 7.2 reservation_slot 表方案

可以设计预约时段表：

```text
reservation_slot
```

核心字段：

```text
id
store_id
table_id
reservation_date
start_time
end_time
status
```

slot 状态建议：

```text
AVAILABLE   可预约
LOCKED      锁定中
RESERVED    已预约
USING       使用中
FINISHED    已完成
UNAVAILABLE 不可用
```

用户预约时，不是直接操作桌位，而是选择某个 `slot_id`。

---

### 7.3 条件更新防止重复预约

预约时使用数据库事务和条件更新：

```sql
UPDATE reservation_slot
SET status = 'LOCKED'
WHERE id = ? AND status = 'AVAILABLE';
```

判断影响行数：

```text
影响行数 = 1：说明抢占成功，可以创建预约单
影响行数 = 0：说明该时段已被别人预约或锁定，返回预约失败
```

推荐预约流程：

```text
用户选择门店、桌位、时段
  ↓
前端提交预约请求
  ↓
后端开启事务
  ↓
检查用户状态
  ↓
检查桌位时段是否 AVAILABLE
  ↓
条件更新 slot 状态为 LOCKED
  ↓
创建预约单，状态为待支付
  ↓
提交事务
  ↓
返回预约单
```

该方案简单、清晰，适合课程设计实现和答辩说明。

---

## 8. 订单重复提交处理

用户可能连续点击两次“提交预约”或“提交订单”。

### 8.1 前端处理

前端在按钮点击后立即禁用按钮：

```text
提交中...
```

接口返回后再恢复按钮状态。

### 8.2 后端幂等处理

后端应检查用户是否已经存在相同业务含义的订单。

例如预约时判断：

```text
用户 ID + 桌位 ID + 时段 ID + 待支付 / 已预约状态
```

如果已经存在，则直接返回已有预约单，而不是重复创建。

推荐逻辑：

```text
用户提交预约
  ↓
查询该用户在该桌位该时段是否已有待支付或已预约记录
  ↓
如果有，直接返回已有订单
  ↓
如果没有，再创建新订单
```

---

## 9. 菜品库存冲突处理

如果菜品存在库存，例如每日限定甜品，需要防止超卖。

错误做法：

```text
先查询库存
再扣减库存
```

在并发情况下，两个请求可能都查到库存充足，导致超卖。

推荐使用条件更新：

```sql
UPDATE dish
SET stock = stock - ?
WHERE id = ? AND stock >= ?;
```

判断影响行数：

```text
影响行数 = 1：扣减成功
影响行数 = 0：库存不足
```

---

## 10. 订单状态机设计

订单状态不能由前端随意修改，必须由后端按照状态机规则流转。

### 10.1 预约订单状态

建议状态：

```text
PENDING_PAYMENT  待支付
RESERVED         已预约
CHECKED_IN       已到店
DINING           用餐中
COMPLETED        已完成
CANCELLED        已取消
REFUNDING        退款中
REFUNDED         已退款
```

预约订单状态流转：

```text
待支付 → 已预约
待支付 → 已取消
已预约 → 已到店
已预约 → 已取消
已到店 → 用餐中
用餐中 → 已完成
已取消 → 终态，不可再变
已完成 → 终态，不可再变
```

### 10.2 菜品订单状态

建议状态：

```text
PENDING_PAYMENT  待支付
PAID             已支付
WAITING_ACCEPT   待接单
PREPARING        制作中
READY            待上菜
COMPLETED        已完成
CANCELLED        已取消
REFUNDED         已退款
```

### 10.3 后端状态流转方法

不建议直接暴露通用状态修改接口。

不推荐：

```text
POST /orders/{id}/status
```

推荐：

```text
payReservation()
cancelReservation()
checkInReservation()
startDining()
completeReservation()
acceptFoodOrder()
serveFoodOrder()
```

每个方法内部检查当前状态是否允许变更。

---

## 11. 多人同时修改订单处理

例如两个店员同时点击“接单”，需要防止重复处理。

推荐使用乐观锁。

订单表增加字段：

```text
version
```

更新时带上 version 和当前状态：

```sql
UPDATE food_order
SET status = 'PREPARING',
    version = version + 1
WHERE id = ?
  AND version = ?
  AND status = 'WAITING_ACCEPT';
```

判断影响行数：

```text
影响行数 = 1：修改成功
影响行数 = 0：订单状态已被其他人修改，提示刷新后重试
```

MyBatis-Plus 支持乐观锁插件，可以简化实现。

---

## 12. 支付沙箱与支付冲突处理

课程设计中建议做支付沙箱，不接入真实支付。

### 12.1 模拟支付流程

```text
创建预约单
  ↓
订单状态：待支付
  ↓
用户点击模拟支付
  ↓
后端检查订单状态
  ↓
更新为已预约 / 已支付
  ↓
创建支付流水
  ↓
返回支付成功
```

### 12.2 支付接口幂等

用户可能重复点击支付按钮，后端应保证支付接口幂等。

推荐逻辑：

```text
如果订单是待支付：
    执行支付成功逻辑
如果订单已经支付：
    直接返回支付成功
如果订单已取消：
    返回订单已取消
```

支付流水表可以添加唯一约束：

```text
payment_no 唯一
order_no 唯一或按业务规则唯一
```

---

## 13. 超时未支付处理

用户创建预约后，如果 15 分钟内未支付，需要自动取消预约并释放桌位。

### 13.1 简单方案：定时任务

课程设计阶段优先推荐 Spring 定时任务：

```text
每 1 分钟扫描一次待支付订单
找到创建时间超过 15 分钟且状态为待支付的预约
取消订单
释放 reservation_slot
```

流程：

```text
定时任务扫描待支付订单
  ↓
找到超过 15 分钟未支付的订单
  ↓
后端开启事务
  ↓
订单状态改为已取消
  ↓
slot 状态改回 AVAILABLE
  ↓
提交事务
```

### 13.2 进阶方案：RabbitMQ 延迟队列

如果时间充足，可以使用 RabbitMQ 延迟队列：

```text
创建订单
  ↓
发送 15 分钟延迟消息
  ↓
15 分钟后消费消息
  ↓
检查订单是否支付
  ↓
未支付则取消订单并释放桌位
```

建议先实现定时任务，后续再升级为消息队列。

---

## 14. Redis 使用建议

### 14.1 门店和菜品缓存

适合缓存：

```text
门店列表
门店详情
菜品列表
桌位状态摘要
```

这些数据读取频繁、修改相对较少。

### 14.2 验证码缓存

如果做短信验证码沙箱，可以用 Redis 存储验证码：

```text
key: sms:login:手机号
value: 验证码
ttl: 5 分钟
```

### 14.3 防重复提交

可以使用 Redis 保存短时间幂等 key，例如：

```text
submit:reservation:{userId}:{slotId}
```

设置较短过期时间，防止用户短时间内重复提交。

### 14.4 数据看板缓存

数据看板统计查询可能较慢，可以缓存统计结果：

```text
dashboard:store:{storeId}:today
dashboard:global:today
```

---

## 15. 推荐接口设计

### 15.1 顾客端接口

```text
POST /api/auth/login
POST /api/auth/register
GET  /api/stores
GET  /api/stores/{id}
GET  /api/stores/{id}/slots
GET  /api/stores/{id}/dishes
POST /api/reservations
POST /api/reservations/{id}/pay
POST /api/reservations/{id}/cancel
GET  /api/users/me/reservations
GET  /api/users/me/orders
GET  /api/cats
GET  /api/recommendations
```

### 15.2 店员端接口

```text
GET  /api/staff/reservations/today
POST /api/staff/reservations/{id}/check-in
POST /api/staff/reservations/{id}/start-dining
POST /api/staff/reservations/{id}/complete
GET  /api/staff/orders
POST /api/staff/orders/{id}/accept
POST /api/staff/orders/{id}/prepare
POST /api/staff/orders/{id}/serve
```

### 15.3 后台管理接口

```text
GET  /api/admin/stores
POST /api/admin/stores
PUT  /api/admin/stores/{id}
GET  /api/admin/tables
POST /api/admin/tables
PUT  /api/admin/tables/{id}
GET  /api/admin/dishes
POST /api/admin/dishes
PUT  /api/admin/dishes/{id}
GET  /api/admin/cats
POST /api/admin/cats
PUT  /api/admin/cats/{id}
GET  /api/admin/dashboard
```

---

## 16. 开发阶段路线

建议按阶段推进，先打通核心闭环，再做特色功能。

### 第一阶段：基础框架

目标：项目能启动，前后端能连通。

```text
前端项目初始化
后端 Spring Boot 项目初始化
数据库 Docker Compose
统一返回格式
统一异常处理
Swagger / Knife4j 接口文档
登录注册
JWT 鉴权
```

注意：JWT token 不要输出到日志，也不要写进代码注释。

---

### 第二阶段：基础业务管理

目标：后台能维护基础数据。

```text
门店管理
桌位管理
菜品管理
猫咪档案管理
```

---

### 第三阶段：顾客预约闭环

目标：完成项目最核心主线。

```text
门店列表
门店详情
桌位时段查询
创建预约
模拟支付
我的预约
取消预约
超时未支付自动取消
```

---

### 第四阶段：店员履约流程

目标：后台能处理顾客到店和订单履约。

```text
今日预约
顾客签到
安排入座
订单接单
订单状态流转
异常提示
```

---

### 第五阶段：点单和订单

目标：完成餐饮订单流程。

```text
菜单展示
购物车
提交点单
库存扣减
订单状态流转
支付沙箱
```

---

### 第六阶段：特色功能

目标：体现猫咪主题差异化。

```text
猫咪推荐
猫咪健康档案
猫咪互动记录
猫咪热度榜
推荐理由展示
```

---

### 第七阶段：数据看板和部署

目标：完成验收和答辩展示。

```text
预约量统计
订单量统计
营业额统计
翻台率统计
会员复购率统计
Docker Compose 一键启动
测试用例
压测脚本
```

---

## 17. Docker 与云数据库建议

推荐采用组合方案：

```text
开发协作：云数据库作为共享测试库
本地开发：Docker PostgreSQL + Redis
最终交付：Docker Compose 一键启动
```

不要让系统强依赖云数据库。云数据库适合团队联调，但最终交付时应保证老师可以在本地通过 Docker Compose 启动完整系统。

建议环境划分：

| 环境 | 数据库 | 用途 |
|---|---|---|
| local | Docker PostgreSQL | 每个人本地开发 |
| dev | 云 PostgreSQL | 团队联调 |
| demo | Docker 或云数据库 | 答辩演示备用 |
| final | Docker PostgreSQL | 老师验收，一键启动 |

---

## 18. 项目技术方案表述

可写入开题报告或概要设计说明书：

> 本项目采用前后端分离架构。前端使用 Vue 3、Vite、TypeScript 和 Element Plus 构建 Web 用户端与后台管理端；后端使用 Spring Boot 3 构建 RESTful API 服务，结合 Spring Security 与 JWT 实现用户认证和权限控制，使用 MyBatis-Plus 访问 PostgreSQL 数据库。系统使用 Redis 缓存门店、菜品和验证码等高频数据，并预留 RabbitMQ 用于订单超时、通知和统计等异步任务。部署方面使用 Docker Compose 编排前端、后端、PostgreSQL 和 Redis，实现一键启动。对于预约和订单并发冲突，系统通过数据库事务、条件更新、唯一约束、乐观锁和订单状态机保证数据一致性。

---

## 19. 最终推荐方案

本项目最终推荐技术路线：

```text
前端：Vue 3 + Vite + TypeScript + Element Plus + ECharts
后端：Spring Boot 3 + Spring Security + JWT + MyBatis-Plus
数据库：PostgreSQL 16
缓存：Redis 7
消息队列：RabbitMQ，可选
部署：Docker Compose
接口文档：Knife4j / Swagger
```

核心冲突处理策略：

```text
预约冲突：reservation_slot 状态 + 条件更新 + 数据库事务
重复提交：前端禁用按钮 + 后端幂等检查
库存冲突：stock >= count 条件扣减
订单状态冲突：状态机 + 乐观锁 version
支付重复调用：支付接口幂等
超时未支付：Spring 定时任务释放桌位，后期可升级 RabbitMQ 延迟队列
```

该方案难度适中、开发效率高、文档表达清晰，既能完成课程设计要求，也能在答辩时体现一定的工程深度。
