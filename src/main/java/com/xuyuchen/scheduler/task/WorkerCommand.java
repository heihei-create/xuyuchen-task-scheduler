package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkerCommand(UUID taskId, String tenantId, String name, String payload, int attempt, String workerId, String leaseToken, Instant issuedAt, Map<String, Object> metadata) {}
