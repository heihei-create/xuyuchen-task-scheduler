package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class TaskDtos {
    private TaskDtos() {}
    public record CreateTaskRequest(@NotBlank String name, String payload, @NotBlank String idempotencyKey) {}
    public record WorkerEventRequest(@NotBlank String workerId, int attempt, boolean success, String result) {}
    public record TaskResponse(UUID id, String tenantId, String name, TaskStatus status, int attempt, Instant createdAt, Instant leaseUntil, String workerId, String result) {
        static TaskResponse from(Task t) { return new TaskResponse(t.getId(), t.getTenantId(), t.getName(), t.getStatus(), t.getAttempt(), t.getCreatedAt(), t.getLeaseUntil(), t.getWorkerId(), t.getResult()); }
    }
}
