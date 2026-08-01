package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dependencies")
public class DependencyStatusController {
    private final DependencyStatusService service;
    private final TaskQueryService queries;
    public DependencyStatusController(DependencyStatusService service, TaskQueryService queries) { this.service = service; this.queries = queries; }
    @GetMapping("/{taskId}")
    public DependencyStatusService.DependencySummary get(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { queries.require(tenantId, taskId); return service.summary(taskId); }
}
