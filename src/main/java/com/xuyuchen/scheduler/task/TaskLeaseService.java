package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskLeaseService {
    private final Map<UUID, TaskLease> leases = new ConcurrentHashMap<>();
    public TaskLease issue(UUID taskId, String workerId, int attempt, long seconds) {
        Instant now = Instant.now();
        TaskLease lease = new TaskLease(taskId, workerId, attempt, now, now.plusSeconds(seconds), UUID.randomUUID().toString());
        leases.put(taskId, lease); return lease;
    }
    public TaskLease require(UUID taskId) { return java.util.Optional.ofNullable(leases.get(taskId)).orElseThrow(() -> new IllegalStateException("task lease missing")); }
    public TaskLease renew(UUID taskId, String workerId, int attempt, String token, long seconds) {
        TaskLease current = require(taskId);
        if (!current.matches(workerId, attempt, token)) throw new IllegalStateException("invalid task lease");
        Instant now = Instant.now();
        TaskLease next = new TaskLease(taskId, workerId, attempt, current.issuedAt(), now.plusSeconds(seconds), token);
        leases.put(taskId, next); return next;
    }
    public void release(UUID taskId, String workerId, int attempt, String token) {
        TaskLease current = require(taskId);
        if (!current.matches(workerId, attempt, token) && !current.expired()) throw new IllegalStateException("invalid task lease release");
        leases.remove(taskId);
    }
    public boolean expired(UUID taskId) { return leases.containsKey(taskId) && leases.get(taskId).expired(); }
}
