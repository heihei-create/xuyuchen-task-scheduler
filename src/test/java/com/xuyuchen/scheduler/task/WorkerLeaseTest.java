package com.xuyuchen.scheduler.task;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkerLeaseTest {
    @Test
    void releaseClearsPersistentLease() {
        InMemoryTaskRepository repository = new InMemoryTaskRepository();
        Task task = new Task(UUID.randomUUID(), "tenant-a", "run", "{}", "key");
        task.ready(); task.dispatch("worker-a", 30); repository.save(task);
        TaskLeaseService leases = new TaskLeaseService(repository);
        TaskLease lease = leases.issue(task.getId(), "worker-a", task.getAttempt(), 30);
        leases.release(task.getId(), "worker-a", task.getAttempt(), lease.token());
        assertThrows(IllegalStateException.class, () -> leases.require(task.getId()));
        assertNull(repository.findById(task.getId()).orElseThrow().getLeaseToken());
    }
}
