package com.xuyuchen.scheduler.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ConcurrentMap<String, Object> idempotencyLocks = new ConcurrentHashMap<>();

    @Autowired
    public TaskService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    /** Compatibility constructor for the original unit tests; production uses the repository constructor. */
    public TaskService() {
        this(new InMemoryTaskRepository());
    }

    public Task create(String tenantId, TaskDtos.CreateTaskRequest request) {
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("X-Tenant-Id is required");
        String key = tenantId + ":" + request.idempotencyKey();
        Object lock = idempotencyLocks.computeIfAbsent(key, ignored -> new Object());
        try {
            synchronized (lock) {
                Task existing = tasks.findByIdempotencyKey(tenantId, request.idempotencyKey()).orElse(null);
                if (existing != null) return existing;
                Task task = new Task(UUID.randomUUID(), tenantId, request.name(), request.payload(), request.idempotencyKey());
                task.ready();
                try {
                    return tasks.save(task);
                } catch (DataIntegrityViolationException duplicate) {
                    return tasks.findByIdempotencyKey(tenantId, request.idempotencyKey()).orElseThrow(() -> duplicate);
                }
            }
        } finally {
            idempotencyLocks.remove(key, lock);
        }
    }

    public Task get(String tenantId, UUID id) {
        Task task = tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        return task;
    }

    public List<Task> list(String tenantId) {
        return tasks.findByTenant(tenantId);
    }

    /** Compatibility hook for the original unit tests. Production dispatch is owned by DispatchOrchestrator. */
    public void dispatchReadyTasks() {
        tasks.findByStatus(TaskStatus.READY).forEach(task -> task.dispatch("local-worker", 30));
    }

    public Task start(String tenantId, UUID id, TaskDtos.WorkerEventRequest event) {
        Task task = get(tenantId, id);
        if (!task.start(event.workerId(), event.attempt())) throw new IllegalStateException("invalid task start event");
        tasks.save(task);
        return task;
    }

    public Task finish(String tenantId, UUID id, TaskDtos.WorkerEventRequest event) {
        Task task = get(tenantId, id);
        if (!task.finish(event.workerId(), event.attempt(), event.success(), event.result())) throw new IllegalStateException("stale or duplicate worker event");
        tasks.save(task);
        return task;
    }

    public Task cancel(String tenantId, UUID id) {
        Task task = get(tenantId, id);
        if (!task.cancel()) throw new IllegalStateException("task cannot be canceled in current state");
        tasks.save(task);
        return task;
    }
}
