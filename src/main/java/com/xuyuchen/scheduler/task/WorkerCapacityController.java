package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/worker-capacity")
public class WorkerCapacityController {
    private final WorkerCapacityService service;
    public WorkerCapacityController(WorkerCapacityService service) { this.service = service; }
    public record CapacityRequest(@Min(1) int capacity) {}
    @PutMapping("/{workerId}")
    public Map<String, Object> put(@PathVariable String workerId, @RequestBody CapacityRequest req) { return Map.of("workerId", workerId, "capacity", service.configure(workerId, req.capacity())); }
    @GetMapping("/{workerId}")
    public Map<String, Object> get(@PathVariable String workerId) { return Map.of("workerId", workerId, "capacity", service.capacity(workerId)); }
}
