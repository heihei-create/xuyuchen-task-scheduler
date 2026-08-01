package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/state-history")
public class TaskStateHistoryController {
    private final TaskStateHistoryService service;
    public TaskStateHistoryController(TaskStateHistoryService service) { this.service = service; }
    @GetMapping("/{taskId}")
    public List<TaskStateHistory> list(@PathVariable UUID taskId) { return service.list(taskId); }
    @GetMapping("/{taskId}/latest")
    public TaskStateHistory latest(@PathVariable UUID taskId) { return service.latest(taskId); }
}
