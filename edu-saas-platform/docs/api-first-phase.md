# 第一阶段接口清单

## 认证

- `POST /api/auth/login` 登录
- `POST /api/auth/logout` 退出
- `POST /api/auth/refresh` 刷新 Token，后续实现

## 租户

- `GET /api/tenant/current` 当前租户
- `GET /api/tenant/theme` 当前租户主题
- `PUT /api/tenant/theme` 更新租户主题，后续实现

## 系统权限

- `GET /api/system/roles` 角色列表
- `GET /api/system/menus` 菜单权限树
- `POST /api/system/roles` 新增角色，后续实现
- `PUT /api/system/roles/{id}` 修改角色，后续实现
- `PUT /api/system/roles/{id}/permissions` 分配权限，后续实现

## 课程

- `GET /api/courses/products` 课程产品列表
- `POST /api/courses/products` 新增课程产品，后续实现
- `PUT /api/courses/products/{id}` 修改课程产品，后续实现
- `GET /api/courses/classes` 班级列表，后续实现
- `GET /api/courses/lessons` 课次列表，后续实现
