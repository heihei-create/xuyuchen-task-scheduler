package com.xuyuchen.scheduler.task;

public interface TaskEventPublisher {
    void publish(TaskEvent event);
}
