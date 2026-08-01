package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/state-history")
public class TaskStateHistoryController {
    private final TaskStateHistoryService service;
    private final TaskQueryService queries;
    public TaskStateHistoryController(TaskStateHistoryService service, TaskQueryService queries) { this.service = service; this.queries = queries; }
    @GetMapping("/{taskId}")
    public List<TaskStateHistory> list(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { queries.require(tenantId, taskId); return service.list(taskId); }
    @GetMapping("/{taskId}/latest")
    public TaskStateHistory latest(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { queries.require(tenantId, taskId); return service.latest(taskId); }
}
