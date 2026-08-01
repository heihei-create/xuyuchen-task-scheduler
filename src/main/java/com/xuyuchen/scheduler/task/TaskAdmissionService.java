package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TaskAdmissionService {
    private final TaskPayloadValidator payloads;
    private final TenantQuotaService quotas;
    private final TaskTemplateService templates;
    public TaskAdmissionService(TaskPayloadValidator payloads, TenantQuotaService quotas, TaskTemplateService templates) {
        this.payloads = payloads; this.quotas = quotas; this.templates = templates;
    }
    public AdmissionResult check(String tenantId, String templateCode, String payload) {
        TaskTemplate template = templates.list(tenantId).stream().filter(t -> t.getCode().equals(templateCode)).findFirst().orElseThrow(() -> new IllegalArgumentException("template not found"));
        if (!template.isEnabled()) return new AdmissionResult(false, "template-disabled", Map.of());
        if (!quotas.reserveSubmission(tenantId)) return new AdmissionResult(false, "daily-quota-exceeded", Map.of());
        payloads.validate(template, payload);
        return new AdmissionResult(true, "accepted", Map.of("executorType", template.getExecutorType(), "priority", template.getPriority().name()));
    }
    public record AdmissionResult(boolean accepted, String reason, Map<String, Object> details) {}
}
