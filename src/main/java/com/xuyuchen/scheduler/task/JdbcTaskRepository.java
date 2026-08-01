package com.xuyuchen.scheduler.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "scheduler.persistence", havingValue = "jdbc")
public class JdbcTaskRepository implements TaskRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcTaskRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Task save(Task task) {
        String sql = "merge into tasks(id,tenant_id,name,payload,idempotency_key,created_at,status,attempt,lease_until,worker_id,result) values(:id,:tenant,:name,:payload,:key,:created,:status,:attempt,:lease,:worker,:result)";
        jdbc.update(sql, params(task)); return task;
    }
    @Override public Optional<Task> findById(UUID id) {
        return jdbc.query("select * from tasks where id=:id", new MapSqlParameterSource("id", id.toString()), this::map).stream().findFirst();
    }
    @Override public List<Task> findByTenant(String tenantId) {
        return jdbc.query("select * from tasks where tenant_id=:tenant order by created_at desc", new MapSqlParameterSource("tenant", tenantId), this::map);
    }
    @Override public List<Task> findByStatus(TaskStatus status) {
        return jdbc.query("select * from tasks where status=:status", new MapSqlParameterSource("status", status.name()), this::map);
    }
    @Override public Optional<Task> findByIdempotencyKey(String tenantId, String key) {
        return jdbc.query("select * from tasks where tenant_id=:tenant and idempotency_key=:key", new MapSqlParameterSource().addValue("tenant", tenantId).addValue("key", key), this::map).stream().findFirst();
    }
    @Override public long countByTenant(String tenantId) {
        Long value = jdbc.queryForObject("select count(*) from tasks where tenant_id=:tenant", new MapSqlParameterSource("tenant", tenantId), Long.class);
        return value == null ? 0 : value;
    }
    private MapSqlParameterSource params(Task t) {
        return new MapSqlParameterSource()
                .addValue("id", t.getId().toString()).addValue("tenant", t.getTenantId()).addValue("name", t.getName())
                .addValue("payload", t.getPayload()).addValue("key", t.getIdempotencyKey()).addValue("created", t.getCreatedAt())
                .addValue("status", t.getStatus().name()).addValue("attempt", t.getAttempt()).addValue("lease", t.getLeaseUntil())
                .addValue("worker", t.getWorkerId()).addValue("result", t.getResult());
    }
    private Task map(ResultSet rs, int row) throws SQLException {
        Task task = new Task(UUID.fromString(rs.getString("id")), rs.getString("tenant_id"), rs.getString("name"), rs.getString("payload"), rs.getString("idempotency_key"));
        return task;
    }
}
