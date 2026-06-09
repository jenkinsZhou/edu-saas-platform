package com.edusphere.api.service.tenant;

import com.edusphere.common.exception.BizException;
import com.edusphere.tenant.domain.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrganizationResolver {

    private final JdbcTemplate jdbcTemplate;
    private final String defaultOrganizationCode;
    private final String baseDomain;

    public OrganizationResolver(
            JdbcTemplate jdbcTemplate,
            @Value("${edu.organization.default-code:demo}") String defaultOrganizationCode,
            @Value("${edu.organization.base-domain:}") String baseDomain
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.defaultOrganizationCode = defaultOrganizationCode;
        this.baseDomain = baseDomain;
    }

    public Tenant resolve(HttpServletRequest request, String requestedOrganizationCode) {
        String code = firstText(
                requestedOrganizationCode,
                request.getHeader("X-Organization-Code"),
                request.getHeader("X-Tenant-Code"),
                resolveCodeFromHost(request.getServerName()),
                defaultOrganizationCode
        );
        Tenant tenant = jdbcTemplate.query("""
                        select id, name, code, status, plan_code
                        from tenant
                        where code = ? and deleted = 0
                        limit 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Tenant item = new Tenant();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setCode(rs.getString("code"));
                    item.setStatus(rs.getString("status"));
                    item.setPlanCode(rs.getString("plan_code"));
                    return item;
                },
                code);
        if (tenant == null || !"ACTIVE".equals(tenant.getStatus())) {
            throw new BizException(401, "机构不存在或已停用");
        }
        return tenant;
    }

    private String resolveCodeFromHost(String host) {
        if (!StringUtils.hasText(host)) {
            return "";
        }
        String normalizedHost = host.split(":")[0].toLowerCase();
        if (isLocalHost(normalizedHost)) {
            return "";
        }
        if (StringUtils.hasText(baseDomain) && normalizedHost.endsWith("." + baseDomain)) {
            String subdomain = normalizedHost.substring(0, normalizedHost.length() - baseDomain.length() - 1);
            return subdomain.contains(".") ? "" : subdomain;
        }
        return "";
    }

    private boolean isLocalHost(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return defaultOrganizationCode;
    }
}
