package com.xuyuchen.scheduler.task;

import java.util.concurrent.atomic.AtomicInteger;

public class TenantQuota {
    private final String tenantId;
    private final int maxRunning;
    private final int maxDaily;
    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger dailySubmitted = new AtomicInteger();
    public TenantQuota(String tenantId, int maxRunning, int maxDaily) {
        this.tenantId = tenantId; this.maxRunning = maxRunning; this.maxDaily = maxDaily;
    }
    public String getTenantId() { return tenantId; }
    public int getMaxRunning() { return maxRunning; }
    public int getMaxDaily() { return maxDaily; }
    public int getRunning() { return running.get(); }
    public int getDailySubmitted() { return dailySubmitted.get(); }
    public boolean reserveSubmission() {
        while (true) {
            int current = dailySubmitted.get();
            if (current >= maxDaily || !dailySubmitted.compareAndSet(current, current + 1)) {
                if (current >= maxDaily) return false;
                continue;
            }
            return true;
        }
    }
    public void rollbackSubmission() { dailySubmitted.updateAndGet(value -> Math.max(0, value - 1)); }
    public boolean tryAcquire() {
        while (true) {
            int current = running.get();
            if (current >= maxRunning || !running.compareAndSet(current, current + 1)) {
                if (current >= maxRunning) return false;
                continue;
            }
            return true;
        }
    }
    public void release() { running.updateAndGet(value -> Math.max(0, value - 1)); }
}
