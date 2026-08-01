package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class TaskEventController {
    private final InMemoryTaskEventPublisher publisher;
    public TaskEventController(InMemoryTaskEventPublisher publisher) { this.publisher = publisher; }
    @GetMapping("/recent")
    public List<TaskEvent> recent(@RequestParam(defaultValue = "50") int limit) { return publisher.recent(Math.min(Math.max(limit, 1), 200)); }
}
