package com.xuyuchen.scheduler.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(UUID id);
    List<Task> findByTenant(String tenantId);
    List<Task> findByStatus(TaskStatus status);
    Optional<Task> findByIdempotencyKey(String tenantId, String key);
    long countByTenant(String tenantId);
}
