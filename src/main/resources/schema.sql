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
