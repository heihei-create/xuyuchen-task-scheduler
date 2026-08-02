create table if not exists tasks (
  id varchar(36) primary key,
  tenant_id varchar(128) not null,
  name varchar(128) not null,
  payload clob,
  idempotency_key varchar(256) not null,
  created_at timestamp(9) not null,
  status varchar(32) not null,
  attempt integer not null,
  lease_until timestamp(9) null,
  lease_token varchar(128) null,
  worker_id varchar(128) null,
  result clob null,
  unique(tenant_id, idempotency_key)
);
create index if not exists idx_tasks_tenant_status on tasks(tenant_id, status, created_at);
create table if not exists task_audits (
  id varchar(36) primary key,
  task_id varchar(36) not null,
  tenant_id varchar(128) not null,
  operator varchar(128) not null,
  from_status varchar(32) not null,
  to_status varchar(32) not null,
  reason varchar(256) not null,
  trace_id varchar(128),
  created_at timestamp(9) not null,
  details clob not null
);
create index if not exists idx_task_audits_tenant_created on task_audits(tenant_id, created_at desc);
create index if not exists idx_task_audits_task_created on task_audits(task_id, created_at asc);
create table if not exists task_state_history (
  id varchar(36) primary key,
  task_id varchar(36) not null,
  from_status varchar(32) not null,
  to_status varchar(32) not null,
  attempt integer not null,
  actor varchar(128) not null,
  reason varchar(256) not null,
  created_at timestamp(9) not null
);
create index if not exists idx_task_state_history_task_created on task_state_history(task_id, created_at asc);
