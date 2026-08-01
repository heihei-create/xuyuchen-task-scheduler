package com.xuyuchen.scheduler.task;

import java.time.Duration;

public record SchedulePolicy(Duration executionTimeout, Duration heartbeatInterval, RetryPolicy retryPolicy, boolean allowManualRetry, boolean allowCancel) {
    public SchedulePolicy {
        if (executionTimeout.isNegative() || heartbeatInterval.isNegative()) throw new IllegalArgumentException("invalid schedule policy");
    }
    public static SchedulePolicy standard(int maxAttempts) {
        return new SchedulePolicy(Duration.ofMinutes(30), Duration.ofSeconds(10), new RetryPolicy(maxAttempts, Duration.ofSeconds(5), 2, Duration.ofMinutes(5)), true, true);
    }
}
