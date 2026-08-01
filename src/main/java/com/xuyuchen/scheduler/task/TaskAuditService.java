package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskAuditService {
    private final CopyOnWriteArrayList<TaskAuditRecord> records = new CopyOnWriteArrayList<>();
    public TaskAuditRecord record(Task task, String operator, TaskStatus from, TaskStatus to, String reason, String traceId, Map<String, Object> details) {
        TaskAuditRecord record = new TaskAuditRecord(task.getId(), task.getTenantId(), operator, from, to, reason, traceId, Instant.now(), details);
        records.add(record); return record;
    }
    public List<TaskAuditRecord> findByTask(String tenantId, UUID taskId) {
        return records.stream().filter(r -> r.tenantId().equals(tenantId) && r.taskId().equals(taskId)).toList();
    }
    public List<TaskAuditRecord> findByTenant(String tenantId, int limit) {
        return records.stream().filter(r -> r.tenantId().equals(tenantId)).skip(Math.max(0, records.size() - limit)).toList();
    }
}
