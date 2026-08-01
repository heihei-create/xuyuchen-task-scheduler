package com.xuyuchen.scheduler.task;

import java.time.Duration;

public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {
    public RetryPolicy {
        if (maxAttempts < 1 || initialDelay.isNegative() || multiplier < 1 || maxDelay.isNegative()) throw new IllegalArgumentException("invalid retry policy");
    }
    public Duration delayFor(int attempt) {
        double factor = Math.pow(multiplier, Math.max(0, attempt - 1));
        long millis = Math.min(maxDelay.toMillis(), (long) (initialDelay.toMillis() * factor));
        return Duration.ofMillis(millis);
    }
    public boolean canRetry(int attempt) { return attempt < maxAttempts; }
}
