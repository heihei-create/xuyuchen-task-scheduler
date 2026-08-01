package com.xuyuchen.scheduler.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "scheduler.lock", havingValue = "redis")
public class RedisTaskLock implements DistributedLock {
    private final StringRedisTemplate redis;
    public RedisTaskLock(StringRedisTemplate redis) { this.redis = redis; }
    @Override public boolean tryAcquire(String key, String owner, Duration ttl) {
        Boolean success = redis.opsForValue().setIfAbsent(key, owner, ttl);
        return Boolean.TRUE.equals(success);
    }
    @Override public boolean release(String key, String owner) {
        String current = redis.opsForValue().get(key);
        if (!owner.equals(current)) return false;
        redis.delete(key); return true;
    }
}
