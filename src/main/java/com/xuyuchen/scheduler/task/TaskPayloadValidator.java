package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TaskPayloadValidator {
    private static final Set<String> ALLOWED_EXECUTORS = Set.of("modal", "python", "shell", "solver");
    public void validate(TaskTemplate template, String payload) {
        if (!ALLOWED_EXECUTORS.contains(template.getExecutorType())) throw new IllegalArgumentException("unsupported executor type");
        if (payload == null || payload.length() > 1_000_000) throw new IllegalArgumentException("payload is empty or too large");
        if (payload.contains("\\u0000")) throw new IllegalArgumentException("payload contains invalid character");
    }
    public boolean supports(String executorType) { return ALLOWED_EXECUTORS.contains(executorType); }
}
