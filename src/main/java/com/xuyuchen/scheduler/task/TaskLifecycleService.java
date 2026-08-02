package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.dao.DataIntegrityViolationException;

import static com.xuyuchen.scheduler.common.JsonValue.object;

@Service
public class TaskLifecycleService {
    private final TaskRepository tasks;
    private final TaskTemplateService templates;
    private final TenantQuotaService quotas;
    private final WorkerRegistry workers;
    private final TaskAuditService audit;
    private final TaskEventPublisher events;
    private final DispatchOrchestrator orchestrator;
    private final TaskPayloadValidator payloads;
    private final ConcurrentMap<String, Object> idempotencyLocks = new ConcurrentHashMap<>();

    public TaskLifecycleService(TaskRepository tasks, TaskTemplateService templates, TenantQuotaService quotas, WorkerRegistry workers, TaskAuditService audit, TaskEventPublisher events, DispatchOrchestrator orchestrator, TaskPayloadValidator payloads) {
        this.tasks = tasks; this.templates = templates; this.quotas = quotas; this.workers = workers; this.audit = audit; this.events = events; this.orchestrator = orchestrator; this.payloads = payloads;
    }

    public Task create(String tenantId, String templateCode, String payload, String idempotencyKey) {
        TaskTemplate template = templates.list(tenantId).stream().filter(t -> t.getCode().equals(templateCode) && t.isEnabled()).findFirst().orElseThrow(() -> new IllegalArgumentException("enabled template not found"));
        payloads.validate(template, payload);
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotency key is required");
        String lockKey = tenantId + ":" + idempotencyKey;
        Object lock = idempotencyLocks.computeIfAbsent(lockKey, ignored -> new Object());
        try {
            synchronized (lock) {
                Task existing = tasks.findByIdempotencyKey(tenantId, idempotencyKey).orElse(null);
                if (existing != null) return existing;
                if (!quotas.reserveSubmission(tenantId)) throw new IllegalStateException("daily tenant quota exceeded");
                try {
                    Task task = new Task(UUID.randomUUID(), tenantId, template.getCode(), payload, idempotencyKey);
                    task.ready();
                    try {
                        tasks.save(task);
                    } catch (DataIntegrityViolationException duplicate) {
                        quotas.rollbackSubmission(tenantId);
                        return tasks.findByIdempotencyKey(tenantId, idempotencyKey).orElseThrow(() -> duplicate);
                    }
                    audit.record(task, "api", TaskStatus.CREATED, TaskStatus.READY, "task-created", RequestContextFilter.traceId(), object("template", template.getCode()));
                    events.publish(TaskEvent.of(task, TaskEventType.CREATED, null, RequestContextFilter.traceId(), object("template", template.getCode())));
                    return task;
                } catch (RuntimeException failure) {
                    quotas.rollbackSubmission(tenantId);
                    throw failure;
                }
            }
        } finally {
            idempotencyLocks.remove(lockKey, lock);
        }
    }

    public Task dispatch(String tenantId, UUID taskId) {
        Task task = require(tenantId, taskId);
        if (!orchestrator.dispatchOne(task)) throw new IllegalStateException("task could not be dispatched");
        return require(tenantId, taskId);
    }

    public Task start(String tenantId, UUID taskId, String workerId, int attempt) {
        Task task = require(tenantId, taskId);
        TaskStatus before = task.getStatus();
        if (!task.start(workerId, attempt)) throw new IllegalStateException("stale start event");
        tasks.save(task);
        audit.record(task, workerId, before, TaskStatus.RUNNING, "worker-started", RequestContextFilter.traceId(), Map.of("attempt", attempt));
        events.publish(TaskEvent.of(task, TaskEventType.STARTED, workerId, RequestContextFilter.traceId(), Map.of()));
        return task;
    }

    public Task finish(String tenantId, UUID taskId, String workerId, int attempt, boolean success, String result) {
        Task task = require(tenantId, taskId);
        TaskStatus before = task.getStatus();
        if (!task.finish(workerId, attempt, success, result)) throw new IllegalStateException("stale finish event");
        tasks.save(task);
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
