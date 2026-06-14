# ADR-0001 使用远程 MySQL 8 作为主数据库

## 状态

Accepted

## 背景

原技术路线文档中推荐 PostgreSQL，但也说明如果团队更熟悉 MySQL 可以替换。当前团队已经在云服务器上完成 MySQL 连接，因此源码框架统一改为远程 MySQL 8。

## 决策

- 主数据库使用远程 MySQL 8。
- 后端使用 Spring Boot 3 + MyBatis-Plus 访问数据库。
- 使用 Flyway 管理 MySQL 方言迁移脚本。
- 真实连接信息通过环境变量注入，不提交到 Git。

## 影响

- DDL 使用 `BIGINT AUTO_INCREMENT`、InnoDB、utf8mb4。
- 预约并发控制使用事务、条件更新和乐观锁。
- Docker Compose 不再默认启动 PostgreSQL；本地 MySQL 只作为应急 profile。

## 风险与缓解

- 远程数据库网络不稳定：保留本地 MySQL 应急方案和备份说明。
- 凭据泄露：使用 `.env.example` 占位，真实 `.env` 被 `.gitignore` 忽略。
- 方言差异：所有迁移脚本统一按 MySQL 8 编写。
