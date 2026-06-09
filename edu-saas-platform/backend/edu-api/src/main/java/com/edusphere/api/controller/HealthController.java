package com.edusphere.api.controller;

import com.edusphere.common.api.ApiResult;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of("status", "UP", "time", Instant.now().toString()));
    }

    @GetMapping("/live")
    public ApiResult<Map<String, String>> live() {
        return ApiResult.ok(Map.of("status", "UP", "time", Instant.now().toString()));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResult<Map<String, Object>>> ready() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean databaseUp = databaseReady();
        boolean redisUp = redisReady();
        checks.put("database", databaseUp ? "UP" : "DOWN");
        checks.put("redis", redisUp ? "UP" : "DOWN");
        checks.put("time", Instant.now().toString());
        boolean ready = databaseUp && redisUp;
        checks.put("status", ready ? "UP" : "DOWN");
        HttpStatus status = ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(ApiResult.ok(checks));
    }

    private boolean databaseReady() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            return result != null && result == 1;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private boolean redisReady() {
        try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (RedisConnectionFailureException | NullPointerException ex) {
            return false;
        }
    }
}
