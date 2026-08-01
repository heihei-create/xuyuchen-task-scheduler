package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TaskDispatchLockService {
    private final DistributedLock lock;
    public TaskDispatchLockService(DistributedLock lock) { this.lock = lock; }
    public boolean tryTenant(String tenantId, String schedulerId) { return lock.tryAcquire("scheduler:tenant:" + tenantId, schedulerId, Duration.ofSeconds(10)); }
    public boolean releaseTenant(String tenantId, String schedulerId) { return lock.release("scheduler:tenant:" + tenantId, schedulerId); }
    public boolean tryTask(String taskId, String schedulerId) { return lock.tryAcquire("scheduler:task:" + taskId, schedulerId, Duration.ofSeconds(30)); }
    public boolean releaseTask(String taskId, String schedulerId) { return lock.release("scheduler:task:" + taskId, schedulerId); }
}
