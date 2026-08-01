package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryDependencyRepository implements DependencyRepository {
    private final CopyOnWriteArrayList<TaskDependency> records = new CopyOnWriteArrayList<>();
    @Override public TaskDependency save(TaskDependency dependency) { records.addIfAbsent(dependency); return dependency; }
    @Override public List<TaskDependency> findByParent(UUID parentId) { return records.stream().filter(d -> d.parentTaskId().equals(parentId)).toList(); }
    @Override public List<TaskDependency> findByChild(UUID childId) { return records.stream().filter(d -> d.childTaskId().equals(childId)).toList(); }
}
