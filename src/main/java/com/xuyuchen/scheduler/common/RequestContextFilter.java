package com.xuyuchen.scheduler.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component("schedulerRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    private static final ThreadLocal<String> TRACE = new ThreadLocal<>();

    public static String traceId() {
        String id = TRACE.get();
        return id == null ? "unknown" : id;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String trace = request.getHeader(TRACE_HEADER);
        TRACE.set(trace == null || trace.isBlank() ? UUID.randomUUID().toString() : trace);
        String tenant = request.getHeader("X-Tenant-Id");
        if (tenant != null && !tenant.isBlank()) TenantContext.set(tenant);
        response.setHeader(TRACE_HEADER, traceId());
        try { chain.doFilter(request, response); } finally { TRACE.remove(); TenantContext.clear(); }
    }
}
