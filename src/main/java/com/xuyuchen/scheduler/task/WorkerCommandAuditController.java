package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/worker-commands")
public class WorkerCommandAuditController {
    private final WorkerCommandAuditService service;
    private final TaskQueryService queries;
    public WorkerCommandAuditController(WorkerCommandAuditService service, TaskQueryService queries) { this.service = service; this.queries = queries; }
    @GetMapping("/{taskId}")
    public List<WorkerCommandAuditService.CommandAudit> list(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { queries.require(tenantId, taskId); return service.list(taskId); }
}
