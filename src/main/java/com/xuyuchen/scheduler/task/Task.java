package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Task {
    private final UUID id;
    private final String tenantId;
    private final String name;
    private final String payload;
    private final String idempotencyKey;
    private final Instant createdAt;
    private volatile TaskStatus status;
    private volatile int attempt;
    private volatile Instant leaseUntil;
    private volatile String leaseToken;
    private volatile String workerId;
    private volatile String result;

    public Task(UUID id, String tenantId, String name, String payload, String idempotencyKey) {
        this(id, tenantId, name, payload, idempotencyKey, Instant.now(), TaskStatus.CREATED, 0, null, null, null, null);
    }

    static Task restore(UUID id, String tenantId, String name, String payload, String idempotencyKey,
                        Instant createdAt, TaskStatus status, int attempt, Instant leaseUntil,
                        String workerId, String result, String leaseToken) {
        return new Task(id, tenantId, name, payload, idempotencyKey, createdAt, status, attempt, leaseUntil, workerId, result, leaseToken);
    }

    private Task(UUID id, String tenantId, String name, String payload, String idempotencyKey,
                 Instant createdAt, TaskStatus status, int attempt, Instant leaseUntil,
                 String workerId, String result, String leaseToken) {
        this.id = id; this.tenantId = tenantId; this.name = name; this.payload = payload;
        this.idempotencyKey = idempotencyKey; this.createdAt = createdAt; this.status = status;
        this.attempt = attempt; this.leaseUntil = leaseUntil; this.workerId = workerId; this.result = result; this.leaseToken = leaseToken;
    }
    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getPayload() { return payload; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public TaskStatus getStatus() { return status; }
    public int getAttempt() { return attempt; }
    public Instant getLeaseUntil() { return leaseUntil; }
    public String getLeaseToken() { return leaseToken; }
    public String getWorkerId() { return workerId; }
    public String getResult() { return result; }
    public synchronized boolean move(TaskStatus expected, TaskStatus next) {
        if (status != expected) return false;
        status = next;
        return true;
    }
    public synchronized boolean ready() {
        return status == TaskStatus.CREATED && move(TaskStatus.CREATED, TaskStatus.READY);
    }
    public synchronized boolean retry() {
        if (status != TaskStatus.FAILED && status != TaskStatus.TIMEOUT) return false;
        status = TaskStatus.READY;
        leaseUntil = null;
        leaseToken = null;
        workerId = null;
        return true;
    }
    public synchronized boolean dispatch(String workerId, long leaseSeconds) {
        if (!move(TaskStatus.READY, TaskStatus.DISPATCHED)) return false;
        this.workerId = workerId; this.attempt++; this.leaseUntil = Instant.now().plusSeconds(leaseSeconds);
        return true;
    }
    public synchronized boolean start(String workerId, int attempt) {
        if (!Objects.equals(this.workerId, workerId) || this.attempt != attempt) return false;
        return move(TaskStatus.DISPATCHED, TaskStatus.RUNNING);
    }
    public synchronized boolean finish(String workerId, int attempt, boolean success, String result) {
        if (!Objects.equals(this.workerId, workerId) || this.attempt != attempt) return false;
        if (status != TaskStatus.RUNNING && status != TaskStatus.DISPATCHED) return false;
        this.result = result; this.status = success ? TaskStatus.SUCCESS : TaskStatus.FAILED; this.leaseUntil = null; this.leaseToken = null;
        return true;
    }
    public synchronized boolean timeout() {
        if (leaseUntil == null || leaseUntil.isAfter(Instant.now())) return false;
        if (status != TaskStatus.RUNNING && status != TaskStatus.DISPATCHED) return false;
        status = TaskStatus.TIMEOUT; leaseUntil = null; leaseToken = null; return true;
    }
    public synchronized boolean cancel() {
        if (status == TaskStatus.SUCCESS || status == TaskStatus.FAILED || status == TaskStatus.TIMEOUT || status == TaskStatus.CANCELED) return false;
        status = TaskStatus.CANCELED; leaseUntil = null; leaseToken = null; return true;
    }

    synchronized void assignLeaseToken(String token) { this.leaseToken = token; }
    synchronized void renewLease(Instant expiresAt) { this.leaseUntil = expiresAt; }
    synchronized void clearLease() { this.leaseUntil = null; this.leaseToken = null; }

    synchronized boolean requeueAfterDispatchFailure() {
        if (status != TaskStatus.DISPATCHED) return false;
        status = TaskStatus.READY;
        leaseUntil = null;
        leaseToken = null;
        workerId = null;
        return true;
    }
}
