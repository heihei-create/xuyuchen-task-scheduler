package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskGraphService {
    private final Map<UUID, TaskGraph> graphs = new ConcurrentHashMap<>();
    private final DependencyRepository dependencies;
    public TaskGraphService(DependencyRepository dependencies) { this.dependencies = dependencies; }
    public TaskGraph graph(UUID rootTaskId, List<TaskDependency> edges) {
        TaskGraph graph = new TaskGraph(); graph.add(rootTaskId);
        edges.forEach(edge -> { graph.addEdge(edge.parentTaskId(), edge.childTaskId()); dependencies.save(edge); });
        graphs.put(rootTaskId, graph); return graph;
    }
    public TaskGraph require(UUID rootTaskId) { return Optional.ofNullable(graphs.get(rootTaskId)).orElseThrow(() -> new IllegalArgumentException("task graph not found")); }
    public boolean canRelease(UUID taskId, Map<UUID, TaskStatus> statuses) {
        TaskGraph graph = graphs.values().stream().filter(g -> g.parentsOf(taskId).size() > 0 || g.root(taskId)).findFirst().orElse(null);
        if (graph == null) return true;
        return graph.parentsOf(taskId).stream().allMatch(parent -> statuses.get(parent) == TaskStatus.SUCCESS);
    }
    public List<UUID> order(UUID rootTaskId) { return require(rootTaskId).topologicalOrder(); }
}
