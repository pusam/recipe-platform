package com.recipeplatform.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 키 단위 토큰 버킷. 단일 인스턴스 기준(클러스터 환경이면 Redis로 전환 필요).
 */
@Slf4j
@Component
public class RateLimiter {

    @Value("${app.rate-limit.recipe-create.capacity:10}")
    private int capacity;

    @Value("${app.rate-limit.recipe-create.refill-per-minute:10}")
    private int refillPerMinute;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        return b.tryConsume(capacity, refillPerMinute);
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillNanos;

        Bucket(int initial) {
            this.tokens = initial;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume(int capacity, int refillPerMinute) {
            long now = System.nanoTime();
            double elapsedMin = (now - lastRefillNanos) / 60_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedMin * refillPerMinute);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
