package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class TaskEventController {
    private final ObjectProvider<InMemoryTaskEventPublisher> publisher;
    public TaskEventController(ObjectProvider<InMemoryTaskEventPublisher> publisher) { this.publisher = publisher; }
    @GetMapping("/recent")
    public List<TaskEvent> recent(@RequestHeader("X-Tenant-Id") String tenantId, @RequestParam(defaultValue = "50") int limit) { InMemoryTaskEventPublisher value = publisher.getIfAvailable(); return value == null ? List.of() : value.recent(tenantId, Math.min(Math.max(limit, 1), 200)); }
}
