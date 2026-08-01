package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryTemplateRepository implements TemplateRepository {
    private final ConcurrentMap<UUID, TaskTemplate> records = new ConcurrentHashMap<>();
    @Override public TaskTemplate save(TaskTemplate template) { records.put(template.getId(), template); return template; }
    @Override public Optional<TaskTemplate> findById(UUID id) { return Optional.ofNullable(records.get(id)); }
    @Override public Optional<TaskTemplate> findByCode(String tenantId, String code) {
        return records.values().stream().filter(t -> t.getTenantId().equals(tenantId) && t.getCode().equals(code)).findFirst();
    }
    @Override public List<TaskTemplate> findByTenant(String tenantId) { return records.values().stream().filter(t -> t.getTenantId().equals(tenantId)).toList(); }
}
