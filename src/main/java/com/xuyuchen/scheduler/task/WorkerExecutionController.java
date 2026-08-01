package com.xuyuchen.scheduler.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/execution")
public class WorkerExecutionController {
    private final WorkerExecutionService service;
    private final TaskProgressService progress;
    private final TaskLogService logs;
    private final TaskArtifactService artifacts;
    public WorkerExecutionController(WorkerExecutionService service, TaskProgressService progress, TaskLogService logs, TaskArtifactService artifacts) {
        this.service = service; this.progress = progress; this.logs = logs; this.artifacts = artifacts;
    }
    public record LeaseRequest(@NotBlank String workerId, int attempt, @NotBlank String leaseToken) {}
    public record FinishRequest(@NotBlank String workerId, int attempt, @NotBlank String leaseToken, boolean success, String result) {}
    public record ArtifactRequest(int attempt, @NotBlank String objectKey, @NotBlank String contentType, long size, String checksum) {}
    @PostMapping("/{taskId}/start")
    public TaskDtos.TaskResponse start(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId, @Valid @RequestBody LeaseRequest req) {
        return TaskDtos.TaskResponse.from(service.start(tenantId, taskId, req.workerId(), req.attempt(), req.leaseToken()));
    }
    @PostMapping("/{taskId}/heartbeat")
    public TaskDtos.TaskResponse heartbeat(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId, @Valid @RequestBody WorkerHeartbeatRequest req) {
        return TaskDtos.TaskResponse.from(service.heartbeat(tenantId, taskId, req));
    }
    @PostMapping("/{taskId}/progress")
    public TaskProgress progress(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId, @Valid @RequestBody WorkerProgressRequest req) {
        return service.progress(tenantId, taskId, req);
    }
    @PostMapping("/{taskId}/finish")
    public TaskDtos.TaskResponse finish(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId, @Valid @RequestBody FinishRequest req) {
        return TaskDtos.TaskResponse.from(service.finish(tenantId, taskId, req.workerId(), req.attempt(), req.leaseToken(), req.success(), req.result()));
    }
    @PostMapping("/{taskId}/artifacts")
    public TaskArtifact artifact(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID taskId, @Valid @RequestBody ArtifactRequest req) {
        return service.artifact(tenantId, taskId, req.attempt(), req.objectKey(), req.contentType(), req.size(), req.checksum());
    }
    @GetMapping("/{taskId}/progress")
    public TaskProgress progress(@PathVariable UUID taskId) { return progress.get(taskId); }
    @GetMapping("/{taskId}/logs")
    public java.util.List<TaskLogEntry> logs(@PathVariable UUID taskId, @RequestParam(required = false) String level) { return logs.list(taskId, level); }
    @GetMapping("/{taskId}/artifacts")
    public java.util.List<TaskArtifact> artifacts(@PathVariable UUID taskId) { return artifacts.list(taskId); }
}
