# EduSphere 教育SaaS平台

一个基于Spring Boot 3和现代化技术栈构建的多租户教育管理系统。面向教培行业的 SaaS 多租户管理系统，提供课程管理、订单管理、学员管理等核心功能。

当前目标是先建立稳定、可扩展的后端架构，并提供 Vue 管理端的基础工程与租户主题能力。后续 Android、iOS、小程序等客户端都可以复用同一套开放 API。

## ⚠️ 安全说明

**生产环境部署前必读：**

本项目已修复多个安全问题（详见下方"已修复的安全问题"），但部署到生产环境前，请务必：

1. **设置强随机JWT密钥** - 使用64+字符的随机字符串
2. **配置所有环境变量** - 不要使用默认密钥
3. **启用HTTPS** - 所有生产流量必须加密
4. **数据库加密连接** - 配置SSL/TLS
5. **Redis密码认证** - 不要使用无密码Redis

详细配置说明见下方"快速开始"章节。

## 技术栈

### 后端

- **Java 21**
- **Spring Boot 3.3.5**
- **MyBatis Plus 3.5.9** - 持久层框架
- **MySQL 8.4** - 数据库
- **Redis** - 缓存和会话管理
- **JWT (JJWT 0.12.6)** - 身份认证
- **Flyway** - 数据库迁移
- **SpringDoc OpenAPI 2.6.0** - API文档

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- ECharts 预留

## 目录

```text
edu-saas-platform
├─ backend
│  ├─ edu-common       公共响应、异常、审计字段、工具类
│  ├─ edu-security     登录认证、权限校验、Token、数据权限上下文
│  ├─ edu-tenant       租户、套餐、主题、租户配置
│  ├─ edu-system       账号、用户、角色、菜单、组织、岗位、数据权限
│  ├─ edu-course       课程模板、班级、课次、扩展字段
│  └─ edu-api          Spring Boot 启动模块和 HTTP API
├─ frontend            Vue 管理端
└─ docs                架构、权限、数据模型文档
```

## 架构原则

1. 先使用模块化单体，保证开发效率、事务简单、部署稳定。
2. 所有核心业务表都带 `tenant_id`，从第一天支持 SaaS。
3. 权限采用 `RBAC + 数据权限 + 租户隔离`。
4. 课程采用 `课程产品 -> 班级实例 -> 课次执行 -> 业务记录` 的抽象模型。
5. 租户主题使用配置化 Token，前端按租户加载主题变量。

## 本地开发环境

### 环境要求
- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6+
- Node.js 18+ (前端开发)

### 配置环境变量

**重要：生产环境必须配置以下环境变量，不要使用默认值！**

创建 `.env` 文件或在系统中设置：

```bash
# 数据库配置
export DB_URL="jdbc:mysql://localhost:3306/edu_saas?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
export DB_USERNAME="root"
export DB_PASSWORD="your_secure_password"

# Redis配置
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD="your_redis_password"

# JWT密钥（必须设置！建议64字符以上的随机字符串）
export JWT_SECRET="your-secure-jwt-secret-key-at-least-64-characters-long-random-string"

# 支付回调密钥
export PAYMENT_CALLBACK_SECRET="your-secure-payment-callback-secret-key"
```

### 后端开发启动

```bash
cd backend
mvn clean install
cd edu-api
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动

**API文档**: `http://localhost:8080/swagger-ui.html`

### 前端开发启动

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

前端应用将在 `http://localhost:5173` 启动

## 核心功能

### 1. 多租户架构
- 基于 `tenant_id` 的数据隔离
- 支持共享表模式（Shared Table）
- 数据权限控制（租户级、校区级、个人级）

### 2. 课程管理
- 课程产品管理
- 班级管理（容量控制、并发安全）
- 学员管理
- 课时管理

### 3. 订单系统
- 报名订单管理
- 支付流程（支持多渠道）
- 退款管理
- 订单状态流转
- 支付回调幂等性保护

### 4. 安全认证
- JWT Token认证
- Token刷新机制
- Token撤销（Redis黑名单）
- BCrypt密码加密（强度12）
- 防用户枚举攻击

### 5. 审计日志
- 订单审计日志
- 支付回调日志
- 操作日志

## 已修复的安全问题

本项目在代码审查后修复了以下安全问题：

1. ✅ **订单号生成安全** - 使用 `SecureRandom` 替代可预测算法，防止订单号伪造
2. ✅ **JSON序列化安全** - 使用 `ObjectMapper` 替代手工字符串拼接，防止注入攻击
3. ✅ **支付回调幂等性** - 数据库唯一约束 + Redis双重保护，防止重复处理
4. ✅ **班级容量并发控制** - 悲观锁原子化容量检查，防止超容
5. ✅ **登录安全** - 统一错误消息，防止用户枚举攻击
6. ✅ **配置安全** - 敏感信息使用环境变量，移除硬编码密钥
7. ✅ **金额精度统一** - 所有BigDecimal操作统一scale，防止精度问题

## 数据库迁移

项目使用Flyway进行数据库版本管理，迁移文件位于：
```
backend/edu-api/src/main/resources/db/migration/
- V1__baseline.sql           # 基础表结构
- V2__audit_and_callbacks.sql  # 审计和回调表
- V3__payment_refund_orders.sql # 支付退款订单表
- V4__add_callback_unique_constraint.sql # 回调幂等性约束
```

首次启动时会自动执行所有迁移脚本。

## API认证

### 登录获取Token

```bash
POST /api/auth/login
Content-Type: application/json

{
  "organizationCode": "demo",
  "username": "admin",
  "password": "password"
}
```

返回：
```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "profile": { ... }
  }
}
```

### 使用Token访问API

```bash
GET /api/orders
Authorization: Bearer {accessToken}
```

## 监控端点

健康检查和指标端点：
- `/actuator/health` - 健康检查
- `/actuator/metrics` - 应用指标
- `/actuator/prometheus` - Prometheus指标

## 生产部署建议

1. **JWT密钥** - 使用强随机密钥（64+字符），不要使用示例密钥
2. **数据库连接** - 启用SSL加密连接
3. **Redis** - 配置密码认证，禁止匿名访问
4. **HTTPS** - 所有生产环境必须启用HTTPS
5. **防火墙** - 仅开放必要端口（80/443）
6. **日志** - 启用审计日志和访问日志
7. **备份** - 配置定期数据库备份

## 文档

- [架构方案](docs/architecture.md)
- [权限与账号模型](docs/security-and-account.md)
- [课程核心模型](docs/course-model.md)
- [租户主题设计](docs/tenant-theme.md)
- [第一阶段接口清单](docs/api-first-phase.md)
- [生产可用性说明](docs/production-readiness.md)
- [部署模式](docs/deployment-modes.md)

## 开发指南

### 添加新模块

1. 在 `backend/` 下创建新模块目录
2. 添加 `pom.xml` 并继承父模块
3. 在父 `pom.xml` 的 `<modules>` 中添加模块引用

### 数据库迁移

创建新的迁移文件：
```
V{version}__{description}.sql
```

例如：`V5__add_student_remarks.sql`

## 开发演示账号

```text
组织代码：demo
用户名：admin
密码：demo123456
```

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题，请提交Issue或联系项目维护者。

---

**版本**: 0.1.0-SNAPSHOT  
**最后更新**: 2026年6月
