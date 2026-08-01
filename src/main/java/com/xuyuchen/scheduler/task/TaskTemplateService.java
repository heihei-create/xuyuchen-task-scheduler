package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskTemplateService {
    private final TemplateRepository repository;
    public TaskTemplateService(TemplateRepository repository) { this.repository = repository; }

    public TaskTemplate create(String tenantId, String code, String name, String executorType, int maxAttempts, TaskPriority priority) {
        if (repository.findByCode(tenantId, code).isPresent()) throw new IllegalStateException("template code already exists");
        return repository.save(new TaskTemplate(UUID.randomUUID(), tenantId, code, name, executorType, maxAttempts, priority));
    }
    public TaskTemplate get(String tenantId, UUID id) {
        TaskTemplate template = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("template not found"));
        if (!template.getTenantId().equals(tenantId)) throw new IllegalArgumentException("template not found");
        return template;
    }
    public List<TaskTemplate> list(String tenantId) { return repository.findByTenant(tenantId); }
    public TaskTemplate setEnabled(String tenantId, UUID id, boolean enabled) {
        TaskTemplate template = get(tenantId, id);
        if (enabled) template.enable(); else template.disable();
        return repository.save(template);
    }
}
