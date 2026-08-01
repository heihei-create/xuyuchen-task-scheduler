package com.xuyuchen.scheduler;

import com.xuyuchen.scheduler.task.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    @Test
    void idempotentCreateReturnsSameTask() {
        TaskService service = new TaskService();
        var req = new TaskDtos.CreateTaskRequest("modal-run", "{\"mesh\":\"demo\"}", "request-1");
        var first = service.create("tenant-a", req);
        var second = service.create("tenant-a", req);
        assertEquals(first.getId(), second.getId());
        assertEquals(1, service.list("tenant-a").size());
    }

    @Test
    void tenantCannotReadAnotherTenantTask() {
        TaskService service = new TaskService();
        var task = service.create("tenant-a", new TaskDtos.CreateTaskRequest("run", "{}", "a"));
        assertThrows(IllegalArgumentException.class, () -> service.get("tenant-b", task.getId()));
    }

    @Test
    void staleWorkerEventIsRejected() {
        TaskService service = new TaskService();
        var task = service.create("tenant-a", new TaskDtos.CreateTaskRequest("run", "{}", "a"));
        service.dispatchReadyTasks();
        var event = new TaskDtos.WorkerEventRequest("local-worker", task.getAttempt(), true, "ok");
        service.start("tenant-a", task.getId(), event);
        service.finish("tenant-a", task.getId(), event);
        assertThrows(IllegalStateException.class, () -> service.finish("tenant-a", task.getId(), event));
        assertEquals(TaskStatus.SUCCESS, task.getStatus());
    }
}
