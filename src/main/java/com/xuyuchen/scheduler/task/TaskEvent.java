package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TaskEvent(UUID taskId, String tenantId, TaskEventType type, int attempt, String workerId, String traceId, Map<String, Object> payload, Instant occurredAt) {
    public static TaskEvent of(Task task, TaskEventType type, String workerId, String traceId, Map<String, Object> payload) {
        return new TaskEvent(task.getId(), task.getTenantId(), type, task.getAttempt(), workerId, traceId, payload, Instant.now());
    }
}
