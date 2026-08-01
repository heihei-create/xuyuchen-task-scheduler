package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TaskDispatchPlanner {
    private final TaskTemplateService templates;
    private final WorkerRegistry workers;
    private final TenantQuotaService quotas;
    public TaskDispatchPlanner(TaskTemplateService templates, WorkerRegistry workers, TenantQuotaService quotas) {
        this.templates = templates; this.workers = workers; this.quotas = quotas;
    }
    public List<TaskDispatchDecision> plan(List<Task> candidates) {
        return candidates.stream().sorted(Comparator.comparingInt(this::priority).reversed()).map(this::planOne).flatMap(java.util.Optional::stream).toList();
    }
    private int priority(Task task) {
        return templates.list(task.getTenantId()).stream().filter(t -> t.getCode().equals(task.getName())).map(t -> t.getPriority().weight()).findFirst().orElse(TaskPriority.NORMAL.weight());
    }
    private java.util.Optional<TaskDispatchDecision> planOne(Task task) {
        if (!quotas.acquire(task.getTenantId())) return java.util.Optional.empty();
        TaskTemplate template = templates.list(task.getTenantId()).stream().filter(t -> t.getCode().equals(task.getName()) && t.isEnabled()).findFirst().orElse(null);
        if (template == null) { quotas.release(task.getTenantId()); return java.util.Optional.empty(); }
        return workers.available(template.getExecutorType()).stream().findFirst()
                .map(worker -> new TaskDispatchDecision(task.getId(), task.getTenantId(), template.getExecutorType(), worker.getWorkerId(), task.getAttempt() + 1, "quota-and-capability-match"))
                .or(() -> { quotas.release(task.getTenantId()); return java.util.Optional.empty(); });
    }
}
