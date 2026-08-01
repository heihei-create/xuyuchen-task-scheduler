package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskRetryPolicyService {
    private final Map<UUID, RetryPolicy> policies = new ConcurrentHashMap<>();
    public RetryPolicy configure(UUID taskId, RetryPolicy policy) { policies.put(taskId, policy); return policy; }
    public RetryPolicy require(UUID taskId) { return policies.getOrDefault(taskId, new RetryPolicy(3, Duration.ofSeconds(5), 2, Duration.ofMinutes(5))); }
    public boolean shouldRetry(UUID taskId, int attempt) { return require(taskId).canRetry(attempt); }
    public Duration nextDelay(UUID taskId, int attempt) { return require(taskId).delayFor(attempt); }
    public void remove(UUID taskId) { policies.remove(taskId); }
}
