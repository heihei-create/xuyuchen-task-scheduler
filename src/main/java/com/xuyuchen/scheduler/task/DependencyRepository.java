package com.xuyuchen.scheduler.task;

import java.util.List;
import java.util.UUID;

public interface DependencyRepository {
    TaskDependency save(TaskDependency dependency);
    List<TaskDependency> findByParent(UUID parentId);
    List<TaskDependency> findByChild(UUID childId);
}
