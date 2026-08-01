package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WorkerProgressRequest(@NotBlank String workerId, int attempt, @NotBlank String leaseToken, @Min(0) @Max(100) int percent, String phase, String message) {}
