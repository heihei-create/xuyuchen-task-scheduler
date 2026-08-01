package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/task-bulk")
public class TaskBulkController {
    private final TaskBulkService service;
    public TaskBulkController(TaskBulkService service) { this.service = service; }
    @PostMapping("/cancel-failed")
    public Map<String, Object> cancelFailed(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam(defaultValue = "operator-cancel") String reason) {
        return Map.of("tenantId", tenantId, "canceled", service.cancelFailed(tenantId, reason));
    }
    @PostMapping("/retry-failed")
    public Map<String, Object> retryFailed(@RequestHeader("X-Tenant-Id") String tenantId) {
        return Map.of("tenantId", tenantId, "retried", service.retryFailed(tenantId));
    }
}
