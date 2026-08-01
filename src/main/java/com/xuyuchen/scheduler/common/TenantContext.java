package com.xuyuchen.scheduler.common;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private TenantContext() {}
    public static void set(String tenantId) { CURRENT.set(tenantId); }
    public static String require() {
        String value = CURRENT.get();
        if (value == null || value.isBlank()) throw new DomainException("TENANT_REQUIRED", "tenant context is required");
        return value;
    }
    public static void clear() { CURRENT.remove(); }
}
