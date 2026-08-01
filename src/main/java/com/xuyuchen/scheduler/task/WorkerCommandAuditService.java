package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WorkerCommandAuditService {
    private final CopyOnWriteArrayList<CommandAudit> records = new CopyOnWriteArrayList<>();
    public CommandAudit dispatched(WorkerCommand command) {
        CommandAudit audit = new CommandAudit(UUID.randomUUID(), command.taskId(), command.workerId(), "DISPATCHED", command.attempt(), Instant.now(), null);
        records.add(audit); return audit;
    }
    public CommandAudit acknowledged(UUID commandId, String workerId) {
        CommandAudit current = records.stream().filter(r -> r.id().equals(commandId)).findFirst().orElseThrow(() -> new IllegalArgumentException("command audit not found"));
        CommandAudit next = new CommandAudit(current.id(), current.taskId(), workerId, "ACKNOWLEDGED", current.attempt(), current.createdAt(), Instant.now());
        records.remove(current); records.add(next); return next;
    }
    public List<CommandAudit> list(UUID taskId) { return records.stream().filter(r -> r.taskId().equals(taskId)).toList(); }
    public record CommandAudit(UUID id, UUID taskId, String workerId, String status, int attempt, Instant createdAt, Instant acknowledgedAt) {}
}
