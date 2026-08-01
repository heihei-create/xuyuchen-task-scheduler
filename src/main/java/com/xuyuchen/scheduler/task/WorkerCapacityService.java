package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkerCapacityService {
    private final Map<String, Integer> capacities = new ConcurrentHashMap<>();
    public int configure(String workerId, int capacity) {
        if (capacity < 1 || capacity > 100) throw new IllegalArgumentException("capacity must be between 1 and 100");
        capacities.put(workerId, capacity); return capacity;
    }
    public boolean hasCapacity(WorkerInfo worker) { return worker.getRunningTasks() < capacities.getOrDefault(worker.getWorkerId(), 1); }
    public int capacity(String workerId) { return capacities.getOrDefault(workerId, 1); }
}
