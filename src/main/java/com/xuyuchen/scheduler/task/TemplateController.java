package com.xuyuchen.scheduler.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.xuyuchen.scheduler.task.TemplateDtos.*;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private final TaskTemplateService service;
    public TemplateController(TaskTemplateService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@RequestHeader("X-Tenant-Id") String tenantId, @Valid @RequestBody CreateTemplateRequest req) {
        TaskPriority priority = req.priority() == null ? TaskPriority.NORMAL : req.priority();
        return TemplateResponse.from(service.create(tenantId, req.code(), req.name(), req.executorType(), req.maxAttempts(), priority));
    }
    @GetMapping
    public List<TemplateResponse> list(@RequestHeader("X-Tenant-Id") String tenantId) {
        return service.list(tenantId).stream().map(TemplateResponse::from).toList();
    }
    @PatchMapping("/{id}/enabled")
    public TemplateResponse enabled(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID id, @RequestParam boolean value) {
        return TemplateResponse.from(service.setEnabled(tenantId, id, value));
    }
}
