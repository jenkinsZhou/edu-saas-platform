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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private static final String DEFAULT_SURFACE = "#ffffff";
    private static final String DEFAULT_SIDEBAR = "#0f1b3d";
    private static final String DEFAULT_SIDEBAR_TEXT = "#dbeafe";
    private static final String DEFAULT_BRAND_ICON = "book";

    private final TenantMapper tenantMapper;
    private final TenantThemeMapper tenantThemeMapper;
    private final RedisSupportService redisSupportService;
    private final ObjectMapper objectMapper;

    public TenantController(TenantMapper tenantMapper, TenantThemeMapper tenantThemeMapper,
                            RedisSupportService redisSupportService, ObjectMapper objectMapper) {
        this.tenantMapper = tenantMapper;
        this.tenantThemeMapper = tenantThemeMapper;
        this.redisSupportService = redisSupportService;
        this.objectMapper = objectMapper;
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
        Map<String, String> vars = parseVars(theme.getCustomCssVarsJson());
        Map<String, Object> result = new HashMap<>();
        result.put("name", theme.getName());
        result.put("primaryColor", theme.getPrimaryColor());
        result.put("accentColor", theme.getAccentColor());
        result.put("surfaceColor", vars.getOrDefault("surfaceColor", DEFAULT_SURFACE));
        result.put("sidebarColor", vars.getOrDefault("sidebarColor", DEFAULT_SIDEBAR));
        result.put("sidebarTextColor", vars.getOrDefault("sidebarTextColor", DEFAULT_SIDEBAR_TEXT));
        result.put("brandIcon", vars.getOrDefault("brandIcon", DEFAULT_BRAND_ICON));
        result.put("logoUrl", theme.getLogoUrl() == null ? "" : theme.getLogoUrl());
        result.put("layout", theme.getLayout());
        return ApiResult.ok(result);
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
        Map<String, String> vars = new HashMap<>();
        vars.put("surfaceColor", orDefault(request.surfaceColor(), DEFAULT_SURFACE));
        vars.put("sidebarColor", orDefault(request.sidebarColor(), DEFAULT_SIDEBAR));
        vars.put("sidebarTextColor", orDefault(request.sidebarTextColor(), DEFAULT_SIDEBAR_TEXT));
        vars.put("brandIcon", requireIconKey(request.brandIcon()));
        theme.setCustomCssVarsJson(writeVars(vars));
        tenantThemeMapper.updateById(theme);
        redisSupportService.evict("tenant:theme:" + tenantId);
        return ApiResult.ok();
    }

    public record SaveThemeRequest(
            String primaryColor,
            String accentColor,
            String surfaceColor,
            String sidebarColor,
            String sidebarTextColor,
            String brandIcon
    ) {
    }

    private Map<String, String> parseVars(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            return parsed == null ? Map.of() : parsed;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeVars(Map<String, String> vars) {
        try {
            return objectMapper.writeValueAsString(vars);
        } catch (Exception e) {
            throw new BizException(500, "主题序列化失败");
        }
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

    private static String requireIconKey(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BRAND_ICON;
        }
        if (!value.matches("^[a-zA-Z]{2,32}$")) {
            throw new BizException(400, "图标标识不合法：" + value);
        }
        return value;
    }
}
