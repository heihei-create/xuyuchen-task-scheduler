package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/results")
public class TaskResultController {
    private final TaskResultService service;
    private final TaskQueryService queries;
    public TaskResultController(TaskResultService service, TaskQueryService queries) { this.service = service; this.queries = queries; }
    @GetMapping("/{taskId}")
    public List<TaskResult> list(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { queries.require(tenantId, taskId); return service.list(taskId); }
    @GetMapping("/{taskId}/latest")
    public TaskResult latest(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { queries.require(tenantId, taskId); return service.latest(taskId); }
}
