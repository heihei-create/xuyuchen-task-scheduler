package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskAuditService {
    private final CopyOnWriteArrayList<TaskAuditRecord> records = new CopyOnWriteArrayList<>();
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TaskStateHistoryService stateHistory;
    public TaskAuditService() { this(null, null, null); }
    @Autowired
    public TaskAuditService(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper, TaskStateHistoryService stateHistory) { this.jdbc = jdbc; this.mapper = mapper; this.stateHistory = stateHistory; }
    public TaskAuditRecord record(Task task, String operator, TaskStatus from, TaskStatus to, String reason, String traceId, Map<String, Object> details) {
        TaskAuditRecord record = new TaskAuditRecord(task.getId(), task.getTenantId(), operator, from, to, reason, traceId, Instant.now(), details);
        if (stateHistory != null) stateHistory.append(task.getId(), from, to, task.getAttempt(), operator, reason);
        if (jdbc == null) { records.add(record); return record; }
        try {
            jdbc.update("insert into task_audits (id,task_id,tenant_id,operator,from_status,to_status,reason,trace_id,created_at,details) values (:id,:task,:tenant,:operator,:from,:to,:reason,:trace,:created,:details)",
                    new MapSqlParameterSource().addValue("id", UUID.randomUUID().toString()).addValue("task", task.getId().toString()).addValue("tenant", task.getTenantId()).addValue("operator", operator).addValue("from", from.name()).addValue("to", to.name()).addValue("reason", reason).addValue("trace", traceId).addValue("created", java.sql.Timestamp.from(record.createdAt())).addValue("details", write(details)));
        } catch (RuntimeException ex) { records.add(record); throw ex; }
        return record;
    }
    public List<TaskAuditRecord> findByTask(String tenantId, UUID taskId) {
        if (jdbc == null) return records.stream().filter(r -> r.tenantId().equals(tenantId) && r.taskId().equals(taskId)).toList();
        return jdbc.query("select * from task_audits where tenant_id=:tenant and task_id=:task order by created_at asc", new MapSqlParameterSource().addValue("tenant", tenantId).addValue("task", taskId.toString()), (rs, row) -> map(rs));
    }
    public List<TaskAuditRecord> findByTenant(String tenantId, int limit) {
        if (jdbc == null) return records.stream().filter(r -> r.tenantId().equals(tenantId)).skip(Math.max(0, records.size() - limit)).toList();
        return jdbc.query("select * from task_audits where tenant_id=:tenant order by created_at desc limit :limit", new MapSqlParameterSource().addValue("tenant", tenantId).addValue("limit", Math.max(1, Math.min(limit, 500))), (rs, row) -> map(rs));
    }
    private String write(Map<String, Object> details) { try { return mapper.writeValueAsString(details == null ? Map.of() : details); } catch (Exception ex) { throw new IllegalArgumentException("audit details are not serializable", ex); } }
    private TaskAuditRecord map(java.sql.ResultSet rs) throws java.sql.SQLException {
        try { return new TaskAuditRecord(UUID.fromString(rs.getString("task_id")), rs.getString("tenant_id"), rs.getString("operator"), TaskStatus.valueOf(rs.getString("from_status")), TaskStatus.valueOf(rs.getString("to_status")), rs.getString("reason"), rs.getString("trace_id"), rs.getTimestamp("created_at").toInstant(), mapper.readValue(rs.getString("details"), new TypeReference<>() {})); }
        catch (Exception ex) { throw new java.sql.SQLException("invalid audit row", ex); }
    }
}
