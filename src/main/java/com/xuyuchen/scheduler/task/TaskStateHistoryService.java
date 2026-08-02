package com.xuyuchen.scheduler.task;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskStateHistoryService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<TaskStateHistory>> history = new ConcurrentHashMap<>();
    private final NamedParameterJdbcTemplate jdbc;
    public TaskStateHistoryService() { this(null); }
    @Autowired
    public TaskStateHistoryService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public TaskStateHistory append(UUID taskId, TaskStatus from, TaskStatus to, int attempt, String actor, String reason) {
        TaskStateHistory value = new TaskStateHistory(taskId, from, to, attempt, actor, reason, Instant.now());
        if (jdbc == null) { history.computeIfAbsent(taskId, id -> new CopyOnWriteArrayList<>()).add(value); return value; }
        jdbc.update("insert into task_state_history (id,task_id,from_status,to_status,attempt,actor,reason,created_at) values (:id,:task,:from,:to,:attempt,:actor,:reason,:created)",
                new MapSqlParameterSource().addValue("id", UUID.randomUUID().toString()).addValue("task", taskId.toString()).addValue("from", from.name()).addValue("to", to.name()).addValue("attempt", attempt).addValue("actor", actor).addValue("reason", reason).addValue("created", java.sql.Timestamp.from(value.changedAt())));
        return value;
    }
    public List<TaskStateHistory> list(UUID taskId) {
        if (jdbc == null) return List.copyOf(history.getOrDefault(taskId, new CopyOnWriteArrayList<>()));
        return jdbc.query("select * from task_state_history where task_id=:task order by created_at asc", new MapSqlParameterSource("task", taskId.toString()), (rs, row) -> map(rs));
    }
    public TaskStateHistory latest(UUID taskId) {
        if (jdbc == null) return history.getOrDefault(taskId, new CopyOnWriteArrayList<>()).stream().reduce((a, b) -> b).orElse(null);
        return jdbc.query("select * from task_state_history where task_id=:task order by created_at desc limit 1", new MapSqlParameterSource("task", taskId.toString()), (rs, row) -> map(rs)).stream().findFirst().orElse(null);
    }
    public void clear(UUID taskId) {
        if (jdbc == null) history.remove(taskId); else jdbc.update("delete from task_state_history where task_id=:task", new MapSqlParameterSource("task", taskId.toString()));
    }
    private TaskStateHistory map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new TaskStateHistory(UUID.fromString(rs.getString("task_id")), TaskStatus.valueOf(rs.getString("from_status")), TaskStatus.valueOf(rs.getString("to_status")), rs.getInt("attempt"), rs.getString("actor"), rs.getString("reason"), rs.getTimestamp("created_at").toInstant());
    }
}
