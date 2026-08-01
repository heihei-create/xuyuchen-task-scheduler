package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public class TaskTemplate {
    private final UUID id;
    private final String tenantId;
    private final String code;
    private final String name;
    private final String executorType;
    private final int maxAttempts;
    private final TaskPriority priority;
    private final Instant createdAt;
    private volatile boolean enabled;

    public TaskTemplate(UUID id, String tenantId, String code, String name, String executorType, int maxAttempts, TaskPriority priority) {
        if (maxAttempts < 1 || maxAttempts > 10) throw new IllegalArgumentException("maxAttempts must be between 1 and 10");
        this.id = id; this.tenantId = tenantId; this.code = code; this.name = name; this.executorType = executorType;
        this.maxAttempts = maxAttempts; this.priority = priority; this.createdAt = Instant.now(); this.enabled = true;
    }
    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getExecutorType() { return executorType; }
    public int getMaxAttempts() { return maxAttempts; }
    public TaskPriority getPriority() { return priority; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isEnabled() { return enabled; }
    public void enable() { enabled = true; }
    public void disable() { enabled = false; }
}
