# 本地启动指南 - EduSphere教育SaaS平台

## 📋 前置要求

### 必需软件
- **Java**: JDK 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 7.0+
- **Node.js**: 18+
- **npm**: 9+

---

## 🚀 快速启动（5分钟）

### 第一步：启动基础服务

```bash
# 1. 启动MySQL（使用Docker）
docker run -d --name mysql-edu \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=edu_saas \
  mysql:8.0

# 2. 启动Redis（使用Docker）
docker run -d --name redis-edu \
  -p 6379:6379 \
  redis:7

# 或者使用已安装的本地服务
# brew services start mysql
# brew services start redis
```

---

### 第二步：启动后端

```bash
cd edu-saas-platform/backend

# 1. 安装依赖（首次运行）
mvn clean install -DskipTests

# 2. 启动应用
cd edu-api
mvn spring-boot:run

# 等待启动完成，看到以下信息：
# Started EduApiApplication in X.XXX seconds
# 后端地址：http://localhost:8080
```

---

### 第三步：启动前端

```bash
# 打开新终端
cd edu-saas-platform/frontend

# 1. 安装依赖（首次运行）
npm install

# 2. 启动开发服务器
npm run dev

# 启动成功后会显示：
# VITE v5.4.10  ready in XXX ms
# ➜  Local:   http://localhost:5173/
# 前端地址：http://localhost:5173
```

---

## 🎉 访问系统

### 打开浏览器

访问：http://localhost:5173

### 登录信息

```
机构编码：demo
账号：admin
密码：123456
```

---

## 🔧 常见问题

### 问题1：MySQL连接失败

**现象**：
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**解决方案**：
```bash
# 检查MySQL是否启动
docker ps | grep mysql

# 或
mysql -uroot -proot -e "SELECT 1"

# 如果失败，重启MySQL
docker restart mysql-edu
```

---

### 问题2：Redis连接失败

**现象**：
```
Unable to connect to Redis
```

**解决方案**：
```bash
# 检查Redis是否启动
redis-cli ping
# 应该返回：PONG

# 如果失败，重启Redis
docker restart redis-edu
```

---

### 问题3：前端npm install失败

**现象**：
```
npm ERR! network timeout
```

**解决方案**：
```bash
# 使用国内镜像
npm config set registry https://registry.npmmirror.com

# 重新安装
npm install
```

---

### 问题4：后端端口被占用

**现象**：
```
Port 8080 is already in use
```

**解决方案**：
```bash
# 查找占用端口的进程
lsof -i:8080

# 杀死进程
kill -9 <PID>

# 或修改端口（application.yml）
server.port: 8081
```

---

### 问题5：数据库初始化失败

**现象**：
```
Flyway migration failed
```

**解决方案**：
```bash
# 手动创建数据库
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS edu_saas"

# 重启后端
```

---

## 📊 系统架构

```
┌─────────────┐      ┌─────────────┐
│  浏览器      │─────▶│   前端      │
│ localhost   │      │ Vue3+Vite   │
│ :5173       │      │ :5173       │
└─────────────┘      └─────┬───────┘
                           │ API请求
                     ┌─────▼───────┐
                     │   后端      │
                     │ Spring Boot │
                     │ :8080       │
                     └─────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │  MySQL  │      │  Redis  │      │  文件   │
    │  :3306  │      │  :6379  │      │  系统   │
    └─────────┘      └─────────┘      └─────────┘
```

---

## 🎯 开发模式

### 热重载

**前端**：自动热重载（修改代码后自动刷新）

**后端**：使用Spring Boot DevTools
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
</dependency>
```

---

### 调试模式

**前端调试**：
```bash
# Chrome DevTools
F12 → Console/Network/Vue DevTools
```

**后端调试**：
```bash
# IntelliJ IDEA
右键 EduApiApplication → Debug

# 或命令行
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 📝 日志查看

### 后端日志

```bash
# 控制台输出
tail -f backend/edu-api/logs/application.log

# 或IntelliJ IDEA控制台
```

### 前端日志

```bash
# 浏览器控制台
F12 → Console

# 开发服务器日志
# 在npm run dev的终端查看
```

---

## 🔄 数据库管理

### 查看数据库

```bash
# 连接MySQL
mysql -uroot -proot edu_saas

# 查看表
SHOW TABLES;

# 查看数据
SELECT * FROM tenant;
SELECT * FROM account;
```

### Flyway迁移

```sql
-- 查看迁移历史
SELECT * FROM flyway_schema_history;
```

---

## 🧪 API测试

### 使用curl

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"organizationCode":"demo","username":"admin","password":"123456"}'

# 获取教师列表
curl http://localhost:8080/api/teachers \
  -H "Authorization: Bearer <token>"
```

### 使用Postman

1. 导入API文档
2. 设置环境变量：`base_url = http://localhost:8080`
3. 测试接口

---

## 🎊 启动成功标志

### 后端启动成功

```
  ____          _ _____       _
 | ____| __    | / ____|     | |
 | |__  / _` | | | (___   __ _| |_ ___
 |  __|| (_| | | |\___ \ / _` | __/ _ \
 | |____\__,_| |  ____) | (_| | ||  __/
 |______\__,_|_| |_____/ \__,_|\__\___|

Started EduApiApplication in 8.123 seconds
Server started on http://localhost:8080
```

### 前端启动成功

```
VITE v5.4.10  ready in 423 ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
➜  press h + enter to show help
```

### 登录成功

- ✅ 看到渐变紫色登录页
- ✅ 输入账号密码后跳转到运营总览
- ✅ 侧边栏显示菜单
- ✅ 顶部显示用户信息

---

## 🚀 下一步

启动成功后，你可以：

1. ✅ 浏览运营总览页（Dashboard）
2. ✅ 查看课程管理
3. ✅ 管理订单
4. ✅ 配置角色权限
5. ✅ 自定义机构主题

---

## 💡 开发建议

### 前端开发

```bash
# 安装Vue DevTools浏览器扩展
# Chrome商店搜索：Vue.js devtools
```

### 后端开发

```bash
# 推荐IDE：IntelliJ IDEA Ultimate
# 安装插件：Lombok、MyBatis、Spring Boot Assistant
```

### 代码格式化

```bash
# 前端
npm run lint

# 后端
mvn spotless:apply
```

---

## ✅ 系统健康检查

访问：http://localhost:8080/actuator/health

返回：
```json
{
  "status": "UP"
}
```

---

## 🎯 下一步：云服务器部署（等你买好后）

届时需要：
1. 配置生产环境数据库
2. 修改application-prod.yml
3. 打包前后端
4. Nginx反向代理
5. 配置域名和SSL

**现在先在本地开发，功能完善后再部署！** 🚀
