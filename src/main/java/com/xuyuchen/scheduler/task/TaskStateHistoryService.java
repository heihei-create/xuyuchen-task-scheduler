package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskStateHistoryService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<TaskStateHistory>> history = new ConcurrentHashMap<>();
    public TaskStateHistory append(UUID taskId, TaskStatus from, TaskStatus to, int attempt, String actor, String reason) {
        TaskStateHistory value = new TaskStateHistory(taskId, from, to, attempt, actor, reason, Instant.now());
        history.computeIfAbsent(taskId, id -> new CopyOnWriteArrayList<>()).add(value); return value;
    }
    public List<TaskStateHistory> list(UUID taskId) { return List.copyOf(history.getOrDefault(taskId, new CopyOnWriteArrayList<>())); }
    public TaskStateHistory latest(UUID taskId) { return history.getOrDefault(taskId, new CopyOnWriteArrayList<>()).stream().reduce((a, b) -> b).orElse(null); }
    public void clear(UUID taskId) { history.remove(taskId); }
}
