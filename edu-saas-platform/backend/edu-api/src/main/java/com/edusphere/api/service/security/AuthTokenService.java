package com.edusphere.api.service.security;

import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AuthTokenService {

    private final RedisSupportService redisSupportService;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenService(
            RedisSupportService redisSupportService,
            @Value("${edu.security.jwt.refresh-token-days:14}") long refreshTokenDays
    ) {
        this.redisSupportService = redisSupportService;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenDays);
    }

    public String issueRefreshToken(Long tenantId, Long accountId) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redisSupportService.putString(refreshKey(token), tenantId + ":" + accountId, refreshTokenTtl);
        return token;
    }

    public RefreshTokenSubject requireRefreshToken(String refreshToken) {
        String stored = redisSupportService.getString(refreshKey(refreshToken));
        if (stored == null || stored.isBlank()) {
            throw new BizException(401, "登录已过期，请重新登录");
        }
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            revokeRefreshToken(refreshToken);
            throw new BizException(401, "登录已过期，请重新登录");
        }
        return new RefreshTokenSubject(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        redisSupportService.evict(refreshKey(refreshToken));
    }

    private String refreshKey(String token) {
        return "auth:refresh:" + sha256(token == null ? "" : token);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    public record RefreshTokenSubject(Long tenantId, Long accountId) {
    }
}
