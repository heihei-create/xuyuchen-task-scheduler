package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public record TaskLogEntry(UUID taskId, int attempt, String level, String message, String traceId, Instant createdAt) {}
