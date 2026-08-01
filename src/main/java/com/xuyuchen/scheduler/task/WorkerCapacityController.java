package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/worker-capacity")
public class WorkerCapacityController {
    private final WorkerCapacityService service;
    private final WorkerRegistry workers;
    public WorkerCapacityController(WorkerCapacityService service, WorkerRegistry workers) { this.service = service; this.workers = workers; }
    public record CapacityRequest(@Min(1) int capacity) {}
    @PutMapping("/{workerId}")
    public Map<String, Object> put(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String workerId, @Valid @RequestBody CapacityRequest req) { WorkerInfo worker = workers.require(workerId); if (!worker.getTenantId().equals(tenantId)) throw new IllegalArgumentException("worker not found"); return Map.of("workerId", workerId, "capacity", service.configure(tenantId, workerId, req.capacity())); }
    @GetMapping("/{workerId}")
    public Map<String, Object> get(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String workerId) { WorkerInfo worker = workers.require(workerId); if (!worker.getTenantId().equals(tenantId)) throw new IllegalArgumentException("worker not found"); return Map.of("workerId", workerId, "capacity", service.capacity(tenantId, workerId)); }
}
