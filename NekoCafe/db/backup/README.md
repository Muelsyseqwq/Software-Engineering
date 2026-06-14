# 数据库备份与恢复

远程 MySQL 建议使用云厂商快照或 `mysqldump` 进行逻辑备份。

示例占位命令：

```bash
mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USERNAME" -p "$MYSQL_DATABASE" > nekocafe_backup.sql
```

不要把备份文件、真实连接串或密码提交到 Git。
