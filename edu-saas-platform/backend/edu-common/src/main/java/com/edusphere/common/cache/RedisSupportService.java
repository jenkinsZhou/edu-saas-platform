package com.edusphere.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class RedisSupportService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, LocalValue> localCache = new ConcurrentHashMap<>();
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    public RedisSupportService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean allow(String key, long limit, Duration window) {
        try {
            Long current = stringRedisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                stringRedisTemplate.expire(key, window);
            }
            return current != null && current <= limit;
        } catch (Exception ex) {
            return allowLocal(key, limit, window);
        }
    }

    private boolean allowLocal(String key, long limit, Duration window) {
        long now = System.currentTimeMillis();
        LocalCounter counter = localCounters.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt <= now) {
                return new LocalCounter(1, now + window.toMillis());
            }
            current.count++;
            return current;
        });
        return counter != null && counter.count <= limit;
    }

    public boolean reserveOnce(String key, Duration ttl) {
        try {
            Boolean reserved = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(reserved);
        } catch (Exception ex) {
            long expiresAt = System.currentTimeMillis() + ttl.toMillis();
            LocalValue current = localCache.get(key);
            if (current != null && current.expiresAt > System.currentTimeMillis()) {
                return false;
            }
            localCache.put(key, new LocalValue("1", expiresAt));
            return true;
        }
    }

    public <T> T getOrLoadJson(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, type);
            }
            T value = loader.get();
            if (value != null) {
                stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
            }
            return value;
        } catch (Exception ex) {
            LocalValue cached = localCache.get(key);
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                try {
                    return objectMapper.readValue(cached.value, type);
                } catch (Exception ignored) {
                    localCache.remove(key);
                }
            }
            T value = loader.get();
            if (value != null) {
                try {
                    localCache.put(key, new LocalValue(objectMapper.writeValueAsString(value), System.currentTimeMillis() + ttl.toMillis()));
                } catch (Exception ignored) {
                }
            }
            return value;
        }
    }

    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }
        localCache.remove(key);
        localCounters.remove(key);
    }

    public void putString(String key, String value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ignored) {
            localCache.put(key, new LocalValue(value, System.currentTimeMillis() + ttl.toMillis()));
        }
    }

    public String getString(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
            LocalValue cached = localCache.get(key);
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                return cached.value;
            }
            return null;
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception ignored) {
            LocalValue cached = localCache.get(key);
            return cached != null && cached.expiresAt > System.currentTimeMillis();
        }
    }

    public void evictByPrefix(String prefix) {
        try {
            Set<String> keys = stringRedisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
        localCache.keySet().removeIf(key -> key.startsWith(prefix));
        localCounters.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private record LocalValue(String value, long expiresAt) {
    }

    private static class LocalCounter {
        private long count;
        private final long expiresAt;

        private LocalCounter(long count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
