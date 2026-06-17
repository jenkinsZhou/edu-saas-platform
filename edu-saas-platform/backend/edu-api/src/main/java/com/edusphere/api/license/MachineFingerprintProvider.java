package com.edusphere.api.license;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 计算当前部署的机器指纹（机器码），用于授权“部署绑定”。
 *
 * <p>容器/虚拟机环境网卡可能变动，因此支持通过 {@code edu.license.fingerprint} 环境变量显式固定，
 * 优先级最高。未显式指定时基于网卡 MAC 地址集合做稳定哈希。</p>
 *
 * <p>激活流程：客户安装后在授权页看到机器码 → 反馈给厂商 → 厂商签发绑定该机器码的授权 → 客户上传激活。</p>
 */
@Component
public class MachineFingerprintProvider {

    private final String overrideFingerprint;
    private volatile String cached;

    public MachineFingerprintProvider(@Value("${edu.license.fingerprint:}") String overrideFingerprint) {
        this.overrideFingerprint = overrideFingerprint;
    }

    public String fingerprint() {
        if (StringUtils.hasText(overrideFingerprint)) {
            return overrideFingerprint.trim();
        }
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cached == null) {
                cached = computeFromHardware();
            }
            return cached;
        }
    }

    private String computeFromHardware() {
        try {
            List<String> macs = new ArrayList<>();
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                byte[] mac = ni.getHardwareAddress();
                if (mac == null || mac.length == 0) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) {
                    sb.append(String.format("%02X", b));
                }
                macs.add(sb.toString());
            }
            Collections.sort(macs);
            String seed = macs.isEmpty() ? "no-hardware-address" : String.join("|", macs);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02X", hash[i]));
            }
            // 形如 1A2B-3C4D-5E6F-7A8B，便于客户口头/邮件转述
            return hex.toString().replaceAll("(.{4})(?!$)", "$1-");
        } catch (Exception e) {
            return "UNKNOWN-FINGERPRINT";
        }
    }
}
