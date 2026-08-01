package com.xuyuchen.scheduler.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.xuyuchen.scheduler.task.TaskDtos.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService service;
    private final TaskLifecycleService lifecycle;
    public TaskController(TaskService service, TaskLifecycleService lifecycle) { this.service = service; this.lifecycle = lifecycle; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@RequestHeader("X-Tenant-Id") String tenantId, @Valid @RequestBody CreateTaskRequest req) { return TaskResponse.from(lifecycle.create(tenantId, req.name(), req.payload(), req.idempotencyKey())); }
    @GetMapping
    public List<TaskResponse> list(@RequestHeader("X-Tenant-Id") String tenantId) { return service.list(tenantId).stream().map(TaskResponse::from).toList(); }
    @GetMapping("/{id}")
    public TaskResponse get(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id) { return TaskResponse.from(service.get(tenantId, id)); }
    @PostMapping("/{id}/cancel")
    public TaskResponse cancel(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id) { return TaskResponse.from(service.cancel(tenantId, id)); }
}
