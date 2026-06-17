package com.edusphere.api.license;

import java.time.LocalDate;
import java.util.List;

/**
 * 授权运行态视图，供管理端授权页展示。
 */
public record LicenseInfo(
        LicenseStatus status,
        boolean writable,
        String message,
        String customer,
        String edition,
        String licenseId,
        LocalDate issuedAt,
        LocalDate expiresAt,
        Integer graceDays,
        long daysToExpiry,
        Integer maxAccounts,
        Integer maxStudents,
        Integer maxCampuses,
        List<String> features,
        String machineFingerprint,
        String boundFingerprint
) {
}
