package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/maintenance/tasks")
public class TaskMaintenanceController {
    private final TaskMaintenanceService service;
    public TaskMaintenanceController(TaskMaintenanceService service) { this.service = service; }
    @GetMapping("/summary")
    public TaskMaintenanceService.MaintenanceSummary summary(@RequestHeader("X-Tenant-Id") String tenantId) { return service.summarize(tenantId); }
    @PostMapping("/cleanup")
    public void cleanup(@RequestHeader("X-Tenant-Id") String tenantId) { service.cleanup(tenantId); }
}
