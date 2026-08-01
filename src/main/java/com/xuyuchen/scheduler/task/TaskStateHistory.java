package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public record TaskStateHistory(UUID taskId, TaskStatus from, TaskStatus to, int attempt, String actor, String reason, Instant changedAt) {}
