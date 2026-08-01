package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.RequestContextFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
    private final DependencyRepository dependencies;
    private final WorkerCapacityService capacities;
    private final String schedulerId = "scheduler-" + UUID.randomUUID();

    public DispatchOrchestrator(TaskRepository tasks, TaskTemplateService templates, WorkerRegistry workers, TenantQuotaService quotas, TaskLeaseService leases, WorkerCommandPublisher commands, TaskEventPublisher events, TaskAuditService audit, TaskDispatchLockService locks, DependencyRepository dependencies) {
        this(tasks, templates, workers, quotas, leases, commands, events, audit, locks, dependencies, new WorkerCapacityService());
    }
    @Autowired
    public DispatchOrchestrator(TaskRepository tasks, TaskTemplateService templates, WorkerRegistry workers, TenantQuotaService quotas, TaskLeaseService leases, WorkerCommandPublisher commands, TaskEventPublisher events, TaskAuditService audit, TaskDispatchLockService locks, DependencyRepository dependencies, WorkerCapacityService capacities) {
        this.tasks = tasks; this.templates = templates; this.workers = workers; this.quotas = quotas; this.leases = leases; this.commands = commands; this.events = events; this.audit = audit; this.locks = locks; this.dependencies = dependencies; this.capacities = capacities;
    }

    @Scheduled(fixedDelayString = "${scheduler.orchestrator-delay-ms:2000}")
    public void dispatch() {
        tasks.findByStatus(TaskStatus.READY).stream().limit(100).forEach(this::dispatchOne);
    }

    public boolean dispatchOne(Task task) {
        if (!locks.tryTask(task.getId().toString(), schedulerId)) return false;
        try {
            if (!dependenciesReady(task)) return false;
            TaskTemplate template = templates.list(task.getTenantId()).stream().filter(t -> t.getCode().equals(task.getName()) && t.isEnabled()).findFirst().orElse(null);
            if (template == null || !quotas.acquire(task.getTenantId())) return false;
            WorkerInfo worker = workers.schedulable(task.getTenantId(), template.getExecutorType()).stream().filter(capacities::hasCapacity).findFirst().orElse(null);
            if (worker == null) { quotas.release(task.getTenantId()); return false; }
            if (!task.dispatch(worker.getWorkerId(), 30)) { quotas.release(task.getTenantId()); return false; }
            TaskLease lease = leases.issue(task.getId(), worker.getWorkerId(), task.getAttempt(), 30);
            task.assignLeaseToken(lease.token());
            task.renewLease(lease.expiresAt());
            tasks.save(task);
            worker.markTaskStarted();
            try {
                commands.dispatch(new WorkerCommand(task.getId(), task.getTenantId(), task.getName(), task.getPayload(), task.getAttempt(), worker.getWorkerId(), lease.token(), Instant.now(), Map.of("traceId", RequestContextFilter.traceId())));
            } catch (RuntimeException publishFailure) {
                task.requeueAfterDispatchFailure();
                tasks.save(task);
                leases.forget(task.getId());
                worker.markTaskFinished();
                quotas.release(task.getTenantId());
                throw publishFailure;
            }
            audit.record(task, schedulerId, TaskStatus.READY, TaskStatus.DISPATCHED, "orchestrator-dispatch", RequestContextFilter.traceId(), Map.of("worker", worker.getWorkerId()));
            events.publish(TaskEvent.of(task, TaskEventType.DISPATCHED, worker.getWorkerId(), RequestContextFilter.traceId(), Map.of("lease", lease.token())));
            return true;
        } finally { locks.releaseTask(task.getId().toString(), schedulerId); }
    }

    private boolean dependenciesReady(Task task) {
        return dependencies.findByChild(task.getId()).stream().allMatch(edge -> tasks.findById(edge.parentTaskId())
                .map(parent -> parent.getTenantId().equals(task.getTenantId()) && parent.getStatus() == TaskStatus.SUCCESS)
                .orElse(false));
    }
}
