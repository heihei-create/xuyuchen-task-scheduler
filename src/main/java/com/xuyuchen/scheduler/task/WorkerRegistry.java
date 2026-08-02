package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkerRegistry {
    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final long heartbeatTimeoutSeconds = 30;

    public WorkerInfo register(String workerId, String host, Set<String> capabilities) {
        return register("default", workerId, host, capabilities);
    }
    public WorkerInfo register(String tenantId, String workerId, String host, Set<String> capabilities) {
        if (tenantId == null || tenantId.isBlank() || workerId == null || workerId.isBlank()) throw new IllegalArgumentException("tenant and worker are required");
        if (capabilities == null || capabilities.isEmpty()) throw new IllegalArgumentException("worker capabilities are required");
        final WorkerInfo[] registered = new WorkerInfo[1];
        workers.compute(workerId, (id, existing) -> {
            if (existing != null && !existing.getTenantId().equals(tenantId)) throw new IllegalStateException("worker id is already registered by another tenant");
            WorkerInfo worker = new WorkerInfo(tenantId, workerId, host, capabilities);
            worker.ready(); registered[0] = worker; return worker;
        });
        return registered[0];
    }
    public WorkerInfo require(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker == null) throw new IllegalArgumentException("worker not found");
        return worker;
    }
    public java.util.Optional<WorkerInfo> find(String workerId) { return java.util.Optional.ofNullable(workers.get(workerId)); }
    public WorkerInfo heartbeat(String workerId, int runningTasks) {
        WorkerInfo worker = require(workerId); worker.heartbeat(runningTasks); return worker;
    }
    public List<WorkerInfo> list() { return workers.values().stream().toList(); }
    public List<WorkerInfo> available(String executorType) {
        return available(null, executorType);
    }
    public List<WorkerInfo> available(String tenantId, String executorType) {
        reapOffline();
        return workers.values().stream().filter(w -> (tenantId == null || w.getTenantId().equals(tenantId) || w.getTenantId().equals("default")) && w.canRun(executorType)).toList();
    }
    public List<WorkerInfo> schedulable(String tenantId, String executorType) {
        reapOffline();
        return workers.values().stream().filter(w -> (tenantId == null || w.getTenantId().equals(tenantId) || w.getTenantId().equals("default"))
                && (w.getStatus() == WorkerStatus.READY || w.getStatus() == WorkerStatus.BUSY) && w.getCapabilities().contains(executorType)).toList();
    }
    public void drain(String workerId) { require(workerId).drain(); }
    public void reapOffline() { workers.values().stream().filter(w -> !w.alive(heartbeatTimeoutSeconds)).forEach(w -> { w.drain(); }); }
}
