# NekoCafé 智慧餐饮预约平台

> T-01 课程设计源码工程。系统面向猫咪主题餐厅，支持顾客预约用餐、提前点单、店员签到与订单履约、会员积分、猫咪档案维护和运营数据看板。

## 技术栈

- 后端：Spring Boot 3、Spring Security、JWT、MyBatis-Plus、Flyway、MySQL 8
- 前端：Vue 3、Vite、TypeScript、Vue Router、Pinia、Element Plus、ECharts
- 数据库：远程 MySQL 8，开发环境通过环境变量连接
- 可选缓存：Redis 7

## 前置依赖

### Docker 一键演示启动

如果只需要按课程验收方式运行完整系统，推荐安装：

- Docker Desktop / Docker Engine
- Docker Compose v2

然后直接使用下方“一键 Docker 启动”。

### 手工本地开发

如果需要分别启动前后端进行开发，需要安装：

- JDK 17+
- Maven 3.9+
- Node.js 20+
- npm 10+ 或兼容包管理器
- 可访问的远程 MySQL 8 数据库

## 一键 Docker 启动

在 `NekoCafe/` 目录执行：

```bash
docker compose up --build
```

首次启动会构建前后端镜像，MySQL 初始化后后端会通过 Flyway 自动创建表结构并写入演示数据。启动完成后访问：

| 服务 | 地址 |
|---|---|
| 前端应用 | `http://localhost` |
| 后端健康检查 | `http://localhost:8080/actuator/health` |
| 反向代理 API 健康检查 | `http://localhost/api/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |

Docker Compose 使用 `COMPOSE_MYSQL_*` 变量创建并连接容器内 MySQL，避免和手工开发用的 `MYSQL_HOST`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 冲突。

如果本机 `80`、`8080`、`3306` 或 `6379` 端口被占用，可复制 `.env.example` 为 `.env` 并调整：

```dotenv
FRONTEND_PORT_HOST=8088
BACKEND_PORT_HOST=18080
MYSQL_PORT_HOST=3307
REDIS_PORT_HOST=6380
```

调整后访问地址同步变为：

| 服务 | 示例地址 |
|---|---|
| 前端应用 | `http://localhost:8088` |
| 后端健康检查 | `http://localhost:18080/actuator/health` |
| 反向代理 API 健康检查 | `http://localhost:8088/api/health` |
| Swagger UI | `http://localhost:18080/swagger-ui/index.html` |
| MySQL | `localhost:3307` |
| Redis | `localhost:6380` |

停止服务：

```bash
docker compose down
```

如需清理数据库、Redis 和上传文件卷，恢复首次启动状态：

```bash
docker compose down -v
```

如果后端日志出现 `Access denied for user ...`，通常是旧 `.env` 变量或旧 MySQL 数据卷中的账号与当前 Compose 配置不一致。请先确认 `.env` 中的 `COMPOSE_MYSQL_*` 变量，必要时执行 `docker compose down -v` 后重新 `docker compose up --build -d`。

### 启用 DeepSeek AI 推荐

Docker Compose 默认保持 `AI_ENABLED=false`，这样没有 API Key 时系统也能稳定启动，并使用规则推荐/兜底推荐文案。如果需要启用你接入的 DeepSeek，请在本地 `.env` 中配置：

```dotenv
AI_ENABLED=true
AI_PROVIDER=openai-compatible
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=你的_DeepSeek_API_Key
AI_MODEL=deepseek-v4-flash
AI_TIMEOUT_SECONDS=20
AI_MAX_TOKENS=1000
AI_TEMPERATURE=0.3
```

修改后重建后端容器：

```bash
docker compose up --build -d backend frontend
```

> 安全提醒：不要把真实 `AI_API_KEY` 写入代码、README、截图、日志或提交记录；只放在本机 `.env`、CI Secret 或服务器环境变量中。

> 安全提醒：`.env.example` 和 `docker-compose.yml` 中的密码、JWT secret 仅用于本地课程演示。真实部署前必须通过 `.env`、CI Secret 或服务器环境变量替换，不要把真实密钥写入代码、README、日志或截图。

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
cp .env.example .env
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

## Redis 与本地 MySQL

`docker compose up --build` 会默认启动 Redis 和 MySQL，并将后端连接到 Compose 内部的 `mysql` 服务。手工开发时如需只启动基础设施，可仍使用 Docker Compose 指定服务：

```bash
docker compose up -d mysql redis
```

如果你使用远程 MySQL 手工开发，请按 `.env.example` 配置 `MYSQL_HOST`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 等变量，并在终端或 IDE 中导出后再启动后端。

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
