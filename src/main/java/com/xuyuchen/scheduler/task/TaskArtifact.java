package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.UUID;

public record TaskArtifact(UUID id, UUID taskId, int attempt, String objectKey, String contentType, long size, String checksum, Instant createdAt) {}
