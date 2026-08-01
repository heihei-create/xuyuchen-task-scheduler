package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public record TaskResult(UUID taskId, int attempt, boolean success, String summary, String outputKey, String checksum, Instant createdAt) {}
