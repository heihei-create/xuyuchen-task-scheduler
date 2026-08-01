package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OutboxService {
    private final CopyOnWriteArrayList<OutboxEvent> events = new CopyOnWriteArrayList<>();
    public OutboxEvent append(UUID aggregateId, String tenantId, String topic, String payload) {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), aggregateId, tenantId, topic, payload, Instant.now(), null, 0, null);
        events.add(event); return event;
    }
    public List<OutboxEvent> pending(int limit) { return events.stream().filter(e -> e.publishedAt() == null).limit(limit).toList(); }
    public void markPublished(UUID id) { replace(id, events.stream().filter(e -> e.id().equals(id)).findFirst().map(OutboxEvent::published).orElseThrow()); }
    public void markFailed(UUID id, String error) { replace(id, events.stream().filter(e -> e.id().equals(id)).findFirst().map(e -> e.retry(error)).orElseThrow()); }
    private void replace(UUID id, OutboxEvent value) { events.removeIf(e -> e.id().equals(id)); events.add(value); }
}
