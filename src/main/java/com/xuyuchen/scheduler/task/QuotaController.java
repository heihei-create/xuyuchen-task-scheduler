package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import com.xuyuchen.scheduler.common.TenantContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quotas")
public class QuotaController {
    private final TenantQuotaService service;
    public QuotaController(TenantQuotaService service) { this.service = service; }
    public record QuotaRequest(@Min(1) int maxRunning, @Min(1) int maxDaily) {}
    @PutMapping("/{tenantId}")
    public TenantQuota configure(@PathVariable String tenantId, @Valid @RequestBody QuotaRequest request) {
        if (!TenantContext.require().equals(tenantId)) throw new IllegalArgumentException("tenant access denied");
        return service.configure(tenantId, request.maxRunning(), request.maxDaily());
    }
    @GetMapping("/{tenantId}")
    public TenantQuota get(@PathVariable String tenantId) { if (!TenantContext.require().equals(tenantId)) throw new IllegalArgumentException("tenant access denied"); return service.require(tenantId); }
}
