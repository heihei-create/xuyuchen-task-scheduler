package com.xuyuchen.scheduler.task;

import java.time.Instant;

public record TaskFilter(TaskStatus status, String name, TaskPriority priority, Instant createdAfter, Instant createdBefore, String workerId) {
    public boolean matches(Task task) {
        if (status != null && task.getStatus() != status) return false;
        if (name != null && !name.isBlank() && !task.getName().contains(name)) return false;
        if (createdAfter != null && task.getCreatedAt().isBefore(createdAfter)) return false;
        if (createdBefore != null && task.getCreatedAt().isAfter(createdBefore)) return false;
        if (workerId != null && !workerId.equals(task.getWorkerId())) return false;
        return true;
    }
}
