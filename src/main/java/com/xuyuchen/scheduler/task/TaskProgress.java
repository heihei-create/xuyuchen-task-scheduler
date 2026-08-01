package com.xuyuchen.scheduler.task;

import java.time.Instant;

public record TaskProgress(String phase, int percent, String message, Instant updatedAt) {
    public TaskProgress {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("percent must be between 0 and 100");
    }
}
