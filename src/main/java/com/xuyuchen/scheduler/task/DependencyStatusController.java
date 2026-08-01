package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dependencies")
public class DependencyStatusController {
    private final DependencyStatusService service;
    public DependencyStatusController(DependencyStatusService service) { this.service = service; }
    @GetMapping("/{taskId}")
    public DependencyStatusService.DependencySummary get(@PathVariable UUID taskId) { return service.summary(taskId); }
}
