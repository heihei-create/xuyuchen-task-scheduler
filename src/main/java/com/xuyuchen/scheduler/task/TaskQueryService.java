package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskQueryService {
    private final TaskRepository repository;
    public TaskQueryService(TaskRepository repository) { this.repository = repository; }
    public PageResult<Task> page(String tenantId, int page, int size, TaskStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Task> all = status == null ? repository.findByTenant(tenantId) : repository.findByStatus(status).stream().filter(t -> t.getTenantId().equals(tenantId)).toList();
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return PageResult.of(all.subList(from, to), all.size(), safePage, safeSize);
    }
    public Task require(String tenantId, UUID taskId) {
        Task task = repository.findById(taskId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("task not found");
        return task;
    }
}
