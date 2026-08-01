package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Map;

@Service
public class SchedulerHealthService {
    private final WorkerRegistry workers;
    private final ObjectProvider<InMemoryTaskEventPublisher> events;
    private final ObjectProvider<InMemoryWorkerCommandPublisher> commands;
    private final SchedulerMetrics metrics;
    public SchedulerHealthService(WorkerRegistry workers, ObjectProvider<InMemoryTaskEventPublisher> events, ObjectProvider<InMemoryWorkerCommandPublisher> commands, SchedulerMetrics metrics) {
        this.workers = workers; this.events = events; this.commands = commands; this.metrics = metrics;
    }
    public Map<String, Object> snapshot() {
        long alive = workers.list().stream().filter(w -> w.alive(30)).count();
        InMemoryTaskEventPublisher eventPublisher = events.getIfAvailable();
        InMemoryWorkerCommandPublisher commandPublisher = commands.getIfAvailable();
        return Map.of(
                "status", alive > 0 || workers.list().isEmpty() ? "UP" : "DEGRADED",
                "checkedAt", Instant.now(),
                "workers", workers.list().size(),
                "aliveWorkers", alive,
                "queuedEvents", eventPublisher == null ? 0 : eventPublisher.recent(100).size(),
                "queuedCommands", commandPublisher == null ? 0 : commandPublisher.commands().size(),
                "metrics", metrics.snapshot()
        );
    }
}
