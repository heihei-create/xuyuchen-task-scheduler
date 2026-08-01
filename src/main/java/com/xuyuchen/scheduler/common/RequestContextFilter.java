package com.xuyuchen.scheduler.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Component("schedulerRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    private static final ThreadLocal<String> TRACE = new ThreadLocal<>();
    private final boolean authEnabled;
    private final Map<String, String> tenantKeys;

    public RequestContextFilter(
            @Value("${scheduler.auth.enabled:true}") boolean authEnabled,
            @Value("${scheduler.auth.tenant-keys:${SCHEDULER_TENANT_KEYS:}}") String configuredKeys) {
        this.authEnabled = authEnabled;
        this.tenantKeys = parseKeys(configuredKeys);
    }

    public static String traceId() {
        String id = TRACE.get();
        return id == null ? "unknown" : id;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private Map<String, String> parseKeys(String raw) {
        Map<String, String> values = new HashMap<>();
        for (String entry : raw == null ? "".split(",") : raw.split(",")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length == 2 && !pair[0].isBlank() && !pair[1].isBlank()) values.put(pair[0].trim(), pair[1].trim());
        }
        return Map.copyOf(values);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String trace = request.getHeader(TRACE_HEADER);
        TRACE.set(trace == null || trace.isBlank() ? UUID.randomUUID().toString() : trace);
        String tenant = request.getHeader("X-Tenant-Id");
        if (authEnabled && !authorized(tenant, request.getHeader("X-API-Key"))) {
            response.sendError(401, "valid tenant credentials are required");
            TRACE.remove();
            return;
        }
        if (tenant != null && !tenant.isBlank()) TenantContext.set(tenant);
        response.setHeader(TRACE_HEADER, traceId());
        try { chain.doFilter(request, response); } finally { TRACE.remove(); TenantContext.clear(); }
    }

    private boolean authorized(String tenant, String key) {
        if (tenant == null || key == null) return false;
        String expected = tenantKeys.get(tenant);
        return expected != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }
}
