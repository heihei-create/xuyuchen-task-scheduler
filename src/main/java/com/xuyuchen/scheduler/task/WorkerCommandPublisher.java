package com.xuyuchen.scheduler.task;

public interface WorkerCommandPublisher {
    void dispatch(WorkerCommand command);
}
