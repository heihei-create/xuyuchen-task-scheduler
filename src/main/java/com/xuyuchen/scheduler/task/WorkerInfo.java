package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerInfo {
    private final String workerId;
    private final String host;
    private final Set<String> capabilities = ConcurrentHashMap.newKeySet();
    private volatile WorkerStatus status;
    private volatile Instant lastHeartbeat;
    private volatile int runningTasks;

    public WorkerInfo(String workerId, String host, Set<String> capabilities) {
        this.workerId = workerId; this.host = host; this.capabilities.addAll(capabilities);
        this.status = WorkerStatus.STARTING; this.lastHeartbeat = Instant.now();
    }
    public String getWorkerId() { return workerId; }
    public String getHost() { return host; }
    public Set<String> getCapabilities() { return Set.copyOf(capabilities); }
    public WorkerStatus getStatus() { return status; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public int getRunningTasks() { return runningTasks; }
    public void ready() { status = WorkerStatus.READY; lastHeartbeat = Instant.now(); }
    public void heartbeat(int runningTasks) { this.runningTasks = Math.max(0, runningTasks); status = WorkerStatus.BUSY.equals(status) && runningTasks == 0 ? WorkerStatus.READY : status; lastHeartbeat = Instant.now(); }
    public void markTaskStarted() { runningTasks++; status = WorkerStatus.BUSY; lastHeartbeat = Instant.now(); }
    public void markTaskFinished() { runningTasks = Math.max(0, runningTasks - 1); status = runningTasks == 0 ? WorkerStatus.READY : WorkerStatus.BUSY; lastHeartbeat = Instant.now(); }
    public void drain() { status = WorkerStatus.DRAINING; }
    public boolean alive(long timeoutSeconds) { return lastHeartbeat.plusSeconds(timeoutSeconds).isAfter(Instant.now()); }
    public boolean canRun(String executorType) { return status == WorkerStatus.READY && capabilities.contains(executorType); }
}
