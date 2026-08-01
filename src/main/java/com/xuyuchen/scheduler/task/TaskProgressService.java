package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskProgressService {
    private final Map<UUID, TaskProgress> progress = new ConcurrentHashMap<>();
    public TaskProgress update(UUID taskId, String phase, int percent, String message) {
        TaskProgress value = new TaskProgress(phase, percent, message, Instant.now());
        progress.put(taskId, value); return value;
    }
    public TaskProgress get(UUID taskId) { return progress.getOrDefault(taskId, new TaskProgress("pending", 0, "not started", Instant.now())); }
    public void complete(UUID taskId, String message) { update(taskId, "completed", 100, message); }
    public void fail(UUID taskId, String message) { update(taskId, "failed", 100, message); }
    public void remove(UUID taskId) { progress.remove(taskId); }
}
