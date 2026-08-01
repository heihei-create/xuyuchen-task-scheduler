package com.xuyuchen.scheduler.task;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerCoreTest {
    @Test
    void taskServiceUsesRepositoryForLifecycleState() {
        InMemoryTaskRepository repository = new InMemoryTaskRepository();
        TaskService service = new TaskService(repository);

        Task task = service.create("tenant-a", new TaskDtos.CreateTaskRequest("run", "{}", "key-1"));
        assertSame(task, repository.findById(task.getId()).orElseThrow());

        service.dispatchReadyTasks();
        service.start("tenant-a", task.getId(), new TaskDtos.WorkerEventRequest("local-worker", 1, true, null));
        service.finish("tenant-a", task.getId(), new TaskDtos.WorkerEventRequest("local-worker", 1, true, "done"));

        assertEquals(TaskStatus.SUCCESS, repository.findById(task.getId()).orElseThrow().getStatus());
        assertEquals("done", repository.findById(task.getId()).orElseThrow().getResult());
    }

    @Test
    void taskCreationIsIdempotentUnderConcurrentRequests() throws Exception {
        TaskService service = new TaskService(new InMemoryTaskRepository());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Task>> futures = java.util.stream.IntStream.range(0, 24)
                    .mapToObj(index -> executor.submit(() -> service.create("tenant-a",
                            new TaskDtos.CreateTaskRequest("run", "{}", "same-key"))))
                    .toList();
            List<Task> created = futures.stream().map(this::join).toList();
            assertEquals(1, created.stream().map(Task::getId).distinct().count());
            assertEquals(1, service.list("tenant-a").size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void jdbcRepositoryRestoresAllTaskExecutionFields() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();
        try {
            JdbcTaskRepository repository = new JdbcTaskRepository(new NamedParameterJdbcTemplate(dataSource));
            Task original = new Task(UUID.randomUUID(), "tenant-a", "run", "{\"mesh\":1}", "key-1");
            original.ready();
            original.dispatch("worker-1", 30);
            repository.save(original);

            Task restored = repository.findById(original.getId()).orElseThrow();
            assertEquals(original.getId(), restored.getId());
            assertEquals(original.getCreatedAt(), restored.getCreatedAt());
            assertEquals(TaskStatus.DISPATCHED, restored.getStatus());
            assertEquals(1, restored.getAttempt());
            assertEquals("worker-1", restored.getWorkerId());
            assertNotNull(restored.getLeaseUntil());
        } finally {
            ((org.springframework.jdbc.datasource.embedded.EmbeddedDatabase) dataSource).shutdown();
        }
    }

    @Test
    void dispatchDoesNotRunBeforeDagParentsSucceed() {
        InMemoryTaskRepository tasks = new InMemoryTaskRepository();
        InMemoryTemplateRepository templateRepository = new InMemoryTemplateRepository();
        TaskTemplateService templates = new TaskTemplateService(templateRepository);
        templates.create("tenant-a", "solver-run", "Solver", "solver", 3, TaskPriority.NORMAL);
        WorkerRegistry workers = new WorkerRegistry();
        workers.register("worker-1", "host-a", Set.of("solver"));
        TenantQuotaService quotas = new TenantQuotaService();
        TaskLeaseService leases = new TaskLeaseService(tasks);
        InMemoryWorkerCommandPublisher commands = new InMemoryWorkerCommandPublisher();
        InMemoryTaskEventPublisher events = new InMemoryTaskEventPublisher();
        InMemoryDependencyRepository dependencies = new InMemoryDependencyRepository();
        DispatchOrchestrator orchestrator = new DispatchOrchestrator(tasks, templates, workers, quotas, leases,
                commands, events, new TaskAuditService(), new TaskDispatchLockService(new InMemoryDistributedLock()), dependencies);

        Task parent = task("tenant-a", "solver-run", "parent");
        Task child = task("tenant-a", "solver-run", "child");
        tasks.save(parent);
        tasks.save(child);
        dependencies.save(new TaskDependency(parent.getId(), child.getId(), 0));

        assertFalse(orchestrator.dispatchOne(child));
        assertEquals(TaskStatus.READY, child.getStatus());

        parent.dispatch("worker-1", 30);
        parent.start("worker-1", 1);
        parent.finish("worker-1", 1, true, "ok");
        assertTrue(orchestrator.dispatchOne(child));
        assertEquals(TaskStatus.DISPATCHED, child.getStatus());
    }

    @Test
    void taskLockIsReleasedAfterDispatch() {
        InMemoryDistributedLock lock = new InMemoryDistributedLock();
        TaskDispatchLockService locks = new TaskDispatchLockService(lock);
        assertTrue(locks.tryTask("task-1", "scheduler-a"));
        assertTrue(locks.releaseTask("task-1", "scheduler-a"));
        assertTrue(locks.tryTask("task-1", "scheduler-b"));
    }

    @Test
    void progressRequiresThePresentedLeaseToken() {
        InMemoryTaskRepository tasks = new InMemoryTaskRepository();
        Task task = task("tenant-a", "run", "key-1");
        task.dispatch("worker-1", 30);
        tasks.save(task);
        TaskLeaseService leases = new TaskLeaseService(tasks);
        TaskLease lease = leases.issue(task.getId(), "worker-1", task.getAttempt(), 30);
        WorkerExecutionService service = executionService(tasks, leases);

        assertThrows(IllegalStateException.class, () -> service.progress("tenant-a", task.getId(),
                new WorkerProgressRequest("worker-1", 1, "wrong-token", 25, "mesh", "running")));
        TaskProgress progress = service.progress("tenant-a", task.getId(),
                new WorkerProgressRequest("worker-1", 1, lease.token(), 25, "mesh", "running"));
        assertEquals(25, progress.percent());
    }

    @Test
    void wrongOwnerCannotReleaseInMemoryTaskLock() {
        InMemoryDistributedLock lock = new InMemoryDistributedLock();
        assertTrue(lock.tryAcquire("k", "owner-a", java.time.Duration.ofSeconds(30)));
        assertFalse(lock.release("k", "owner-b"));
        assertFalse(lock.tryAcquire("k", "owner-b", java.time.Duration.ofSeconds(30)));
        assertTrue(lock.release("k", "owner-a"));
    }

    private Task task(String tenant, String name, String key) {
        Task task = new Task(UUID.randomUUID(), tenant, name, "{}", key);
        task.ready();
        return task;
    }

    private WorkerExecutionService executionService(InMemoryTaskRepository tasks, TaskLeaseService leases) {
        return new WorkerExecutionService(tasks, leases, new WorkerRegistry(), new TaskProgressService(),
                new TaskLogService(), new TaskArtifactService(), new TaskAuditService(),
                new InMemoryTaskEventPublisher(), new TenantQuotaService());
    }

    private Task join(Future<Task> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
