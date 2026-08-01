package com.xuyuchen.scheduler.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "scheduler.lock", havingValue = "redis")
public class RedisTaskLock implements DistributedLock {
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    private final StringRedisTemplate redis;
    public RedisTaskLock(StringRedisTemplate redis) { this.redis = redis; }
    @Override public boolean tryAcquire(String key, String owner, Duration ttl) {
        Boolean success = redis.opsForValue().setIfAbsent(key, owner, ttl);
        return Boolean.TRUE.equals(success);
    }
    @Override public boolean release(String key, String owner) {
        Long released = redis.execute(RELEASE_SCRIPT, java.util.List.of(key), owner);
        return Long.valueOf(1L).equals(released);
    }
}
