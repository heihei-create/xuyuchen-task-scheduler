package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {
    private final TaskAuditService service;
    public AuditController(TaskAuditService service) { this.service = service; }
    @GetMapping("/tasks/{taskId}")
    public List<TaskAuditRecord> task(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId) { return service.findByTask(tenantId, taskId); }
    @GetMapping
    public List<TaskAuditRecord> tenant(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam(defaultValue = "100") int limit) {
        return service.findByTenant(tenantId, Math.min(Math.max(limit, 1), 500));
    }
}
