package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cancellations")
public class TaskCancellationController {
    private final TaskCancellationService service;
    public TaskCancellationController(TaskCancellationService service) { this.service = service; }
    @PostMapping("/{taskId}")
    public TaskDtos.TaskResponse cancel(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId, @RequestParam(defaultValue = "user-cancel") String reason) {
        return TaskDtos.TaskResponse.from(service.cancel(tenantId, taskId, "api-user", reason));
    }
}
