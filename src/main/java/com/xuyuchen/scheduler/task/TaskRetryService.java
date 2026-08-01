package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TaskRetryService {
    private final TaskRepository tasks;
    private final TaskTemplateService templates;
    private final TaskAuditService audit;
    private final TaskEventPublisher events;
    public TaskRetryService(TaskRepository tasks, TaskTemplateService templates, TaskAuditService audit, TaskEventPublisher events) {
        this.tasks = tasks; this.templates = templates; this.audit = audit; this.events = events;
    }
    public Task retry(String tenantId, UUID taskId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        TaskTemplate template = templates.list(tenantId).stream().filter(t -> t.getCode().equals(task.getName())).findFirst().orElseThrow(() -> new IllegalStateException("template missing"));
        if (task.getAttempt() >= template.getMaxAttempts()) throw new IllegalStateException("retry limit reached");
        if (task.getStatus() != TaskStatus.FAILED && task.getStatus() != TaskStatus.TIMEOUT) throw new IllegalStateException("only failed tasks can retry");
        if (!task.retry()) throw new IllegalStateException("task cannot return to ready");
        tasks.save(task);
        audit.record(task, "operator", TaskStatus.FAILED, TaskStatus.READY, "manual-retry", "manual", Map.of("maxAttempts", template.getMaxAttempts()));
        events.publish(TaskEvent.of(task, TaskEventType.RETRIED, null, "manual", Map.of()));
        return task;
    }
}
