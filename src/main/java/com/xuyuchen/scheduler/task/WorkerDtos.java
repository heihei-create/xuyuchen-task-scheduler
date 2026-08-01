package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public final class WorkerDtos {
    private WorkerDtos() {}
    public record RegisterRequest(@NotBlank String workerId, @NotBlank String host, Set<String> capabilities) {}
    public record HeartbeatRequest(int runningTasks) {}
    public record WorkerResponse(String workerId, String host, Set<String> capabilities, WorkerStatus status, int runningTasks, java.time.Instant lastHeartbeat) {
        static WorkerResponse from(WorkerInfo w) { return new WorkerResponse(w.getWorkerId(), w.getHost(), w.getCapabilities(), w.getStatus(), w.getRunningTasks(), w.getLastHeartbeat()); }
    }
}
