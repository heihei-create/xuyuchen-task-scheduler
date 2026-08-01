package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "scheduler.lock", havingValue = "memory", matchIfMissing = true)
public class InMemoryDistributedLock implements DistributedLock {
    private record Lock(String owner, Instant expiresAt) {}
    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    @Override public boolean tryAcquire(String key, String owner, Duration ttl) {
        Instant now = Instant.now();
        return locks.compute(key, (k, current) -> {
            if (current == null || current.expiresAt().isBefore(now)) return new Lock(owner, now.plus(ttl));
            return current;
        }).owner().equals(owner);
    }
    @Override public boolean release(String key, String owner) {
        Lock current = locks.get(key);
        return current != null && current.owner().equals(owner) && locks.remove(key, current);
    }
}
