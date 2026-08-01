package com.xuyuchen.scheduler.task;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/worker-commands")
public class WorkerCommandAuditController {
    private final WorkerCommandAuditService service;
    public WorkerCommandAuditController(WorkerCommandAuditService service) { this.service = service; }
    @GetMapping("/{taskId}")
    public List<WorkerCommandAuditService.CommandAudit> list(@PathVariable UUID taskId) { return service.list(taskId); }
}
