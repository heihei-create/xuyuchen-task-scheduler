package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
public class SchedulerMetrics {
    private final LongAdder submitted = new LongAdder();
    private final LongAdder dispatched = new LongAdder();
    private final LongAdder succeeded = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder timedOut = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final AtomicLong lastDispatchAt = new AtomicLong();
    public void submitted() { submitted.increment(); }
    public void dispatched() { dispatched.increment(); lastDispatchAt.set(System.currentTimeMillis()); }
    public void succeeded() { succeeded.increment(); }
    public void failed() { failed.increment(); }
    public void timedOut() { timedOut.increment(); }
    public void rejected() { rejected.increment(); }
    public SchedulerMetricSnapshot snapshot() {
        return new SchedulerMetricSnapshot(submitted.sum(), dispatched.sum(), succeeded.sum(), failed.sum(), timedOut.sum(), rejected.sum(), lastDispatchAt.get());
    }
    public record SchedulerMetricSnapshot(long submitted, long dispatched, long succeeded, long failed, long timedOut, long rejected, long lastDispatchAt) {
        public long terminal() { return succeeded + failed + timedOut; }
        public double successRate() { return terminal() == 0 ? 0 : (double) succeeded / terminal(); }
    }
}
