# 生产可用性说明

这份文档记录当前系统面向生产环境已经具备的能力，以及后续继续增强的方向。

## 当前已经具备

- 多租户隔离：核心业务表带 `tenant_id`，登录态、菜单、角色、课程、订单接口都按租户上下文处理。
- 权限底座：支持账号、角色、菜单权限、数据范围、校区范围，前端菜单由后端权限动态生成。
- 数据可靠性：订单支付、退款、确认、取消等关键写操作使用幂等键、行锁和审计记录。
- 缓存与限流：Redis 用于分布式缓存、登录限流、关键写接口限流和幂等；开发环境保留本地降级。
- 数据库治理：Flyway 负责 schema 版本迁移，关键查询已增加索引，列表接口使用数据库分页。
- 请求追踪：所有 HTTP 响应返回 `X-Request-Id`，业务响应体也带 `requestId`，日志中可按 requestId 串联排查。
- 慢请求日志：接口耗时超过 `edu.observability.slow-request-threshold-ms` 会自动输出 WARN 日志。
- 健康检查：`/api/health/live` 检查进程活性，`/api/health/ready` 检查 MySQL 和 Redis 是否可用。
- 指标监控：Spring Actuator 暴露健康、JVM、线程、HTTP、连接池等指标，Prometheus 可抓取 `/actuator/prometheus`。
- 部署骨架：提供 MySQL、Redis、API、Web、Prometheus、Grafana 的 Docker Compose 生产骨架。

## 关键接口

```text
GET /api/health/live       进程活性检查
GET /api/health/ready      依赖就绪检查，MySQL 或 Redis 异常时返回 503
GET /actuator/health       Spring Boot 健康检查
GET /actuator/prometheus   Prometheus 指标抓取
```

生产部署中 API 容器只在 Docker 内网暴露给 Web、Prometheus 等服务，不建议直接暴露到公网。

## 建议的生产部署边界

- 公网入口只暴露 Nginx/Web。
- MySQL、Redis、API、Prometheus、Grafana 默认部署在内网。
- Redis 必须启用密码和持久化。
- MySQL 必须使用独立数据盘并配置定时备份。
- `JWT_SECRET`、`PAYMENT_CALLBACK_SECRET`、数据库密码、Redis 密码必须使用随机强密码。
- Prometheus 和 Grafana 如果需要公网访问，必须额外加反向代理认证或 VPN。

## 仍需继续增强

- 支付通道抽象：按微信、支付宝、线下收款等实现签名验签、回调验签、退款查询。
- 对账与补偿任务：定时扫描长时间未完成的支付单、退款单、回调异常记录。
- 单元测试和集成测试：覆盖订单状态机、幂等、并发扣减、权限边界、租户隔离。
- SQL 审计和慢 SQL：接入数据库侧慢查询日志，并把高频慢接口与 SQL 关联。
- 灰度发布：增加版本号、实例信息、滚动发布与回滚脚本。
- 备份恢复演练：定期验证 MySQL 备份可恢复，而不只是“已经备份”。
