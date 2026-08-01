package com.xuyuchen.scheduler.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.xuyuchen.scheduler.task.WorkerDtos.*;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {
    private final WorkerRegistry registry;
    public WorkerController(WorkerRegistry registry) { this.registry = registry; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkerResponse register(@RequestHeader("X-Tenant-Id") String tenantId, @Valid @RequestBody RegisterRequest req) {
        return WorkerResponse.from(registry.register(tenantId, req.workerId(), req.host(), req.capabilities() == null ? java.util.Set.of() : req.capabilities()));
    }
    @PostMapping("/{workerId}/heartbeat")
    public WorkerResponse heartbeat(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String workerId, @Valid @RequestBody HeartbeatRequest req) { WorkerInfo worker = registry.require(workerId); if (!worker.getTenantId().equals(tenantId)) throw new IllegalArgumentException("worker not found"); return WorkerResponse.from(registry.heartbeat(workerId, req.runningTasks())); }
    @PostMapping("/{workerId}/drain")
    public WorkerResponse drain(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable String workerId) { WorkerInfo worker = registry.require(workerId); if (!worker.getTenantId().equals(tenantId)) throw new IllegalArgumentException("worker not found"); registry.drain(workerId); return WorkerResponse.from(registry.require(workerId)); }
    @GetMapping
    public List<WorkerResponse> list(@RequestHeader("X-Tenant-Id") String tenantId) { return registry.list().stream().filter(worker -> worker.getTenantId().equals(tenantId)).map(WorkerResponse::from).toList(); }
}
