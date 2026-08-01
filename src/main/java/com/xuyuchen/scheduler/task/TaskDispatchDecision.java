package com.xuyuchen.scheduler.task;

import java.util.UUID;

public record TaskDispatchDecision(UUID taskId, String tenantId, String executorType, String workerId, int attempt, String reason) {}
