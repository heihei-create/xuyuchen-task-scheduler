package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class SchedulerHealthService {
    private final WorkerRegistry workers;
    private final InMemoryTaskEventPublisher events;
    private final InMemoryWorkerCommandPublisher commands;
    private final SchedulerMetrics metrics;
    public SchedulerHealthService(WorkerRegistry workers, InMemoryTaskEventPublisher events, InMemoryWorkerCommandPublisher commands, SchedulerMetrics metrics) {
        this.workers = workers; this.events = events; this.commands = commands; this.metrics = metrics;
    }
    public Map<String, Object> snapshot() {
        long alive = workers.list().stream().filter(w -> w.alive(30)).count();
        return Map.of(
                "status", alive > 0 || workers.list().isEmpty() ? "UP" : "DEGRADED",
                "checkedAt", Instant.now(),
                "workers", workers.list().size(),
                "aliveWorkers", alive,
                "queuedEvents", events.recent(100).size(),
                "queuedCommands", commands.commands().size(),
                "metrics", metrics.snapshot()
        );
    }
}
