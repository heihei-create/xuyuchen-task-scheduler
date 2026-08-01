package com.xuyuchen.scheduler.task;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/graphs")
public class GraphController {
    private final TaskGraphService service;
    public GraphController(TaskGraphService service) { this.service = service; }
    public record EdgeRequest(UUID parentTaskId, UUID childTaskId, int order) {}
    public record GraphRequest(UUID rootTaskId, List<@Valid EdgeRequest> edges) {}
    @PostMapping
    public List<UUID> create(@RequestHeader("X-Tenant-Id") String tenantId, @jakarta.validation.Valid @RequestBody GraphRequest request) {
        List<TaskDependency> edges = request.edges() == null ? List.of() : request.edges().stream().map(e -> new TaskDependency(e.parentTaskId(), e.childTaskId(), e.order())).toList();
        return service.graph(tenantId, request.rootTaskId(), edges).topologicalOrder();
    }
    @GetMapping("/{rootTaskId}/order")
    public List<UUID> order(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable UUID rootTaskId) { service.requireTenant(tenantId, rootTaskId); return service.order(rootTaskId); }
}
