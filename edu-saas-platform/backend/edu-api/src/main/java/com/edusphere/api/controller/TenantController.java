package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.tenant.domain.Tenant;
import com.edusphere.tenant.domain.TenantTheme;
import com.edusphere.tenant.mapper.TenantMapper;
import com.edusphere.tenant.mapper.TenantThemeMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private static final Long DEMO_TENANT_ID = 1L;

    private final TenantMapper tenantMapper;
    private final TenantThemeMapper tenantThemeMapper;
    private final RedisSupportService redisSupportService;

    public TenantController(TenantMapper tenantMapper, TenantThemeMapper tenantThemeMapper, RedisSupportService redisSupportService) {
        this.tenantMapper = tenantMapper;
        this.tenantThemeMapper = tenantThemeMapper;
        this.redisSupportService = redisSupportService;
    }

    @GetMapping("/current")
    public ApiResult<Map<String, Object>> currentTenant() {
        Tenant tenant = tenantMapper.selectById(DEMO_TENANT_ID);
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }
        return ApiResult.ok(Map.of(
                "id", tenant.getId(),
                "name", tenant.getName(),
                "code", tenant.getCode(),
                "planCode", tenant.getPlanCode(),
                "status", tenant.getStatus()
        ));
    }

    @GetMapping("/theme")
    public ApiResult<Map<String, Object>> theme() {
        TenantTheme theme = redisSupportService.getOrLoadJson(
                "tenant:theme:" + DEMO_TENANT_ID,
                Duration.ofMinutes(10),
                TenantTheme.class,
                () -> tenantThemeMapper.selectOne(new LambdaQueryWrapper<TenantTheme>()
                        .eq(TenantTheme::getTenantId, DEMO_TENANT_ID)
                        .eq(TenantTheme::getDeleted, false)
                        .last("limit 1"))
        );
        if (theme == null) {
            throw new BizException(404, "租户主题不存在");
        }
        return ApiResult.ok(Map.of(
                "name", theme.getName(),
                "primaryColor", theme.getPrimaryColor(),
                "accentColor", theme.getAccentColor(),
                "surfaceColor", "#ffffff",
                "sidebarColor", "#111827",
                "sidebarTextColor", "#e5e7eb",
                "logoUrl", theme.getLogoUrl() == null ? "" : theme.getLogoUrl(),
                "layout", theme.getLayout()
        ));
    }
}
