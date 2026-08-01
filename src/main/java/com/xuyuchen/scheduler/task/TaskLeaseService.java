package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskLeaseService {
    private final TaskRepository tasks;
    private final Map<UUID, TaskLease> leases = new ConcurrentHashMap<>();
    public TaskLeaseService(TaskRepository tasks) { this.tasks = tasks; }
    public TaskLease issue(UUID taskId, String workerId, int attempt, long seconds) {
        Instant now = Instant.now();
        TaskLease lease = new TaskLease(taskId, workerId, attempt, now, now.plusSeconds(seconds), UUID.randomUUID().toString());
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        task.assignLeaseToken(lease.token()); task.renewLease(lease.expiresAt()); tasks.save(task);
        leases.put(taskId, lease); return lease;
    }
    public TaskLease require(UUID taskId) {
        TaskLease inMemory = leases.get(taskId);
        if (inMemory != null) return inMemory;
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalStateException("task lease missing"));
        if (task.getLeaseToken() == null || task.getLeaseUntil() == null || task.getWorkerId() == null) throw new IllegalStateException("task lease missing");
        TaskLease restored = new TaskLease(taskId, task.getWorkerId(), task.getAttempt(), task.getCreatedAt(), task.getLeaseUntil(), task.getLeaseToken());
        leases.putIfAbsent(taskId, restored);
        return leases.get(taskId);
    }
    public TaskLease renew(UUID taskId, String workerId, int attempt, String token, long seconds) {
        TaskLease current = require(taskId);
        if (!current.matches(workerId, attempt, token)) throw new IllegalStateException("invalid task lease");
        Instant now = Instant.now();
        TaskLease next = new TaskLease(taskId, workerId, attempt, current.issuedAt(), now.plusSeconds(seconds), token);
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        task.renewLease(next.expiresAt()); tasks.save(task);
        leases.put(taskId, next); return next;
    }
    public void release(UUID taskId, String workerId, int attempt, String token) {
        TaskLease current = require(taskId);
        if (!current.matches(workerId, attempt, token) && !current.expired()) throw new IllegalStateException("invalid task lease release");
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        task.clearLease(); tasks.save(task); leases.remove(taskId);
    }
    public boolean expired(UUID taskId) {
        Task task = tasks.findById(taskId).orElse(null);
        return task != null && task.getLeaseUntil() != null && !task.getLeaseUntil().isAfter(Instant.now());
    }
    public void forget(UUID taskId) { leases.remove(taskId); }
}
