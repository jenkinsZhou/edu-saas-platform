package com.edusphere.api.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

/**
 * 授权文件的明文载荷。由厂商用私钥对其 JSON 字节签名后下发，系统内置公钥校验。
 *
 * <p>所有用量上限字段：{@code null} 或负数表示不限。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LicensePayload(
        String licenseId,
        String customer,
        String edition,
        LocalDate issuedAt,
        LocalDate expiresAt,
        Integer graceDays,
        Integer maxAccounts,
        Integer maxStudents,
        Integer maxCampuses,
        List<String> features,
        String fingerprint
) {

    public int graceDaysOrDefault() {
        return graceDays == null || graceDays < 0 ? 0 : graceDays;
    }

    public boolean unlimited(Integer cap) {
        return cap == null || cap < 0;
    }
}
