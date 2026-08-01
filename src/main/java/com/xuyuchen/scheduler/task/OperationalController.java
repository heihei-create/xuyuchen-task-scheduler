package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/scheduler")
public class OperationalController {
    private final SchedulerHealthService health;
    private final SchedulerMetrics metrics;
    public OperationalController(SchedulerHealthService health, SchedulerMetrics metrics) { this.health = health; this.metrics = metrics; }
    @GetMapping("/health")
    public Map<String, Object> health() { return health.snapshot(); }
    @GetMapping("/metrics")
    public SchedulerMetrics.SchedulerMetricSnapshot metrics() { return metrics.snapshot(); }
}
