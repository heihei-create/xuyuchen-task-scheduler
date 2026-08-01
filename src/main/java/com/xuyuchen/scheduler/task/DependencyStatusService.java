package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DependencyStatusService {
    private final DependencyRepository dependencies;
    private final TaskRepository tasks;
    public DependencyStatusService(DependencyRepository dependencies, TaskRepository tasks) { this.dependencies = dependencies; this.tasks = tasks; }
    public DependencySummary summary(UUID taskId) {
        List<TaskDependency> edges = dependencies.findByChild(taskId);
        int total = edges.size();
        int success = 0, failed = 0, pending = 0;
        for (TaskDependency edge : edges) {
            Task parent = tasks.findById(edge.parentTaskId()).orElse(null);
            if (parent == null) { pending++; continue; }
            if (parent.getStatus() == TaskStatus.SUCCESS) success++;
            else if (parent.getStatus() == TaskStatus.FAILED || parent.getStatus() == TaskStatus.TIMEOUT) failed++;
            else pending++;
        }
        return new DependencySummary(taskId, total, success, failed, pending, failed == 0 && pending == 0);
    }
    public record DependencySummary(UUID taskId, int total, int success, int failed, int pending, boolean releasable) {}
}
