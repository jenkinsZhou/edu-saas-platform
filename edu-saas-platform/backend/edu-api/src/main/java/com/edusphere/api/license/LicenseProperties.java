package com.edusphere.api.license;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 授权相关配置（前缀 {@code edu.license}）。
 */
@Component
@ConfigurationProperties(prefix = "edu.license")
public class LicenseProperties {

    /** 是否启用授权校验与拦截。私有化交付应为 true；本地开发可设 false 关闭。 */
    private boolean enabled = true;

    /** 厂商签发授权所用密钥对的公钥（X.509, Base64）。为空则无法验签，系统视为未授权。 */
    private String publicKey = "";

    /** 启动时若数据库无授权，则尝试从该文件加载（便于随交付包附带授权文件）。 */
    private String file = "";

    /**
     * 功能模块开关：URL 路径前缀 → 模块功能码。
     * 命中的路径仅当授权 features 含该功能码时放行，否则 403。未命中的路径视为核心模块，始终放行。
     */
    private Map<String, String> featurePaths = defaultFeaturePaths();

    private static Map<String, String> defaultFeaturePaths() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("/api/attendance", "attendance");
        map.put("/api/classrooms", "classroom");
        map.put("/api/scheduling", "scheduling");
        map.put("/api/course-consumption", "consumption");
        map.put("/api/class-transfer", "transfer");
        map.put("/api/teachers", "teacher");
        map.put("/api/coupons", "marketing");
        map.put("/api/contracts", "contract");
        map.put("/api/notifications", "notification");
        map.put("/api/reports", "report");
        return map;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Map<String, String> getFeaturePaths() {
        return featurePaths;
    }

    public void setFeaturePaths(Map<String, String> featurePaths) {
        this.featurePaths = featurePaths;
    }
}
