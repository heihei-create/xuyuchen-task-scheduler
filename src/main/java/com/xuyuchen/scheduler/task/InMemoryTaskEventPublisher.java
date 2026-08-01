package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryTaskEventPublisher implements TaskEventPublisher {
    private final CopyOnWriteArrayList<TaskEvent> events = new CopyOnWriteArrayList<>();
    @Override public void publish(TaskEvent event) { events.add(event); }
    public List<TaskEvent> recent(int limit) {
        int from = Math.max(0, events.size() - Math.max(1, limit));
        List<TaskEvent> result = new java.util.ArrayList<>(events.subList(from, events.size()));
        java.util.Collections.reverse(result);
        return result;
    }
}
