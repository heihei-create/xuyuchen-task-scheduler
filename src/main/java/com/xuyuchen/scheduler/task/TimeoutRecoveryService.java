package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TimeoutRecoveryService {
    private final TaskRepository tasks;
    private final TaskLeaseService leases;
    private final TenantQuotaService quotas;
    private final SchedulerMetrics metrics;
    private final TaskAuditService audit;
    private final TaskEventPublisher events;
    public TimeoutRecoveryService(TaskRepository tasks, TaskLeaseService leases, TenantQuotaService quotas, SchedulerMetrics metrics, TaskAuditService audit, TaskEventPublisher events) {
        this.tasks = tasks; this.leases = leases; this.quotas = quotas; this.metrics = metrics; this.audit = audit; this.events = events;
    }
    @Scheduled(fixedDelayString = "${scheduler.timeout-delay-ms:5000}")
    public void recover() {
        tasks.findByStatus(TaskStatus.RUNNING).stream().filter(t -> leases.expired(t.getId())).forEach(this::recoverOne);
        tasks.findByStatus(TaskStatus.DISPATCHED).stream().filter(t -> leases.expired(t.getId())).forEach(this::recoverOne);
    }
    private void recoverOne(Task task) {
        TaskStatus before = task.getStatus();
        if (!task.timeout()) return;
        quotas.release(task.getTenantId()); metrics.timedOut();
        audit.record(task, "timeout-reaper", before, TaskStatus.TIMEOUT, "lease-expired", RequestContextFilter.traceId(), Map.of("attempt", task.getAttempt()));
        events.publish(TaskEvent.of(task, TaskEventType.TIMED_OUT, null, RequestContextFilter.traceId(), Map.of()));
    }
}
