package com.edusphere.api.license;

import com.edusphere.common.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 用量上限校验。授权以“整套部署”为维度，因此统计不区分租户（私有化场景通常单机构多校区）。
 * 仅在写操作放行（VALID/GRACE）后才会触达这里，过期只读已由拦截器拦截。
 */
@Service
public class LicenseQuotaService {

    private final LicenseService licenseService;
    private final JdbcTemplate jdbcTemplate;

    public LicenseQuotaService(LicenseService licenseService, JdbcTemplate jdbcTemplate) {
        this.licenseService = licenseService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void assertCanCreateAccount() {
        check(licenseService.maxAccounts(), "account", "账号");
    }

    public void assertCanCreateStudent() {
        check(licenseService.maxStudents(), "student", "学员");
    }

    public void assertCanCreateCampus() {
        check(licenseService.maxCampuses(), "campus", "校区");
    }

    private void check(Integer cap, String table, String label) {
        if (!licenseService.enforcementEnabled() || cap == null || cap < 0) {
            return; // 未启用授权或不限量
        }
        long current = count(table);
        if (current >= cap) {
            throw new BizException(402, "已达授权" + label + "数量上限（" + cap + "），如需扩容请联系厂商升级授权");
        }
    }

    private long count(String table) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where deleted = 0", Long.class);
        return count == null ? 0 : count;
    }
}
