package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class DispatchOrchestrator {
    private final TaskRepository tasks;
    private final TaskTemplateService templates;
    private final WorkerRegistry workers;
    private final TenantQuotaService quotas;
    private final TaskLeaseService leases;
    private final WorkerCommandPublisher commands;
    private final TaskEventPublisher events;
    private final TaskAuditService audit;
    private final TaskDispatchLockService locks;
    private final String schedulerId = "scheduler-local";

    public DispatchOrchestrator(TaskRepository tasks, TaskTemplateService templates, WorkerRegistry workers, TenantQuotaService quotas, TaskLeaseService leases, WorkerCommandPublisher commands, TaskEventPublisher events, TaskAuditService audit, TaskDispatchLockService locks) {
        this.tasks = tasks; this.templates = templates; this.workers = workers; this.quotas = quotas; this.leases = leases; this.commands = commands; this.events = events; this.audit = audit; this.locks = locks;
    }

    @Scheduled(fixedDelayString = "${scheduler.orchestrator-delay-ms:2000}")
    public void dispatch() {
        tasks.findByStatus(TaskStatus.READY).stream().limit(100).forEach(this::dispatchOne);
    }

    public boolean dispatchOne(Task task) {
        if (!locks.tryTask(task.getId().toString(), schedulerId)) return false;
        try {
            TaskTemplate template = templates.list(task.getTenantId()).stream().filter(t -> t.getCode().equals(task.getName()) && t.isEnabled()).findFirst().orElse(null);
            if (template == null || !quotas.acquire(task.getTenantId())) return false;
            WorkerInfo worker = workers.available(template.getExecutorType()).stream().findFirst().orElse(null);
            if (worker == null) { quotas.release(task.getTenantId()); return false; }
            if (!task.dispatch(worker.getWorkerId(), 30)) { quotas.release(task.getTenantId()); return false; }
            TaskLease lease = leases.issue(task.getId(), worker.getWorkerId(), task.getAttempt(), 30);
            worker.markTaskStarted();
            commands.dispatch(new WorkerCommand(task.getId(), task.getTenantId(), task.getName(), task.getPayload(), task.getAttempt(), worker.getWorkerId(), lease.token(), Instant.now(), Map.of("traceId", RequestContextFilter.traceId())));
            audit.record(task, schedulerId, TaskStatus.READY, TaskStatus.DISPATCHED, "orchestrator-dispatch", RequestContextFilter.traceId(), Map.of("worker", worker.getWorkerId()));
            events.publish(TaskEvent.of(task, TaskEventType.DISPATCHED, worker.getWorkerId(), RequestContextFilter.traceId(), Map.of("lease", lease.token())));
            return true;
        } finally { locks.releaseTenant(task.getTenantId(), schedulerId); }
    }
}
