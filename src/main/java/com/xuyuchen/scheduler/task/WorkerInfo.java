package com.xuyuchen.scheduler.task;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerInfo {
    private final String workerId;
    private final String tenantId;
    private final String host;
    private final Set<String> capabilities = ConcurrentHashMap.newKeySet();
    private volatile WorkerStatus status;
    private volatile Instant lastHeartbeat;
    private final AtomicInteger runningTasks = new AtomicInteger();

    public WorkerInfo(String workerId, String host, Set<String> capabilities) {
        this("default", workerId, host, capabilities);
    }
    public WorkerInfo(String tenantId, String workerId, String host, Set<String> capabilities) {
        this.tenantId = tenantId;
        this.workerId = workerId; this.host = host; this.capabilities.addAll(capabilities);
        this.status = WorkerStatus.STARTING; this.lastHeartbeat = Instant.now();
    }
    public String getWorkerId() { return workerId; }
    public String getTenantId() { return tenantId; }
    public String getHost() { return host; }
    public Set<String> getCapabilities() { return Set.copyOf(capabilities); }
    public WorkerStatus getStatus() { return status; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public int getRunningTasks() { return runningTasks.get(); }
    public void ready() { status = WorkerStatus.READY; lastHeartbeat = Instant.now(); }
    public void heartbeat(int runningTasks) { int current = this.runningTasks.updateAndGet(value -> Math.max(0, runningTasks)); status = current == 0 ? WorkerStatus.READY : WorkerStatus.BUSY; lastHeartbeat = Instant.now(); }
    public void markTaskStarted() { runningTasks.incrementAndGet(); status = WorkerStatus.BUSY; lastHeartbeat = Instant.now(); }
    public void markTaskFinished() { int current = runningTasks.updateAndGet(value -> Math.max(0, value - 1)); status = current == 0 ? WorkerStatus.READY : WorkerStatus.BUSY; lastHeartbeat = Instant.now(); }
    public void drain() { status = WorkerStatus.DRAINING; }
    public boolean alive(long timeoutSeconds) { return lastHeartbeat.plusSeconds(timeoutSeconds).isAfter(Instant.now()); }
    public boolean canRun(String executorType) { return status == WorkerStatus.READY && capabilities.contains(executorType); }
}
