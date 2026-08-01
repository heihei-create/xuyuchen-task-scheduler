package com.xuyuchen.scheduler.task;

import java.time.Instant;
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
    private volatile String workerId;
    private volatile String result;

    public Task(UUID id, String tenantId, String name, String payload, String idempotencyKey) {
        this.id = id; this.tenantId = tenantId; this.name = name; this.payload = payload;
        this.idempotencyKey = idempotencyKey; this.createdAt = Instant.now(); this.status = TaskStatus.CREATED;
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
        workerId = null;
        return true;
    }
    public synchronized boolean dispatch(String workerId, long leaseSeconds) {
        if (!move(TaskStatus.READY, TaskStatus.DISPATCHED)) return false;
        this.workerId = workerId; this.attempt++; this.leaseUntil = Instant.now().plusSeconds(leaseSeconds);
        return true;
    }
    public synchronized boolean start(String workerId, int attempt) {
        if (!this.workerId.equals(workerId) || this.attempt != attempt) return false;
        return move(TaskStatus.DISPATCHED, TaskStatus.RUNNING);
    }
    public synchronized boolean finish(String workerId, int attempt, boolean success, String result) {
        if (!this.workerId.equals(workerId) || this.attempt != attempt) return false;
        if (status != TaskStatus.RUNNING && status != TaskStatus.DISPATCHED) return false;
        this.result = result; this.status = success ? TaskStatus.SUCCESS : TaskStatus.FAILED; this.leaseUntil = null;
        return true;
    }
    public synchronized boolean timeout() {
        if (leaseUntil == null || leaseUntil.isAfter(Instant.now())) return false;
        if (status != TaskStatus.RUNNING && status != TaskStatus.DISPATCHED) return false;
        status = TaskStatus.TIMEOUT; return true;
    }
    public synchronized boolean cancel() {
        if (status == TaskStatus.SUCCESS || status == TaskStatus.FAILED || status == TaskStatus.TIMEOUT || status == TaskStatus.CANCELED) return false;
        status = TaskStatus.CANCELED; leaseUntil = null; return true;
    }
}
