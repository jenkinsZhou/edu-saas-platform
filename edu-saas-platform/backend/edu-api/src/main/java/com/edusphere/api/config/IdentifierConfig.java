package com.edusphere.api.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class IdentifierConfig {

    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new SafeIdentifierGenerator();
    }

    private static final class SafeIdentifierGenerator implements IdentifierGenerator {

        private final AtomicLong sequence = new AtomicLong(0);
        private volatile long lastTimestamp;

        @Override
        public synchronized Number nextId(Object entity) {
            long current = System.currentTimeMillis();
            long last = lastTimestamp;
            if (current == last) {
                long seq = sequence.incrementAndGet();
                if (seq >= 1000) {
                    current = waitNextMillis(current);
                    sequence.set(0);
                }
            } else {
                lastTimestamp = current;
                sequence.set(0);
            }
            return current * 1000 + sequence.get();
        }

        private long waitNextMillis(long current) {
            long next = System.currentTimeMillis();
            while (next <= current) {
                next = System.currentTimeMillis();
            }
            lastTimestamp = next;
            return next;
        }
    }
}
