package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TaskCancellationService {
    private final TaskRepository tasks;
    private final TenantQuotaService quotas;
    private final TaskAuditService audit;
    private final TaskEventPublisher events;
    public TaskCancellationService(TaskRepository tasks, TenantQuotaService quotas, TaskAuditService audit, TaskEventPublisher events) {
        this.tasks = tasks; this.quotas = quotas; this.audit = audit; this.events = events;
    }
    public Task cancel(String tenantId, UUID taskId, String operator, String reason) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        TaskStatus before = task.getStatus();
        if (!task.cancel()) throw new IllegalStateException("task cannot be canceled");
        quotas.release(tenantId);
        tasks.save(task);
        audit.record(task, operator, before, TaskStatus.CANCELED, reason, RequestContextFilter.traceId(), Map.of());
        events.publish(TaskEvent.of(task, TaskEventType.CANCELED, operator, RequestContextFilter.traceId(), Map.of("reason", reason)));
        return task;
    }
}
