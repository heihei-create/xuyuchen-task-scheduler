package com.xuyuchen.scheduler.task;

import java.util.UUID;

public record TaskGraphNode(UUID taskId, String taskCode, String executorType, int depth, boolean terminal) {}
