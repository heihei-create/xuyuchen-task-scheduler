package com.xuyuchen.scheduler.task;

import java.util.*;

public class TaskGraph {
    private final Map<UUID, Set<UUID>> parents = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> children = new LinkedHashMap<>();
    public void add(UUID taskId) { parents.computeIfAbsent(taskId, k -> new LinkedHashSet<>()); children.computeIfAbsent(taskId, k -> new LinkedHashSet<>()); }
    public void addEdge(UUID parent, UUID child) {
        add(parent); add(child);
        if (parent.equals(child)) throw new IllegalArgumentException("graph cycle");
        if (reachable(child, parent, new HashSet<>())) throw new IllegalArgumentException("graph cycle");
        parents.get(child).add(parent); children.get(parent).add(child);
    }
    public Set<UUID> parentsOf(UUID taskId) { return Set.copyOf(parents.getOrDefault(taskId, Set.of())); }
    public Set<UUID> childrenOf(UUID taskId) { return Set.copyOf(children.getOrDefault(taskId, Set.of())); }
    public boolean root(UUID taskId) { return parentsOf(taskId).isEmpty(); }
    public boolean terminal(UUID taskId) { return childrenOf(taskId).isEmpty(); }
    public List<UUID> topologicalOrder() {
        Map<UUID, Integer> degree = new HashMap<>();
        parents.forEach((id, value) -> degree.put(id, value.size()));
        ArrayDeque<UUID> queue = new ArrayDeque<>(degree.entrySet().stream().filter(e -> e.getValue() == 0).map(Map.Entry::getKey).toList());
        List<UUID> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            UUID current = queue.removeFirst(); result.add(current);
            for (UUID child : childrenOf(current)) {
                int value = degree.merge(child, -1, Integer::sum);
                if (value == 0) queue.addLast(child);
            }
        }
        if (result.size() != parents.size()) throw new IllegalStateException("graph contains cycle");
        return result;
    }
    private boolean reachable(UUID current, UUID target, Set<UUID> visited) {
        if (current.equals(target)) return true;
        if (!visited.add(current)) return false;
        return childrenOf(current).stream().anyMatch(child -> reachable(child, target, visited));
    }
}
