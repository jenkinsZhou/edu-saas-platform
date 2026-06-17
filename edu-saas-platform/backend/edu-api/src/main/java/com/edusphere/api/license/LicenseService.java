package com.edusphere.api.license;

import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * 授权加载、验签与状态判定的核心服务。
 *
 * <p>授权来源优先级：数据库 {@code system_license} 单行记录 → 配置文件 {@code edu.license.file}。
 * 验签通过的载荷缓存在内存，按日期实时计算 {@link LicenseStatus}（无需重复验签）。</p>
 */
@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);
    private static final long ROW_ID = 1L;

    private final LicenseProperties properties;
    private final MachineFingerprintProvider fingerprintProvider;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RedisSupportService redisSupportService;

    /** 当前已验签通过的载荷；null 表示未授权或验签失败。 */
    private volatile LicensePayload payload;
    /** 上次加载是否因签名/格式问题失败（区分 UNLICENSED 与 INVALID）。 */
    private volatile boolean signatureRejected;

    public LicenseService(
            LicenseProperties properties,
            MachineFingerprintProvider fingerprintProvider,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            RedisSupportService redisSupportService
    ) {
        this.properties = properties;
        this.fingerprintProvider = fingerprintProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.redisSupportService = redisSupportService;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.warn("授权校验已关闭 (edu.license.enabled=false)，仅应用于开发环境");
            return;
        }
        reload();
    }

    /** 重新从数据库（或文件兜底）加载并验签当前授权。 */
    public synchronized void reload() {
        String licenseText = readFromDatabase();
        if (!StringUtils.hasText(licenseText)) {
            licenseText = importFromFileIfPresent();
        }
        applyLicenseText(licenseText, false);
        evictNavCache();
    }

    private void evictNavCache() {
        try {
            redisSupportService.evictByPrefix("nav:");
        } catch (Exception e) {
            log.warn("清理导航缓存失败: {}", e.getMessage());
        }
    }

    /** 上传/激活新授权：验签通过即落库并刷新缓存，失败抛 BizException。 */
    public synchronized LicenseInfo activate(String licenseText, Long accountId) {
        if (!StringUtils.hasText(licenseText)) {
            throw new BizException(400, "授权内容为空");
        }
        LicensePayload parsed = verify(licenseText.trim());
        if (parsed == null) {
            throw new BizException(400, "授权文件无效：签名校验未通过或格式错误");
        }
        if (!fingerprintMatches(parsed)) {
            throw new BizException(400, "授权文件与本机不匹配：当前机器码 " + fingerprintProvider.fingerprint());
        }
        persist(licenseText.trim(), parsed, accountId);
        applyLicenseText(licenseText.trim(), true);
        evictNavCache();
        log.info("授权已更新 licenseId={} customer={} expiresAt={}", parsed.licenseId(), parsed.customer(), parsed.expiresAt());
        return currentInfo();
    }

    /** 当前是否允许写操作。 */
    public boolean writable() {
        return !properties.isEnabled() || currentStatus().writable();
    }

    /** 指定功能模块是否在授权范围内（用于功能开关）。 */
    public boolean isFeatureEnabled(String featureCode) {
        if (!properties.isEnabled()) {
            return true;
        }
        LicenseStatus status = currentStatus();
        if (status == LicenseStatus.INVALID || status == LicenseStatus.UNLICENSED) {
            return false;
        }
        LicensePayload current = payload;
        if (current == null || current.features() == null) {
            return false;
        }
        return current.features().stream()
                .filter(Objects::nonNull)
                .anyMatch(f -> f.trim().equalsIgnoreCase(featureCode));
    }

    public boolean enforcementEnabled() {
        return properties.isEnabled();
    }

    public Integer maxAccounts() {
        LicensePayload current = payload;
        return current == null ? null : current.maxAccounts();
    }

    public Integer maxStudents() {
        LicensePayload current = payload;
        return current == null ? null : current.maxStudents();
    }

    public Integer maxCampuses() {
        LicensePayload current = payload;
        return current == null ? null : current.maxCampuses();
    }

    public LicenseStatus currentStatus() {
        if (!properties.isEnabled()) {
            return LicenseStatus.VALID; // 校验未启用（开发模式）：不打扰、不只读
        }
        LicensePayload current = payload;
        if (current == null) {
            return signatureRejected ? LicenseStatus.INVALID : LicenseStatus.UNLICENSED;
        }
        if (!fingerprintMatches(current)) {
            return LicenseStatus.INVALID;
        }
        LocalDate today = LocalDate.now();
        LocalDate expiresAt = current.expiresAt();
        if (expiresAt == null) {
            return LicenseStatus.VALID;
        }
        if (!today.isAfter(expiresAt)) {
            return LicenseStatus.VALID;
        }
        LocalDate graceEnd = expiresAt.plusDays(current.graceDaysOrDefault());
        if (!today.isAfter(graceEnd)) {
            return LicenseStatus.GRACE;
        }
        return LicenseStatus.EXPIRED;
    }

    public LicenseInfo currentInfo() {
        LicenseStatus status = currentStatus();
        LicensePayload current = payload;
        String machine = fingerprintProvider.fingerprint();
        if (current == null) {
            return new LicenseInfo(status, status.writable(), describe(status, null),
                    null, null, null, null, null, null, 0,
                    null, null, null, List.of(), machine, null);
        }
        long daysToExpiry = current.expiresAt() == null
                ? Long.MAX_VALUE
                : ChronoUnit.DAYS.between(LocalDate.now(), current.expiresAt());
        return new LicenseInfo(
                status,
                status.writable(),
                describe(status, current),
                current.customer(),
                current.edition(),
                current.licenseId(),
                current.issuedAt(),
                current.expiresAt(),
                current.graceDaysOrDefault(),
                daysToExpiry,
                current.maxAccounts(),
                current.maxStudents(),
                current.maxCampuses(),
                current.features() == null ? List.of() : current.features(),
                machine,
                current.fingerprint()
        );
    }

    // ---- 内部 ----

    private void applyLicenseText(String licenseText, boolean alreadyVerified) {
        if (!StringUtils.hasText(licenseText)) {
            this.payload = null;
            this.signatureRejected = false;
            log.warn("未检测到授权，系统处于未授权（只读）状态，机器码={}", fingerprintProvider.fingerprint());
            return;
        }
        LicensePayload parsed = verify(licenseText);
        if (parsed == null) {
            this.payload = null;
            this.signatureRejected = true;
            log.error("授权验签失败，系统处于只读状态");
            return;
        }
        this.payload = parsed;
        this.signatureRejected = false;
        if (!alreadyVerified) {
            log.info("授权加载成功 licenseId={} customer={} status={}", parsed.licenseId(), parsed.customer(), currentStatus());
        }
    }

    private LicensePayload verify(String licenseText) {
        String json = LicenseSignature.verifyAndExtract(licenseText, properties.getPublicKey());
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LicensePayload.class);
        } catch (Exception e) {
            log.error("授权载荷解析失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean fingerprintMatches(LicensePayload current) {
        String bound = current.fingerprint();
        if (!StringUtils.hasText(bound)) {
            return true; // 未绑定：任意部署可用
        }
        return bound.trim().equalsIgnoreCase(fingerprintProvider.fingerprint());
    }

    private String readFromDatabase() {
        try {
            List<String> rows = jdbcTemplate.queryForList(
                    "select license_text from system_license where id = ?", String.class, ROW_ID);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("读取授权记录失败（可能表尚未创建）: {}", e.getMessage());
            return null;
        }
    }

    private String importFromFileIfPresent() {
        String filePath = properties.getFile();
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                return null;
            }
            String text = Files.readString(path).trim();
            LicensePayload parsed = verify(text);
            if (parsed != null) {
                persist(text, parsed, null);
                log.info("已从文件导入授权: {}", filePath);
            }
            return text;
        } catch (Exception e) {
            log.warn("从文件加载授权失败 {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    private void persist(String licenseText, LicensePayload parsed, Long accountId) {
        jdbcTemplate.update("""
                insert into system_license (id, license_text, license_id, customer, expires_at, uploaded_by)
                values (?, ?, ?, ?, ?, ?)
                on duplicate key update
                    license_text = values(license_text),
                    license_id   = values(license_id),
                    customer     = values(customer),
                    expires_at   = values(expires_at),
                    uploaded_by  = values(uploaded_by),
                    uploaded_at  = current_timestamp
                """,
                ROW_ID,
                licenseText,
                parsed.licenseId(),
                parsed.customer(),
                parsed.expiresAt() == null ? null : java.sql.Date.valueOf(parsed.expiresAt()),
                accountId
        );
    }

    private String describe(LicenseStatus status, LicensePayload current) {
        return switch (status) {
            case VALID -> "授权有效";
            case GRACE -> "授权已到期，正处于宽限期，请尽快联系厂商续期";
            case EXPIRED -> "授权已过期且超过宽限期，系统已进入只读状态，请联系厂商续期";
            case INVALID -> current != null && !fingerprintMatches(current)
                    ? "授权与本机不匹配（本机机器码 " + fingerprintProvider.fingerprint() + "），系统只读"
                    : "授权文件无效或被篡改，系统只读";
            case UNLICENSED -> "系统尚未激活授权，处于只读状态，请上传授权文件（本机机器码 "
                    + fingerprintProvider.fingerprint() + "）";
        };
    }
}
