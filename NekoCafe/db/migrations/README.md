# 数据库迁移说明

正式可执行 Flyway 脚本位于：

```text
backend/src/main/resources/db/migration/
```

当前项目使用远程 MySQL 8，所有迁移脚本必须使用 MySQL 方言。不要使用 PostgreSQL 的 `BIGSERIAL`、`JSONB`、partial index 等语法。

执行迁移时，后端会在启动阶段由 Flyway 自动执行。真实数据库连接信息通过环境变量提供，不写入脚本。
