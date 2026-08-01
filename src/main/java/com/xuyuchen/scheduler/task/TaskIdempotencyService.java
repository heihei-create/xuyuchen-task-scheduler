package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskIdempotencyService {
    private record Entry(UUID taskId, Instant expiresAt) {}
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    public UUID find(String tenantId, String key) {
        Entry entry = entries.get(tenantId + ":" + key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) return null;
        return entry.taskId();
    }
    public UUID put(String tenantId, String key, UUID taskId, Duration ttl) {
        String composite = tenantId + ":" + key;
        Entry candidate = new Entry(taskId, Instant.now().plus(ttl));
        Entry existing = entries.putIfAbsent(composite, candidate);
        return existing == null ? taskId : existing.taskId();
    }
    public void remove(String tenantId, String key) { entries.remove(tenantId + ":" + key); }
    public int size() { return entries.size(); }
}
