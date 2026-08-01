package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class WorkerExecutionService {
    private final TaskRepository tasks;
    private final TaskLeaseService leases;
    private final WorkerRegistry workers;
    private final TaskProgressService progress;
    private final TaskLogService logs;
    private final TaskArtifactService artifacts;
    private final TaskAuditService audit;
    private final TaskEventPublisher events;
    private final TenantQuotaService quotas;

    public WorkerExecutionService(TaskRepository tasks, TaskLeaseService leases, WorkerRegistry workers, TaskProgressService progress, TaskLogService logs, TaskArtifactService artifacts, TaskAuditService audit, TaskEventPublisher events, TenantQuotaService quotas) {
        this.tasks = tasks; this.leases = leases; this.workers = workers; this.progress = progress; this.logs = logs; this.artifacts = artifacts; this.audit = audit; this.events = events; this.quotas = quotas;
    }
    public Task start(String tenantId, UUID taskId, String workerId, int attempt, String token) {
        Task task = require(tenantId, taskId);
        if (!leases.require(taskId).matches(workerId, attempt, token)) throw new IllegalStateException("invalid lease");
        TaskStatus before = task.getStatus();
        if (!task.start(workerId, attempt)) throw new IllegalStateException("stale start event");
        logs.append(taskId, attempt, "INFO", "worker started execution", RequestContextFilter.traceId());
        audit.record(task, workerId, before, TaskStatus.RUNNING, "worker-started", RequestContextFilter.traceId(), Map.of());
        events.publish(TaskEvent.of(task, TaskEventType.STARTED, workerId, RequestContextFilter.traceId(), Map.of()));
        return task;
    }
    public TaskProgress progress(String tenantId, UUID taskId, WorkerProgressRequest request) {
        Task task = require(tenantId, taskId);
        if (!leases.require(taskId).matches(request.workerId(), request.attempt(), leases.require(taskId).token())) throw new IllegalStateException("invalid lease");
        TaskProgress value = progress.update(taskId, request.phase(), request.percent(), request.message());
        logs.append(taskId, request.attempt(), "INFO", request.phase() + ": " + request.message(), RequestContextFilter.traceId());
        events.publish(TaskEvent.of(task, TaskEventType.PROGRESS, request.workerId(), RequestContextFilter.traceId(), Map.of("percent", request.percent(), "phase", request.phase())));
        return value;
    }
    public Task heartbeat(String tenantId, UUID taskId, WorkerHeartbeatRequest request) {
        Task task = require(tenantId, taskId);
        leases.renew(taskId, request.workerId(), request.attempt(), request.leaseToken(), 30);
        workers.heartbeat(request.workerId(), request.runningTasks());
        events.publish(TaskEvent.of(task, TaskEventType.HEARTBEAT, request.workerId(), RequestContextFilter.traceId(), Map.of("runningTasks", request.runningTasks())));
        return task;
    }
    public Task finish(String tenantId, UUID taskId, String workerId, int attempt, String token, boolean success, String result) {
        Task task = require(tenantId, taskId);
        if (!leases.require(taskId).matches(workerId, attempt, token)) throw new IllegalStateException("invalid lease");
        TaskStatus before = task.getStatus();
        if (!task.finish(workerId, attempt, success, result)) throw new IllegalStateException("stale finish event");
        leases.release(taskId, workerId, attempt, token);
        workers.require(workerId).markTaskFinished(); quotas.release(tenantId);
        if (success) progress.complete(taskId, "worker completed"); else progress.fail(taskId, result);
        logs.append(taskId, attempt, success ? "INFO" : "ERROR", success ? "worker completed" : "worker failed: " + result, RequestContextFilter.traceId());
        audit.record(task, workerId, before, task.getStatus(), success ? "worker-success" : "worker-failed", RequestContextFilter.traceId(), Map.of());
        events.publish(TaskEvent.of(task, success ? TaskEventType.SUCCEEDED : TaskEventType.FAILED, workerId, RequestContextFilter.traceId(), Map.of()));
        return task;
    }
    public TaskArtifact artifact(String tenantId, UUID taskId, int attempt, String key, String contentType, long size, String checksum) {
        Task task = require(tenantId, taskId);
        return artifacts.register(task.getId(), attempt, key, contentType, size, checksum);
    }
    private Task require(String tenantId, UUID id) {
        Task task = tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        return task;
    }
}
