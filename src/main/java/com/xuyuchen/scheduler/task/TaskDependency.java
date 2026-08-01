package com.xuyuchen.scheduler.task;

import java.util.UUID;

public record TaskDependency(UUID parentTaskId, UUID childTaskId, int order) {
    public TaskDependency {
        if (parentTaskId.equals(childTaskId)) throw new IllegalArgumentException("task cannot depend on itself");
        if (order < 0) throw new IllegalArgumentException("dependency order must be non-negative");
    }
}
