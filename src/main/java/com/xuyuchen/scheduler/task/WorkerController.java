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
    public WorkerResponse register(@Valid @RequestBody RegisterRequest req) {
        return WorkerResponse.from(registry.register(req.workerId(), req.host(), req.capabilities() == null ? java.util.Set.of() : req.capabilities()));
    }
    @PostMapping("/{workerId}/heartbeat")
    public WorkerResponse heartbeat(@PathVariable String workerId, @Valid @RequestBody HeartbeatRequest req) { return WorkerResponse.from(registry.heartbeat(workerId, req.runningTasks())); }
    @PostMapping("/{workerId}/drain")
    public WorkerResponse drain(@PathVariable String workerId) { registry.drain(workerId); return WorkerResponse.from(registry.require(workerId)); }
    @GetMapping
    public List<WorkerResponse> list() { return registry.list().stream().map(WorkerResponse::from).toList(); }
}
