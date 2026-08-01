package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/task-operations")
public class TaskOperationsController {
    private final TaskRetryService retries;
    private final TaskLifecycleService lifecycle;
    private final TaskQueryService queries;
    public TaskOperationsController(TaskRetryService retries, TaskLifecycleService lifecycle, TaskQueryService queries) {
        this.retries = retries; this.lifecycle = lifecycle; this.queries = queries;
    }
    @PostMapping("/{taskId}/retry")
    public TaskDtos.TaskResponse retry(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) {
        return TaskDtos.TaskResponse.from(retries.retry(tenantId, taskId));
    }
    @PostMapping("/{taskId}/dispatch")
    public TaskDtos.TaskResponse dispatch(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) {
        return TaskDtos.TaskResponse.from(lifecycle.dispatch(tenantId, taskId));
    }
    @GetMapping("/{taskId}")
    public TaskDtos.TaskResponse get(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) {
        return TaskDtos.TaskResponse.from(queries.require(tenantId, taskId));
    }
}
