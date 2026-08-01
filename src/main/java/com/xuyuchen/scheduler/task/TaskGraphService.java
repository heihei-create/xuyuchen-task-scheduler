package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskGraphService {
    private final Map<UUID, TaskGraph> graphs = new ConcurrentHashMap<>();
    private final DependencyRepository dependencies;
    private final TaskRepository tasks;
    public TaskGraphService(DependencyRepository dependencies, TaskRepository tasks) { this.dependencies = dependencies; this.tasks = tasks; }
    public TaskGraph graph(String tenantId, UUID rootTaskId, List<TaskDependency> edges) {
        if (!belongsTo(tenantId, rootTaskId)) throw new IllegalArgumentException("task not found");
        for (TaskDependency edge : edges) if (!belongsTo(tenantId, edge.parentTaskId()) || !belongsTo(tenantId, edge.childTaskId())) throw new IllegalArgumentException("dependency task not found");
        return graph(rootTaskId, edges);
    }
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
    public void requireTenant(String tenantId, UUID rootTaskId) { if (!belongsTo(tenantId, rootTaskId)) throw new IllegalArgumentException("task not found"); require(rootTaskId); }
    private boolean belongsTo(String tenantId, UUID taskId) { return tasks.findById(taskId).map(task -> task.getTenantId().equals(tenantId)).orElse(false); }
}
