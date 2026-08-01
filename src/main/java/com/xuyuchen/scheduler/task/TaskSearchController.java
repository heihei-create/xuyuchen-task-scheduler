package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.PageResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/search/tasks")
public class TaskSearchController {
    private final TaskSearchService service;
    public TaskSearchController(TaskSearchService service) { this.service = service; }
    @GetMapping
    public PageResult<Task> search(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String workerId,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(tenantId, new TaskFilter(status, name, null, createdAfter, createdBefore, workerId), page, size);
    }
}
