package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public record TaskLease(UUID taskId, String workerId, int attempt, Instant issuedAt, Instant expiresAt, String token) {
    public boolean expired() { return expiresAt.isBefore(Instant.now()); }
    public boolean matches(String worker, int currentAttempt, String currentToken) {
        return workerId.equals(worker) && attempt == currentAttempt && token.equals(currentToken) && !expired();
    }
}
