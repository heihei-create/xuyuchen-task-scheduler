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
        WorkerInfo worker = new WorkerInfo(workerId, host, capabilities);
        worker.ready(); workers.put(workerId, worker); return worker;
    }
    public WorkerInfo require(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker == null) throw new IllegalArgumentException("worker not found");
        return worker;
    }
    public WorkerInfo heartbeat(String workerId, int runningTasks) {
        WorkerInfo worker = require(workerId); worker.heartbeat(runningTasks); return worker;
    }
    public List<WorkerInfo> list() { return workers.values().stream().toList(); }
    public List<WorkerInfo> available(String executorType) {
        reapOffline();
        return workers.values().stream().filter(w -> w.canRun(executorType)).toList();
    }
    public void drain(String workerId) { require(workerId).drain(); }
    public void reapOffline() { workers.values().stream().filter(w -> !w.alive(heartbeatTimeoutSeconds)).forEach(w -> { w.drain(); }); }
}
