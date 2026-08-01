package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/results")
public class TaskResultController {
    private final TaskResultService service;
    public TaskResultController(TaskResultService service) { this.service = service; }
    @GetMapping("/{taskId}")
    public List<TaskResult> list(@PathVariable UUID taskId) { return service.list(taskId); }
    @GetMapping("/{taskId}/latest")
    public TaskResult latest(@PathVariable UUID taskId) { return service.latest(taskId); }
}
