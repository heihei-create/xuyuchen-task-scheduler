package com.xuyuchen.scheduler.task;

import java.time.Duration;

public interface DistributedLock {
    boolean tryAcquire(String key, String owner, Duration ttl);
    boolean release(String key, String owner);
}
