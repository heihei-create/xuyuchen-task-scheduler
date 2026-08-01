package com.xuyuchen.scheduler.task;

import java.time.Instant;

public class TaskAttempt {
    private final int attempt;
    private final String workerId;
    private final Instant dispatchedAt;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile String error;

    public TaskAttempt(int attempt, String workerId) { this.attempt = attempt; this.workerId = workerId; this.dispatchedAt = Instant.now(); }
    public int getAttempt() { return attempt; }
    public String getWorkerId() { return workerId; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getError() { return error; }
    public void markStarted() { startedAt = Instant.now(); }
    public void markFinished(String error) { finishedAt = Instant.now(); this.error = error; }
    public boolean finished() { return finishedAt != null; }
}
