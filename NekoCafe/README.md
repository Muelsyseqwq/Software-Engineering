# NekoCafé 智慧餐饮预约平台

> T-01 课程设计源码工程。系统面向猫咪主题餐厅，支持顾客预约用餐、提前点单、店员签到与订单履约、会员积分、猫咪档案维护和运营数据看板。

## 技术栈

- 后端：Spring Boot 3、Spring Security、JWT、MyBatis-Plus、Flyway、MySQL 8
- 前端：Vue 3、Vite、TypeScript、Vue Router、Pinia、Element Plus、ECharts
- 数据库：远程 MySQL 8，开发环境通过环境变量连接
- 可选缓存：Redis 7

## 前置依赖

- JDK 17+
- Maven 3.9+
- Node.js 20+
- npm 10+ 或兼容包管理器
- 可访问的远程 MySQL 8 数据库

## 配置环境变量

复制 `.env.example` 为 `.env`，填入自己的远程 MySQL 配置。不要把 `.env` 提交到 Git。

> 注意：Spring Boot 和 Vite 不会自动读取仓库根目录 `.env`。使用 Makefile 启动时会自动加载根目录 `.env`；如果手动启动，请先把这些变量配置到终端或 IDE 运行配置中。

关键变量：

```text
MYSQL_HOST=your-mysql-host
MYSQL_PORT=3306
MYSQL_DATABASE=nekocafe
MYSQL_USERNAME=nekocafe_app
MYSQL_PASSWORD=change_me
JWT_SECRET=change_me_to_at_least_32_chars
VITE_API_BASE_URL=http://localhost:8080/api
```

> 安全提醒：项目运行账号不要使用 MySQL root。真实数据库密码、JWT 密钥不要写入代码、README、日志或截图。

## 启动后端

```bash
make dev-backend
```

或手动执行：

```bash
set -a && source .env && set +a
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端默认端口：`8080`。

验证：

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/health
```

Swagger UI：

```text
http://localhost:8080/swagger-ui/index.html
```

## 启动前端

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`。

## 角色演示账号

数据库迁移会创建以下课程演示账号，用于验证不同角色登录后的首页、菜单和接口权限。数据库中保存的是 BCrypt 哈希，不是明文密码；这些账号仅用于开发/答辩演示，真实部署前请禁用或更换。

| 角色 | 用户名 | 登录后入口 |
|---|---|---|
| 顾客 | `demo_customer` | `/stores` |
| 店员 | `demo_staff` | `/staff` |
| 猫咪管家 | `demo_cat` | `/cats` |
| 店长 | `demo_manager` | `/manager` |
| 总部运营 | `demo_hq` | `/dashboard` |
| 系统管理员 | `demo_admin` | `/admin` |

统一演示密码：`NekoCafe@2026`。

## 可选 Redis

如果需要本地 Redis：

```bash
make up
```

如果远程 MySQL 临时不可用，可用本地 MySQL 应急 profile：

```bash
docker compose --profile local-mysql up -d mysql-local
```

这只是离线开发兜底，正式开发配置仍以远程 MySQL 为准。

## MVP 开发主线

1. 注册登录
2. 浏览门店
3. 查询桌位与预约时段
4. 创建预约
5. 菜品点单
6. 支付沙箱
7. 店员签到与订单履约
8. 会员积分与基础数据看板

## 目录说明

```text
backend/   Spring Boot 后端
frontend/  Vue 3 前端
db/        数据库迁移与备份说明
infra/     部署和运维脚本
tests/     API/E2E 测试
docs/      课程设计文档和 ADR
```

## 团队

- 组别：第 3 组
- 选题：T-01 NekoCafé 智慧餐饮预约平台
