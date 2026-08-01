package com.xuyuchen.scheduler.task;

import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
public class TaskPolicyController {
    private final SchedulePolicyService schedules;
    private final TaskRetryPolicyService retries;
    public TaskPolicyController(SchedulePolicyService schedules, TaskRetryPolicyService retries) { this.schedules = schedules; this.retries = retries; }
    public record PolicyRequest(@Min(1) long timeoutSeconds, @Min(1) long heartbeatSeconds, @Min(1) int maxAttempts, boolean allowManualRetry, boolean allowCancel) {}
    @PutMapping("/{taskId}")
    public SchedulePolicy put(@PathVariable UUID taskId, @RequestBody PolicyRequest request) {
        SchedulePolicy policy = new SchedulePolicy(Duration.ofSeconds(request.timeoutSeconds()), Duration.ofSeconds(request.heartbeatSeconds()), new RetryPolicy(request.maxAttempts(), Duration.ofSeconds(5), 2, Duration.ofMinutes(5)), request.allowManualRetry(), request.allowCancel());
        retries.configure(taskId, policy.retryPolicy()); return schedules.configure(taskId, policy);
    }
    @GetMapping("/{taskId}")
    public SchedulePolicy get(@PathVariable UUID taskId) { return schedules.get(taskId); }
}
