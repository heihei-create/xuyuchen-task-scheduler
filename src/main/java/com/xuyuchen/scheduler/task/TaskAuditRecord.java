package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TaskAuditRecord(UUID taskId, String tenantId, String operator, TaskStatus from, TaskStatus to, String reason, String traceId, Instant createdAt, Map<String, Object> details) {}
