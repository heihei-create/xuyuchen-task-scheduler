package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskBulkService {
    private final TaskRepository tasks;
    private final TaskCancellationService cancellation;
    private final TaskRetryService retry;
    public TaskBulkService(TaskRepository tasks, TaskCancellationService cancellation, TaskRetryService retry) {
        this.tasks = tasks; this.cancellation = cancellation; this.retry = retry;
    }
    public int cancelFailed(String tenantId, String reason) {
        int count = 0;
        for (Task task : tasks.findByTenant(tenantId)) {
            if (task.getStatus() == TaskStatus.FAILED || task.getStatus() == TaskStatus.TIMEOUT) {
                cancellation.cancel(tenantId, task.getId(), "bulk-operator", reason); count++;
            }
        }
        return count;
    }
    public int retryFailed(String tenantId) {
        int count = 0;
        for (Task task : tasks.findByTenant(tenantId)) {
            if (task.getStatus() == TaskStatus.FAILED || task.getStatus() == TaskStatus.TIMEOUT) {
                try { retry.retry(tenantId, task.getId()); count++; } catch (IllegalStateException ignored) { }
            }
        }
        return count;
    }
}
