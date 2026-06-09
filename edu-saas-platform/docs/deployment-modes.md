# 部署模式

系统按“一套代码，两种部署形态”设计：

- 公网 SaaS：多租户统一入口，适合云服务器、公网域名、统一运营。
- 私有化部署：单客户独立实例，适合客户内网、学校本地服务器、培训机构自有机房。

核心业务代码保持一致，差异主要在 Spring Profile、Docker Compose、端口暴露和环境变量。

## 公网 SaaS 部署

适用场景：

- 统一域名对多个租户提供服务。
- MySQL、Redis、API、Prometheus、Grafana 尽量只在内网访问。
- 公网只暴露 Web/Nginx。
- Prometheus/Grafana 端口默认绑定 `127.0.0.1`，需要通过 VPN、SSH 隧道或内网访问。

配置文件：

```text
backend/edu-api/src/main/resources/application-prod.yml
deploy/docker-compose.prod.yml
deploy/.env.saas.example
```

启动示例：

```bash
cd deploy
cp .env.saas.example .env
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

生产环境建议再加：

- HTTPS 证书和域名。
- WAF 或网关限流。
- 数据库定时备份。
- 对象存储，后续可接 OSS/S3/MinIO。
- Grafana 访问控制或 VPN。

## 私有化部署

适用场景：

- 单客户独立部署。
- 可在内网运行，不依赖公有云。
- 可本地挂载文件目录。
- 允许按客户网络策略暴露 API、MySQL、Redis、监控端口。

配置文件：

```text
backend/edu-api/src/main/resources/application-private.yml
deploy/docker-compose.private.yml
deploy/.env.private.example
```

启动示例：

```bash
cd deploy
cp .env.private.example .env
docker compose -f docker-compose.private.yml --env-file .env up -d --build
```

默认端口策略：

```text
Web        0.0.0.0:80
API        127.0.0.1:8080
MySQL      127.0.0.1:3306
Redis      127.0.0.1:6379
Prometheus 127.0.0.1:9090
Grafana    127.0.0.1:3000
```

如果客户内网需要其他机器访问，可在 `.env` 中调整：

```text
API_BIND_IP=0.0.0.0
PROMETHEUS_BIND_IP=0.0.0.0
GRAFANA_BIND_IP=0.0.0.0
```

不建议把 MySQL 和 Redis 直接暴露到公网。

## 配置差异

| 项目 | 公网 SaaS | 私有化 |
| --- | --- | --- |
| Spring Profile | `prod` | `private` |
| 多租户 | 开启 | 可单租户或多租户 |
| 公网入口 | Web/Nginx | Web/Nginx |
| API 端口 | Docker 内网 | 默认本机，可配置 |
| 监控端口 | 默认仅本机 | 默认仅本机，可配置 |
| 存储 | OSS/S3/MinIO 预留 | 本地文件/MinIO 预留 |
| JWT 时长 | 默认 120 分钟 | 默认 240 分钟 |
| 资源规格 | 更高并发 | 更保守，方便单机 |

## 后续增强

- 增加一键备份与恢复脚本。
- 增加 HTTPS/Nginx 证书模板。
- 增加 MinIO 私有化文件存储模板。
- 增加安装初始化脚本，自动生成强密码、检查端口、初始化管理员。
- 增加升级和回滚说明，避免客户现场升级不可控。
