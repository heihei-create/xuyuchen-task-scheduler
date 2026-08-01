package com.xuyuchen.scheduler.task;

import com.xuyuchen.scheduler.common.PageResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class TaskSearchService {
    private final TaskRepository tasks;
    public TaskSearchService(TaskRepository tasks) { this.tasks = tasks; }
    public PageResult<Task> search(String tenantId, TaskFilter filter, int page, int size) {
        var all = tasks.findByTenant(tenantId).stream().filter(filter == null ? t -> true : filter::matches)
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed()).toList();
        int safePage = Math.max(0, page), safeSize = Math.min(Math.max(1, size), 100);
        int from = Math.min(safePage * safeSize, all.size()), to = Math.min(from + safeSize, all.size());
        return PageResult.of(all.subList(from, to), all.size(), safePage, safeSize);
    }
}
