package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.NotBlank;

public record WorkerHeartbeatRequest(@NotBlank String workerId, int attempt, @NotBlank String leaseToken, int runningTasks) {}
