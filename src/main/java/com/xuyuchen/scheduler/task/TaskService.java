package com.xuyuchen.scheduler.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TaskService {
    private final ConcurrentMap<UUID, Task> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> idempotency = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> tenantRunning = new ConcurrentHashMap<>();
    private final int maxPerTenant = 3;
    private final long leaseSeconds = 30;

    public Task create(String tenantId, TaskDtos.CreateTaskRequest request) {
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("X-Tenant-Id is required");
        String key = tenantId + ":" + request.idempotencyKey();
        UUID newId = UUID.randomUUID();
        UUID existing = idempotency.putIfAbsent(key, newId);
        UUID id = existing == null ? newId : existing;
        if (existing != null) return tasks.get(id);
        Task task = new Task(id, tenantId, request.name(), request.payload(), request.idempotencyKey());
        task.ready();
        tasks.put(id, task);
        return task;
    }

    public Task get(String tenantId, UUID id) {
        Task task = tasks.get(id);
        if (task == null || !task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        return task;
    }

    public List<Task> list(String tenantId) {
        return tasks.values().stream().filter(t -> t.getTenantId().equals(tenantId)).sorted(Comparator.comparing(Task::getCreatedAt).reversed()).toList();
    }

    @Scheduled(fixedDelayString = "${scheduler.dispatch-delay-ms:1000}")
    public void dispatchReadyTasks() {
        tasks.values().stream().filter(t -> t.getStatus() == TaskStatus.READY).sorted(Comparator.comparing(Task::getCreatedAt)).forEach(task -> {
            int running = tenantRunning.getOrDefault(task.getTenantId(), 0);
            if (running >= maxPerTenant) return;
            if (task.dispatch("local-worker", leaseSeconds)) tenantRunning.merge(task.getTenantId(), 1, Integer::sum);
        });
    }

    public Task start(String tenantId, UUID id, TaskDtos.WorkerEventRequest event) {
        Task task = get(tenantId, id);
        if (!task.start(event.workerId(), event.attempt())) throw new IllegalStateException("invalid task start event");
        return task;
    }

    public Task finish(String tenantId, UUID id, TaskDtos.WorkerEventRequest event) {
        Task task = get(tenantId, id);
        if (!task.finish(event.workerId(), event.attempt(), event.success(), event.result())) throw new IllegalStateException("stale or duplicate worker event");
        tenantRunning.computeIfPresent(tenantId, (k, v) -> Math.max(0, v - 1));
        return task;
    }

    public Task cancel(String tenantId, UUID id) {
        Task task = get(tenantId, id);
        if (!task.cancel()) throw new IllegalStateException("task cannot be canceled in current state");
        tenantRunning.computeIfPresent(tenantId, (k, v) -> Math.max(0, v - 1));
        return task;
    }

    @Scheduled(fixedDelayString = "${scheduler.reaper-delay-ms:5000}")
    public void reapExpiredLeases() {
        tasks.values().stream().filter(Task::timeout).forEach(task -> tenantRunning.computeIfPresent(task.getTenantId(), (k, v) -> Math.max(0, v - 1)));
    }
}
