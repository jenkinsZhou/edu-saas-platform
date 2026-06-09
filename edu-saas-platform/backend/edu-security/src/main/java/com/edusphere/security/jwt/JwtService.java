package com.edusphere.security.jwt;

import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.security.context.DataScope;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenMinutes;
    private final RedisSupportService redisSupportService;

    public JwtService(
            @Value("${edu.security.jwt.secret}") String secret,
            @Value("${edu.security.jwt.access-token-minutes:120}") long accessTokenMinutes,
            RedisSupportService redisSupportService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
        this.redisSupportService = redisSupportService;
    }

    public String createAccessToken(JwtClaims claims) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", claims.accountId());
        payload.put("userId", claims.userId());
        payload.put("tenantId", claims.tenantId());
        payload.put("roles", claims.roles());
        payload.put("permissions", claims.permissions());
        payload.put("dataScope", claims.dataScope().name());
        payload.put("campusIds", claims.campusIds());
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(claims.accountId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)))
                .claims(payload)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtClaims parseAccessToken(String token) {
        if (isAccessTokenRevoked(token)) {
            throw new IllegalArgumentException("Access token has been revoked");
        }
        Jws<Claims> jws = parseToken(token);
        Claims claims = jws.getPayload();
        List<String> roles = readStringList(claims, "roles");
        List<String> permissions = readStringList(claims, "permissions");
        List<Long> campusIds = readLongList(claims, "campusIds");
        return new JwtClaims(
                claims.get("accountId", Number.class).longValue(),
                claims.get("userId", Number.class).longValue(),
                claims.get("tenantId", Number.class).longValue(),
                roles,
                permissions,
                DataScope.valueOf(claims.get("dataScope", String.class)),
                campusIds
        );
    }

    public void revokeAccessToken(String token) {
        Jws<Claims> jws = parseToken(token);
        Claims claims = jws.getPayload();
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return;
        }
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        redisSupportService.putString(revokedKey(token), "1", Duration.ofMillis(ttlMillis));
    }

    public boolean isAccessTokenRevoked(String token) {
        return redisSupportService.exists(revokedKey(token));
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    private String revokedKey(String token) {
        return "jwt:revoked:" + sha256(token);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private List<String> readStringList(Claims claims, String key) {
        Object raw = claims.get(key);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).collect(Collectors.toList());
    }

    private List<Long> readLongList(Claims claims, String key) {
        Object raw = claims.get(key);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(item -> item instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(item)))
                .toList();
    }
}
