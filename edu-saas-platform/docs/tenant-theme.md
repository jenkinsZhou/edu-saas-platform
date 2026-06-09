# 租户主题设计

## 目标

不同租户可以使用不同品牌主题，例如 logo、主色、侧边栏颜色、布局风格。平台也可以提供几套官方主题给客户选择。

## 后端模型

建议表：

```text
tenant_theme
```

核心字段：

- tenant_id
- name
- primary_color
- accent_color
- logo_url
- layout
- custom_css_vars_json
- enabled

## 前端实现

前端启动后请求：

```text
GET /api/tenant/theme
```

拿到主题配置后写入 CSS 变量：

```text
--edu-primary
--edu-accent
--edu-sidebar
--edu-sidebar-text
--edu-surface
```

Element Plus 的主色也同步写入：

```text
--el-color-primary
```

## 为什么用 CSS 变量

- 不需要为每个客户重新打包。
- 主题切换实时生效。
- 适合 SaaS 多租户。
- 后续可以扩展暗色模式、节日主题、行业主题。
