package com.xuyuchen.scheduler.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository {
    TaskTemplate save(TaskTemplate template);
    Optional<TaskTemplate> findById(UUID id);
    Optional<TaskTemplate> findByCode(String tenantId, String code);
    List<TaskTemplate> findByTenant(String tenantId);
}
