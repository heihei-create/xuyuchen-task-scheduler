package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ConditionalOnProperty(name = "scheduler.worker-messaging", havingValue = "memory", matchIfMissing = true)
public class InMemoryWorkerCommandPublisher implements WorkerCommandPublisher {
    private final CopyOnWriteArrayList<WorkerCommand> commands = new CopyOnWriteArrayList<>();
    @Override public void dispatch(WorkerCommand command) { commands.add(command); }
    public List<WorkerCommand> commands() { return List.copyOf(commands); }
}
