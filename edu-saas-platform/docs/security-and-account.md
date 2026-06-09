# 权限与账号模型

## 账号身份拆分

不要把账号、人员、老师、学员混在一张表里。建议拆成：

```text
Account  登录账号
User     用户基础资料
Staff    员工身份
Teacher  教师身份
Student  学员身份
Parent   家长身份
```

一个账号可以绑定一种或多种身份。这样后续接老师端、家长端、学员端或移动端时，不需要推翻账号体系。

## 权限结构

采用：

```text
RBAC + 数据权限 + 机构数据隔离
```

代码和数据库内部仍使用 `tenant_id` 表示隔离边界，但产品界面和客户沟通统一使用“机构/公司/客户”，不要求最终使用者理解“租户”概念。

### RBAC

```text
Account/User -> Role -> Menu/Button/API Permission
```

权限粒度分三类：

- 菜单权限：能不能看到页面。
- 按钮权限：能不能新增、编辑、删除、导出。
- 接口权限：后端最终校验，不能只依赖前端隐藏。

### 数据权限

建议内置这些范围：

```text
ALL         全部数据
TENANT      当前租户数据
CAMPUS      指定校区数据
DEPARTMENT  指定部门数据
OWNER       本人数据
CUSTOM      自定义数据范围
```

典型角色：

```text
总部管理员  ALL
校区校长    CAMPUS
教务老师    CUSTOM，负责班级和学员
任课老师    OWNER，自己的课次和学员
财务        CUSTOM，收费相关数据
```

## 登录与 Token

建议：

- Access Token 短有效期。
- Refresh Token 长有效期。
- Redis 记录登录会话、Token 黑名单、权限缓存。
- 修改密码、禁用账号、角色变更后立即清理缓存。

当前管理端已经使用正式登录页，不再自动使用演示账号登录。登录时可输入机构编码、账号和密码，本地演示机构编码为 `demo`。

管理端已具备：

- 未登录访问后台自动跳转 `/login`。
- 登录成功后保存 Access Token 并跳回原访问页面。
- 接口返回 401/403 时清理本地登录态并回到登录页。
- 顶栏提供退出登录入口。

服务端已补齐：

- 登录时发放 Access Token 和 Refresh Token。
- Refresh Token 以哈希 key 存储在 Redis，不明文落库。
- `/api/auth/refresh` 可刷新 Access Token。
- `/api/auth/logout` 会吊销当前 Access Token，并删除 Refresh Token。
- 被吊销的 Access Token 再访问接口会返回 `HTTP 401`。

后续仍需增强多端会话列表、踢下线、单账号最大在线设备数。

## 密码安全

当前账号密码已使用 BCrypt 存储：

- 新建账号和重置密码只写入 BCrypt 哈希。
- 登录校验使用 `PasswordService.matches`。
- 兼容早期演示数据中的明文密码，首次成功登录后会自动升级为 BCrypt。
- 新密码至少 8 位，并要求同时包含字母和数字。

生产环境禁止直接写入明文密码到 `account.password_hash`。

## 审计要求

所有关键操作记录：

- 操作人
- 租户
- IP
- User-Agent
- 操作模块
- 操作类型
- 业务 ID
- 操作前后摘要
- 操作时间
