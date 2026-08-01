package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TaskDAGValidator {
    private final DependencyRepository dependencies;
    public TaskDAGValidator(DependencyRepository dependencies) { this.dependencies = dependencies; }
    public void addDependency(TaskDependency dependency) {
        if (wouldCreateCycle(dependency.parentTaskId(), dependency.childTaskId(), new HashSet<>())) throw new IllegalArgumentException("dependency would create a cycle");
        dependencies.save(dependency);
    }
    public boolean ready(UUID taskId, Set<TaskStatus> completedStates, Map<UUID, TaskStatus> statuses) {
        return dependencies.findByChild(taskId).stream().allMatch(d -> completedStates.contains(statuses.get(d.parentTaskId())));
    }
    private boolean wouldCreateCycle(UUID from, UUID target, Set<UUID> visited) {
        if (from.equals(target)) return true;
        if (!visited.add(from)) return false;
        return dependencies.findByParent(from).stream().anyMatch(d -> wouldCreateCycle(d.childTaskId(), target, visited));
    }
}
