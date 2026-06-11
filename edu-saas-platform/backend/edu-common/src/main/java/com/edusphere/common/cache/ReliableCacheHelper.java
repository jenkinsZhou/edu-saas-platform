package com.edusphere.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ReliableCacheHelper {

    private static final Logger log = LoggerFactory.getLogger(ReliableCacheHelper.class);
    private final StringRedisTemplate redis;

    public ReliableCacheHelper(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void evictWithDelay(String key, long delayMillis) {
        try {
            redis.delete(key);
            log.debug("立即删除缓存: {}", key);

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(delayMillis);
                    redis.delete(key);
                    log.debug("延迟删除缓存: {} (延迟{}ms)", key, delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("延迟删除缓存被中断: {}", key, e);
                } catch (Exception e) {
                    log.error("延迟删除缓存失败: {}", key, e);
                }
            });
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
        }
    }

    public void evictPattern(String pattern) {
        try {
            redis.keys(pattern).forEach(redis::delete);
            log.debug("批量删除缓存: {}", pattern);
        } catch (Exception e) {
            log.error("批量删除缓存失败: {}", pattern, e);
        }
    }
}
