package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static com.xuyuchen.scheduler.common.JsonValue.object;

@Service
public class TaskLifecycleService {
    private final TaskRepository tasks;
    private final TaskTemplateService templates;
    private final TenantQuotaService quotas;
    private final WorkerRegistry workers;
    private final TaskAuditService audit;
    private final TaskEventPublisher events;

    public TaskLifecycleService(TaskRepository tasks, TaskTemplateService templates, TenantQuotaService quotas, WorkerRegistry workers, TaskAuditService audit, TaskEventPublisher events) {
        this.tasks = tasks; this.templates = templates; this.quotas = quotas; this.workers = workers; this.audit = audit; this.events = events;
    }

    public Task create(String tenantId, String templateCode, String payload, String idempotencyKey) {
        TaskTemplate template = templates.list(tenantId).stream().filter(t -> t.getCode().equals(templateCode) && t.isEnabled()).findFirst().orElseThrow(() -> new IllegalArgumentException("enabled template not found"));
        if (!quotas.reserveSubmission(tenantId)) throw new IllegalStateException("daily tenant quota exceeded");
        Task existing = tasks.findByIdempotencyKey(tenantId, idempotencyKey).orElse(null);
        if (existing != null) return existing;
        Task task = new Task(UUID.randomUUID(), tenantId, template.getCode(), payload, idempotencyKey);
        task.ready(); tasks.save(task);
        audit.record(task, "api", TaskStatus.CREATED, TaskStatus.READY, "task-created", RequestContextFilter.traceId(), object("template", template.getCode()));
        events.publish(TaskEvent.of(task, TaskEventType.CREATED, null, RequestContextFilter.traceId(), object("template", template.getCode())));
        return task;
    }

    public Task dispatch(String tenantId, UUID taskId) {
        Task task = require(tenantId, taskId);
        TaskTemplate template = templates.list(tenantId).stream().filter(t -> t.getCode().equals(task.getName())).findFirst().orElseThrow(() -> new IllegalStateException("template missing"));
        WorkerInfo worker = workers.available(template.getExecutorType()).stream().findFirst().orElseThrow(() -> new IllegalStateException("no worker capability available"));
        if (!quotas.acquire(tenantId)) throw new IllegalStateException("running quota exceeded");
        TaskStatus before = task.getStatus();
        if (!task.dispatch(worker.getWorkerId(), 30)) { quotas.release(tenantId); throw new IllegalStateException("task is not ready"); }
        worker.markTaskStarted();
        audit.record(task, "scheduler", before, TaskStatus.DISPATCHED, "worker-selected", RequestContextFilter.traceId(), object("workerId", worker.getWorkerId(), "executor", template.getExecutorType()));
        events.publish(TaskEvent.of(task, TaskEventType.DISPATCHED, worker.getWorkerId(), RequestContextFilter.traceId(), object("executor", template.getExecutorType())));
        return task;
    }

    public Task start(String tenantId, UUID taskId, String workerId, int attempt) {
        Task task = require(tenantId, taskId);
        TaskStatus before = task.getStatus();
        if (!task.start(workerId, attempt)) throw new IllegalStateException("stale start event");
        audit.record(task, workerId, before, TaskStatus.RUNNING, "worker-started", RequestContextFilter.traceId(), Map.of("attempt", attempt));
        events.publish(TaskEvent.of(task, TaskEventType.STARTED, workerId, RequestContextFilter.traceId(), Map.of()));
        return task;
    }

    public Task finish(String tenantId, UUID taskId, String workerId, int attempt, boolean success, String result) {
        Task task = require(tenantId, taskId);
        TaskStatus before = task.getStatus();
        if (!task.finish(workerId, attempt, success, result)) throw new IllegalStateException("stale finish event");
        quotas.release(tenantId); workers.require(workerId).markTaskFinished();
        TaskStatus after = task.getStatus();
        audit.record(task, workerId, before, after, success ? "worker-success" : "worker-failed", RequestContextFilter.traceId(), object("resultSize", result == null ? 0 : result.length()));
        events.publish(TaskEvent.of(task, success ? TaskEventType.SUCCEEDED : TaskEventType.FAILED, workerId, RequestContextFilter.traceId(), Map.of()));
        return task;
    }

    public Task require(String tenantId, UUID taskId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        return task;
    }
}
