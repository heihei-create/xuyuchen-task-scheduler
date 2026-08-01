package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class TemplateDtos {
    private TemplateDtos() {}
    public record CreateTemplateRequest(@NotBlank String code, @NotBlank String name, @NotBlank String executorType, @Min(1) int maxAttempts, TaskPriority priority) {}
    public record TemplateResponse(UUID id, String tenantId, String code, String name, String executorType, int maxAttempts, TaskPriority priority, boolean enabled) {
        static TemplateResponse from(TaskTemplate t) { return new TemplateResponse(t.getId(), t.getTenantId(), t.getCode(), t.getName(), t.getExecutorType(), t.getMaxAttempts(), t.getPriority(), t.isEnabled()); }
    }
}
