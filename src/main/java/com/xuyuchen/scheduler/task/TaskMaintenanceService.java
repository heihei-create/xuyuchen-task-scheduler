package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TaskMaintenanceService {
    private final TaskRepository tasks;
    private final TaskLogService logs;
    private final TaskArtifactService artifacts;
    private final TaskProgressService progress;
    public TaskMaintenanceService(TaskRepository tasks, TaskLogService logs, TaskArtifactService artifacts, TaskProgressService progress) {
        this.tasks = tasks; this.logs = logs; this.artifacts = artifacts; this.progress = progress;
    }
    public MaintenanceSummary summarize(String tenantId) {
        List<Task> records = tasks.findByTenant(tenantId);
        Map<TaskStatus, Long> byStatus = records.stream().collect(java.util.stream.Collectors.groupingBy(Task::getStatus, java.util.stream.Collectors.counting()));
        long active = records.stream().filter(t -> t.getStatus() == TaskStatus.RUNNING || t.getStatus() == TaskStatus.DISPATCHED).count();
        return new MaintenanceSummary(tenantId, records.size(), active, byStatus, Instant.now());
    }
    public void cleanup(String tenantId) {
        tasks.findByTenant(tenantId).stream().filter(t -> t.getStatus() == TaskStatus.CANCELED).forEach(t -> {
            logs.clear(t.getId()); artifacts.delete(t.getId()); progress.remove(t.getId());
        });
    }
    public record MaintenanceSummary(String tenantId, long total, long active, Map<TaskStatus, Long> byStatus, Instant generatedAt) {}
}
