package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulePolicyService {
    private final Map<UUID, SchedulePolicy> policies = new ConcurrentHashMap<>();
    public SchedulePolicy configure(UUID taskId, SchedulePolicy policy) { policies.put(taskId, policy); return policy; }
    public SchedulePolicy get(UUID taskId) { return policies.getOrDefault(taskId, SchedulePolicy.standard(3)); }
    public void remove(UUID taskId) { policies.remove(taskId); }
}
