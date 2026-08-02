package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantQuotaService {
    private final Map<String, TenantQuota> quotas = new ConcurrentHashMap<>();
    public TenantQuotaService() { quotas.put("default", new TenantQuota("default", 3, 1000)); }
    public TenantQuota configure(String tenantId, int maxRunning, int maxDaily) {
        if (maxRunning < 1 || maxDaily < 1) throw new IllegalArgumentException("quota must be positive");
        TenantQuota quota = new TenantQuota(tenantId, maxRunning, maxDaily); quotas.put(tenantId, quota); return quota;
    }
    public TenantQuota require(String tenantId) { return quotas.computeIfAbsent(tenantId, id -> new TenantQuota(id, 3, 1000)); }
    public boolean reserveSubmission(String tenantId) { return require(tenantId).reserveSubmission(); }
    public void rollbackSubmission(String tenantId) { require(tenantId).rollbackSubmission(); }
    public boolean acquire(String tenantId) { return require(tenantId).tryAcquire(); }
    public void release(String tenantId) { require(tenantId).release(); }
}
