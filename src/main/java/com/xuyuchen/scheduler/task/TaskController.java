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
    public TaskController(TaskService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@RequestHeader("X-Tenant-Id") String tenantId, @Valid @RequestBody CreateTaskRequest req) { return TaskResponse.from(service.create(tenantId, req)); }
    @GetMapping
    public List<TaskResponse> list(@RequestHeader("X-Tenant-Id") String tenantId) { return service.list(tenantId).stream().map(TaskResponse::from).toList(); }
    @GetMapping("/{id}")
    public TaskResponse get(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id) { return TaskResponse.from(service.get(tenantId, id)); }
    @PostMapping("/{id}/start")
    public TaskResponse start(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id, @Valid @RequestBody WorkerEventRequest req) { return TaskResponse.from(service.start(tenantId, id, req)); }
    @PostMapping("/{id}/finish")
    public TaskResponse finish(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id, @Valid @RequestBody WorkerEventRequest req) { return TaskResponse.from(service.finish(tenantId, id, req)); }
    @PostMapping("/{id}/cancel")
    public TaskResponse cancel(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id) { return TaskResponse.from(service.cancel(tenantId, id)); }
}
