package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskResultService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<TaskResult>> results = new ConcurrentHashMap<>();
    public TaskResult save(UUID taskId, int attempt, boolean success, String summary, String outputKey, String checksum) {
        TaskResult result = new TaskResult(taskId, attempt, success, summary, outputKey, checksum, Instant.now());
        results.computeIfAbsent(taskId, key -> new CopyOnWriteArrayList<>()).add(result); return result;
    }
    public List<TaskResult> list(UUID taskId) { return List.copyOf(results.getOrDefault(taskId, new CopyOnWriteArrayList<>())); }
    public TaskResult latest(UUID taskId) { return results.getOrDefault(taskId, new CopyOnWriteArrayList<>()).stream().reduce((a, b) -> b).orElse(null); }
}
