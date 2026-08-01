package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(UUID id, UUID aggregateId, String tenantId, String topic, String payload, Instant createdAt, Instant publishedAt, int attempts, String lastError) {
    public OutboxEvent retry(String error) { return new OutboxEvent(id, aggregateId, tenantId, topic, payload, createdAt, null, attempts + 1, error); }
    public OutboxEvent published() { return new OutboxEvent(id, aggregateId, tenantId, topic, payload, createdAt, Instant.now(), attempts, null); }
}
