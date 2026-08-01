package com.xuyuchen.scheduler.task;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskCoreTest {
    @Test
    void leaseTokenSurvivesRepositoryReload() {
        InMemoryTaskRepository repository = new InMemoryTaskRepository();
        Task task = new Task(UUID.randomUUID(), "tenant-a", "run", "{}", "key");
        task.ready(); task.dispatch("worker-a", 30); repository.save(task);
        TaskLeaseService first = new TaskLeaseService(repository);
        TaskLease lease = first.issue(task.getId(), "worker-a", task.getAttempt(), 30);
        TaskLeaseService restarted = new TaskLeaseService(repository);
        assertEquals(lease.token(), restarted.require(task.getId()).token());
        assertTrue(restarted.require(task.getId()).matches("worker-a", task.getAttempt(), lease.token()));
    }

    @Test
    void staleWorkerCannotFinishAfterDifferentAttempt() {
        Task task = new Task(UUID.randomUUID(), "tenant-a", "run", "{}", "key");
        task.ready(); task.dispatch("worker-a", 30); assertTrue(task.start("worker-a", 1));
        assertFalse(task.finish("worker-a", 2, true, "stale"));
        assertEquals(TaskStatus.RUNNING, task.getStatus());
    }

    @Test
    void dagValidatorRejectsCycle() {
        InMemoryDependencyRepository repository = new InMemoryDependencyRepository();
        TaskDAGValidator validator = new TaskDAGValidator(repository);
        UUID a = UUID.randomUUID(); UUID b = UUID.randomUUID();
        validator.addDependency(new TaskDependency(a, b, 0));
        assertThrows(IllegalArgumentException.class, () -> validator.addDependency(new TaskDependency(b, a, 0)));
    }

    @Test
    void dagIsReadyOnlyWhenAllParentsSucceeded() {
        InMemoryDependencyRepository repository = new InMemoryDependencyRepository();
        TaskDAGValidator validator = new TaskDAGValidator(repository);
        UUID parent = UUID.randomUUID(); UUID child = UUID.randomUUID();
        validator.addDependency(new TaskDependency(parent, child, 0));
        assertFalse(validator.ready(child, Set.of(TaskStatus.SUCCESS), java.util.Map.of(parent, TaskStatus.RUNNING)));
        assertTrue(validator.ready(child, Set.of(TaskStatus.SUCCESS), java.util.Map.of(parent, TaskStatus.SUCCESS)));
    }

    @Test
    void restoredTaskKeepsTerminalStateAndCreatedAt() {
        UUID id = UUID.randomUUID(); Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Task task = Task.restore(id, "tenant-a", "run", "{}", "key", created, TaskStatus.SUCCESS, 2, null, "worker-a", "done", null);
        assertEquals(created, task.getCreatedAt());
        assertEquals(TaskStatus.SUCCESS, task.getStatus());
        assertEquals(2, task.getAttempt());
    }

    @Test
    void timeoutClearsPersistedLeaseAndCanBeDetectedAfterRestart() throws Exception {
        InMemoryTaskRepository repository = new InMemoryTaskRepository();
        Task task = new Task(UUID.randomUUID(), "tenant-a", "run", "{}", "key");
        task.ready();
        task.dispatch("worker-a", 1);
        repository.save(task);
        TaskLeaseService first = new TaskLeaseService(repository);
        first.issue(task.getId(), "worker-a", task.getAttempt(), 1);
        Thread.sleep(1100);
        TaskLeaseService restarted = new TaskLeaseService(repository);
        assertTrue(restarted.expired(task.getId()));
        assertTrue(task.timeout());
        assertNull(task.getLeaseUntil());
        assertNull(task.getLeaseToken());
    }
}
