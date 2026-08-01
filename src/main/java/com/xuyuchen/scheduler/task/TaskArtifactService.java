package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskArtifactService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<TaskArtifact>> artifacts = new ConcurrentHashMap<>();
    public TaskArtifact register(UUID taskId, int attempt, String objectKey, String contentType, long size, String checksum) {
        if (size < 0 || objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("invalid artifact");
        TaskArtifact artifact = new TaskArtifact(UUID.randomUUID(), taskId, attempt, objectKey, contentType, size, checksum, Instant.now());
        artifacts.computeIfAbsent(taskId, id -> new CopyOnWriteArrayList<>()).add(artifact); return artifact;
    }
    public List<TaskArtifact> list(UUID taskId) { return List.copyOf(artifacts.getOrDefault(taskId, new CopyOnWriteArrayList<>())); }
    public void delete(UUID taskId) { artifacts.remove(taskId); }
}
