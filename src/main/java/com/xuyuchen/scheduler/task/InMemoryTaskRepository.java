package com.xuyuchen.scheduler.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnProperty(name = "scheduler.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryTaskRepository implements TaskRepository {
    private final ConcurrentMap<UUID, Task> records = new ConcurrentHashMap<>();
    @Override public Task save(Task task) { records.put(task.getId(), task); return task; }
    @Override public Optional<Task> findById(UUID id) { return Optional.ofNullable(records.get(id)); }
    @Override public List<Task> findByTenant(String tenantId) {
        return records.values().stream().filter(t -> t.getTenantId().equals(tenantId))
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed()).toList();
    }
    @Override public List<Task> findByStatus(TaskStatus status) { return records.values().stream().filter(t -> t.getStatus() == status).toList(); }
    @Override public Optional<Task> findByIdempotencyKey(String tenantId, String key) {
        return records.values().stream().filter(t -> t.getTenantId().equals(tenantId) && t.getIdempotencyKey().equals(key)).findFirst();
    }
    @Override public long countByTenant(String tenantId) { return records.values().stream().filter(t -> t.getTenantId().equals(tenantId)).count(); }
}
