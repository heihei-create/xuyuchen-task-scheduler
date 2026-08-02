package com.xuyuchen.scheduler.task;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class TaskLifecycleConcurrencyTest {
    @Test
    void concurrentRetriesOfSameIdempotencyKeyCreateOneTaskAndConsumeOneQuotaSlot() throws Exception {
        InMemoryTaskRepository tasks = new InMemoryTaskRepository();
        InMemoryTemplateRepository templateRepository = new InMemoryTemplateRepository();
        TaskTemplateService templates = new TaskTemplateService(templateRepository);
        templates.create("tenant-a", "modal-run", "Modal run", "modal", 3, TaskPriority.NORMAL);
        TenantQuotaService quotas = new TenantQuotaService();
        quotas.configure("tenant-a", 2, 1);
        TaskLifecycleService lifecycle = new TaskLifecycleService(tasks, templates, quotas, new WorkerRegistry(), new TaskAuditService(), mock(TaskEventPublisher.class), mock(DispatchOrchestrator.class), new TaskPayloadValidator());

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Task>> calls = new ArrayList<>();
            for (int i = 0; i < 32; i++) calls.add(() -> lifecycle.create("tenant-a", "modal-run", "{}", "same-request"));
            List<Task> results = pool.invokeAll(calls).stream().map(future -> {
                try { return future.get(); } catch (Exception ex) { throw new AssertionError(ex); }
            }).toList();

            UUID id = results.get(0).getId();
            assertNotNull(id);
            assertEquals(1, results.stream().map(Task::getId).distinct().count());
            assertEquals(1, tasks.countByTenant("tenant-a"));
            assertEquals(1, quotas.require("tenant-a").getDailySubmitted());
        } finally {
            pool.shutdownNow();
        }
    }
}
