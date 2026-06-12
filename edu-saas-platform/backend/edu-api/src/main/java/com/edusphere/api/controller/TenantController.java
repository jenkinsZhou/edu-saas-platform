package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.tenant.domain.Tenant;
import com.edusphere.tenant.domain.TenantTheme;
import com.edusphere.tenant.mapper.TenantMapper;
import com.edusphere.tenant.mapper.TenantThemeMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

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
        Tenant tenant = tenantMapper.selectById(SecurityContext.tenantId());
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
        Long tenantId = SecurityContext.tenantId();
        TenantTheme theme = redisSupportService.getOrLoadJson(
                "tenant:theme:" + tenantId,
                Duration.ofMinutes(10),
                TenantTheme.class,
                () -> tenantThemeMapper.selectOne(new LambdaQueryWrapper<TenantTheme>()
                        .eq(TenantTheme::getTenantId, tenantId)
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

    @PostMapping("/theme")
    public ApiResult<Void> saveTheme(@RequestBody SaveThemeRequest request) {
        Long tenantId = SecurityContext.tenantId();
        TenantTheme theme = tenantThemeMapper.selectOne(new LambdaQueryWrapper<TenantTheme>()
                .eq(TenantTheme::getTenantId, tenantId)
                .eq(TenantTheme::getDeleted, false)
                .last("limit 1"));
        if (theme == null) {
            throw new BizException(404, "租户主题不存在");
        }
        if (request.primaryColor() != null) {
            theme.setPrimaryColor(requireColor(request.primaryColor()));
        }
        if (request.accentColor() != null) {
            theme.setAccentColor(requireColor(request.accentColor()));
        }
        theme.setCustomCssVarsJson(String.format(
                "{\"surfaceColor\":\"%s\",\"sidebarColor\":\"%s\",\"sidebarTextColor\":\"%s\"}",
                orDefault(request.surfaceColor(), "#ffffff"),
                orDefault(request.sidebarColor(), "#111827"),
                orDefault(request.sidebarTextColor(), "#e5e7eb")));
        tenantThemeMapper.updateById(theme);
        redisSupportService.evict("tenant:theme:" + tenantId);
        return ApiResult.ok();
    }

    public record SaveThemeRequest(
            String primaryColor,
            String accentColor,
            String surfaceColor,
            String sidebarColor,
            String sidebarTextColor
    ) {
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : requireColor(value);
    }

    private static String requireColor(String value) {
        if (!value.matches("^#[0-9a-fA-F]{3,8}$")) {
            throw new BizException(400, "颜色值格式不正确：" + value);
        }
        return value;
    }
}
