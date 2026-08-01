package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskLogService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<TaskLogEntry>> logs = new ConcurrentHashMap<>();
    public TaskLogEntry append(UUID taskId, int attempt, String level, String message, String traceId) {
        TaskLogEntry entry = new TaskLogEntry(taskId, attempt, level, message, traceId, Instant.now());
        logs.computeIfAbsent(taskId, id -> new CopyOnWriteArrayList<>()).add(entry); return entry;
    }
    public List<TaskLogEntry> list(UUID taskId, String level) {
        return logs.getOrDefault(taskId, new CopyOnWriteArrayList<>()).stream().filter(e -> level == null || e.level().equalsIgnoreCase(level)).toList();
    }
    public void clear(UUID taskId) { logs.remove(taskId); }
}
